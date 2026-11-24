package com.philbert.workflow;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class RuleTest {

    private final Action doNothing = event -> { };

    @Test
    void matchesEventWithSameType() {
        Rule rule = new Rule("approve-sale", "sale.approved", List.of(doNothing));

        assertTrue(rule.matches(new Event("sale.approved", "sale-1")));
    }

    @Test
    void doesNotMatchEventWithDifferentType() {
        Rule rule = new Rule("approve-sale", "sale.approved", List.of(doNothing));

        assertFalse(rule.matches(new Event("sale.rejected", "sale-1")));
    }

    @Test
    void runsAllActionsInOrder() {
        List<String> calls = new ArrayList<>();
        Rule rule = new Rule("approve-sale", "sale.approved", List.of(
                event -> calls.add("create build for " + event.recordId()),
                event -> calls.add("notify manager")));

        rule.run(new Event("sale.approved", "sale-1"));

        assertEquals(List.of("create build for sale-1", "notify manager"), calls);
    }

    @Test
    void changingOriginalListDoesNotChangeRule() {
        List<Action> actions = new ArrayList<>(List.of(doNothing));
        Rule rule = new Rule("approve-sale", "sale.approved", actions);

        actions.add(doNothing);

        assertEquals(1, rule.actions().size());
    }

    @Test
    void rejectsInvalidRules() {
        assertThrows(IllegalArgumentException.class, () -> new Rule("", "sale.approved", List.of(doNothing)));
        assertThrows(IllegalArgumentException.class, () -> new Rule("approve-sale", null, List.of(doNothing)));
        assertThrows(IllegalArgumentException.class, () -> new Rule("approve-sale", "sale.approved", List.of()));
    }
}
