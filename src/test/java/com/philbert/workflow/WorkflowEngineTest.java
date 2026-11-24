package com.philbert.workflow;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class WorkflowEngineTest {

    private final WorkflowEngine engine = new WorkflowEngine();
    private final List<String> calls = new ArrayList<>();

    private Rule recordingRule(String name, String triggerEventType) {
        return new Rule(name, triggerEventType, List.of(event -> calls.add(name)));
    }

    @Test
    void runsMatchingRule() {
        engine.addRule(recordingRule("approve-sale", "sale.approved"));

        HandleResult result = engine.handle(new Event("sale.approved", "sale-1"));

        assertEquals(List.of("approve-sale"), result.succeededRules());
        assertEquals(List.of("approve-sale"), calls);
    }

    @Test
    void skipsRulesThatDoNotMatch() {
        engine.addRule(recordingRule("approve-sale", "sale.approved"));
        engine.addRule(recordingRule("reject-sale", "sale.rejected"));

        engine.handle(new Event("sale.rejected", "sale-1"));

        assertEquals(List.of("reject-sale"), calls);
    }

    @Test
    void runsEveryMatchingRuleInTheOrderAdded() {
        engine.addRule(recordingRule("create-build", "sale.approved"));
        engine.addRule(recordingRule("notify-manager", "sale.approved"));

        HandleResult result = engine.handle(new Event("sale.approved", "sale-1"));

        assertEquals(2, result.rulesRun());
        assertEquals(List.of("create-build", "notify-manager"), calls);
    }

    @Test
    void eventWithNoMatchingRulesDoesNothing() {
        engine.addRule(recordingRule("approve-sale", "sale.approved"));

        HandleResult result = engine.handle(new Event("sale.created", "sale-1"));

        assertEquals(0, result.rulesRun());
        assertTrue(calls.isEmpty());
    }

    @Test
    void saleApprovedCreatesBuildRecord() {
        RecordStore store = new RecordStore();
        store.add(new DataRecord("sale-1", "Sale"));
        engine.addRule(new Rule("create-build", "sale.approved", List.of(
                event -> store.add(new DataRecord("build-for-" + event.recordId(), "Build")))));

        engine.handle(new Event("sale.approved", "sale-1"));

        assertEquals(2, store.size());
        assertTrue(store.findById("build-for-sale-1").isPresent());
    }

    @Test
    void failingRuleDoesNotStopOtherRules() {
        RuntimeException boom = new IllegalStateException("build service down");
        engine.addRule(new Rule("create-build", "sale.approved", List.of(event -> { throw boom; })));
        engine.addRule(recordingRule("notify-manager", "sale.approved"));

        HandleResult result = engine.handle(new Event("sale.approved", "sale-1"));

        assertEquals(List.of("notify-manager"), calls);
        assertEquals(List.of("notify-manager"), result.succeededRules());
        assertEquals(1, result.failures().size());
        assertEquals("create-build", result.failures().get(0).ruleName());
        assertSame(boom, result.failures().get(0).error());
        assertFalse(result.allSucceeded());
    }

    @Test
    void failingActionStopsTheRestOfItsOwnRule() {
        engine.addRule(new Rule("create-and-notify", "sale.approved", List.of(
                event -> { throw new IllegalStateException("build service down"); },
                event -> calls.add("notify manager"))));

        HandleResult result = engine.handle(new Event("sale.approved", "sale-1"));

        assertTrue(calls.isEmpty());
        assertEquals("create-and-notify", result.failures().get(0).ruleName());
    }

    @Test
    void allSucceededWhenNoRuleFails() {
        engine.addRule(recordingRule("approve-sale", "sale.approved"));

        assertTrue(engine.handle(new Event("sale.approved", "sale-1")).allSucceeded());
    }

    @Test
    void rulesListCannotBeChangedFromOutside() {
        engine.addRule(recordingRule("approve-sale", "sale.approved"));

        assertThrows(UnsupportedOperationException.class,
                () -> engine.getRules().add(recordingRule("sneaky", "sale.approved")));
    }

    @Test
    void rejectsNullRuleOrEvent() {
        assertThrows(IllegalArgumentException.class, () -> engine.addRule(null));
        assertThrows(IllegalArgumentException.class, () -> engine.handle(null));
    }
}
