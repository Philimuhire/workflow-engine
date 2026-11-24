package com.philbert.workflow;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class EventProcessorTest {

    private final WorkflowEngine engine = new WorkflowEngine();
    private final Queue<HandleResult> results = new ConcurrentLinkedQueue<>();

    @Test
    void processesEverySubmittedEvent() throws InterruptedException {
        AtomicInteger handled = new AtomicInteger();
        engine.addRule(new Rule("count", "sale.approved", List.of(event -> handled.incrementAndGet())));
        EventProcessor processor = new EventProcessor(engine, 4, results::add);

        processor.start();
        for (int i = 0; i < 1000; i++) {
            processor.submit(new Event("sale.approved", "sale-" + i));
        }
        processor.shutdown();

        assertEquals(1000, handled.get());
        assertEquals(1000, results.size());
    }

    @Test
    void workersRunEventsAtTheSameTime() throws InterruptedException {
        int workerCount = 4;
        CountDownLatch allInsideAtOnce = new CountDownLatch(workerCount);
        engine.addRule(new Rule("wait-for-others", "sale.approved", List.of(event -> {
            allInsideAtOnce.countDown();
            try {
                allInsideAtOnce.await(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        })));
        EventProcessor processor = new EventProcessor(engine, workerCount, results::add);

        processor.start();
        for (int i = 0; i < workerCount; i++) {
            processor.submit(new Event("sale.approved", "sale-" + i));
        }
        processor.shutdown();

        assertEquals(0, allInsideAtOnce.getCount());
    }

    @Test
    void failuresReachTheResultListener() throws InterruptedException {
        engine.addRule(new Rule("broken", "sale.approved", List.of(event -> {
            throw new IllegalStateException("build service down");
        })));
        EventProcessor processor = new EventProcessor(engine, 2, results::add);

        processor.start();
        processor.submit(new Event("sale.approved", "sale-1"));
        processor.shutdown();

        HandleResult result = results.poll();
        assertEquals("broken", result.failures().get(0).ruleName());
    }

    @Test
    void eventsSubmittedBeforeStartAreStillProcessed() throws InterruptedException {
        EventProcessor processor = new EventProcessor(engine, 2, results::add);

        processor.submit(new Event("sale.approved", "sale-1"));
        processor.start();
        processor.shutdown();

        assertEquals(1, results.size());
    }

    @Test
    void rejectsEventsAfterShutdown() throws InterruptedException {
        EventProcessor processor = new EventProcessor(engine, 2, results::add);
        processor.start();
        processor.shutdown();

        assertThrows(IllegalStateException.class,
                () -> processor.submit(new Event("sale.approved", "sale-1")));
    }

    @Test
    void shutdownTwiceIsHarmless() throws InterruptedException {
        EventProcessor processor = new EventProcessor(engine, 2, results::add);
        processor.start();

        processor.shutdown();
        processor.shutdown();

        assertTrue(results.isEmpty());
    }

    @Test
    void cannotStartTwice() throws InterruptedException {
        EventProcessor processor = new EventProcessor(engine, 1, results::add);
        processor.start();

        assertThrows(IllegalStateException.class, processor::start);
        processor.shutdown();
    }

    @Test
    void rejectsInvalidSetup() {
        assertThrows(IllegalArgumentException.class, () -> new EventProcessor(engine, 0, results::add));
        assertThrows(IllegalArgumentException.class, () -> new EventProcessor(null, 2, results::add));
        assertThrows(IllegalArgumentException.class, () -> new EventProcessor(engine, 2, null));
    }
}
