package com.philbert.workflow;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class RecordStoreTest {

    private final RecordStore store = new RecordStore();

    @Test
    void addedRecordCanBeFound() {
        DataRecord sale = new DataRecord("sale-1", "Sale");

        store.add(sale);

        assertSame(sale, store.findById("sale-1").orElseThrow());
        assertEquals(1, store.size());
    }

    @Test
    void unknownIdReturnsEmpty() {
        assertTrue(store.findById("missing").isEmpty());
        assertTrue(store.findById(null).isEmpty());
    }

    @Test
    void rejectsDuplicateId() {
        store.add(new DataRecord("sale-1", "Sale"));

        assertThrows(IllegalStateException.class, () -> store.add(new DataRecord("sale-1", "Sale")));
    }

    @Test
    void rejectsNullRecord() {
        assertThrows(IllegalArgumentException.class, () -> store.add(null));
    }

    @Test
    void updateChangesTheStoredRecord() {
        store.add(new DataRecord("sale-1", "Sale"));

        store.update("sale-1", record -> record.set("status", "APPROVED"));

        assertEquals("APPROVED", store.findById("sale-1").orElseThrow().get("status"));
    }

    @Test
    void updateOfUnknownRecordFails() {
        assertThrows(NoSuchElementException.class, () -> store.update("missing", record -> { }));
    }

    @Test
    void ruleActionCanCreateRecordInStore() {
        Action createBuild = event -> store.add(new DataRecord("build-for-" + event.recordId(), "Build"));
        Rule rule = new Rule("approve-sale", "sale.approved", List.of(createBuild));

        rule.run(new Event("sale.approved", "sale-1"));

        assertEquals("Build", store.findById("build-for-sale-1").orElseThrow().getType());
    }

    @Test
    void onlyOneThreadWinsWhenAddingSameIdAtOnce() throws InterruptedException {
        int threadCount = 10;
        CountDownLatch startTogether = new CountDownLatch(1);
        AtomicInteger successes = new AtomicInteger();
        List<Thread> threads = new ArrayList<>();

        for (int i = 0; i < threadCount; i++) {
            Thread thread = new Thread(() -> {
                try {
                    startTogether.await();
                    store.add(new DataRecord("sale-1", "Sale"));
                    successes.incrementAndGet();
                } catch (IllegalStateException | InterruptedException expected) {
                }
            });
            threads.add(thread);
            thread.start();
        }

        startTogether.countDown();
        for (Thread thread : threads) {
            thread.join();
        }

        assertEquals(1, successes.get());
        assertEquals(1, store.size());
    }
}
