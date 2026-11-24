package com.philbert.workflow;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.RepeatedTest;

class LostUpdateTest {

    @RepeatedTest(5)
    void concurrentIncrementsOnSameRecordAreNotLost() throws InterruptedException {
        RecordStore store = new RecordStore();
        DataRecord sale = new DataRecord("sale-1", "Sale");
        sale.set("payments", 0);
        store.add(sale);

        WorkflowEngine engine = new WorkflowEngine();
        engine.addRule(new Rule("count-payment", "sale.paid", List.of(event ->
                store.update(event.recordId(), record -> {
                    int current = (Integer) record.get("payments");
                    record.set("payments", current + 1);
                }))));

        EventProcessor processor = new EventProcessor(engine, 8, result -> { });
        processor.start();
        for (int i = 0; i < 1000; i++) {
            processor.submit(new Event("sale.paid", "sale-1"));
        }
        processor.shutdown();

        assertEquals(1000, sale.get("payments"));
    }
}
