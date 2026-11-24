package com.philbert.workflow;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Consumer;

public class EventProcessor {

    private static final Event STOP = new Event("stop", "stop");

    private final BlockingQueue<Event> queue = new LinkedBlockingQueue<>();
    private final List<Thread> workers = new ArrayList<>();
    private final WorkflowEngine engine;
    private final Consumer<HandleResult> resultListener;
    private boolean started = false;
    private boolean accepting = true;

    public EventProcessor(WorkflowEngine engine, int workerCount, Consumer<HandleResult> resultListener) {
        if (engine == null || resultListener == null) {
            throw new IllegalArgumentException("engine and resultListener must not be null");
        }
        if (workerCount < 1) {
            throw new IllegalArgumentException("workerCount must be at least 1");
        }
        this.engine = engine;
        this.resultListener = resultListener;
        for (int i = 1; i <= workerCount; i++) {
            workers.add(new Thread(this::workLoop, "worker-" + i));
        }
    }

    public synchronized void start() {
        if (started) {
            throw new IllegalStateException("already started");
        }
        started = true;
        for (Thread worker : workers) {
            worker.start();
        }
    }

    public synchronized void submit(Event event) {
        if (event == null) {
            throw new IllegalArgumentException("event must not be null");
        }
        if (!accepting) {
            throw new IllegalStateException("processor is shut down");
        }
        queue.add(event);
    }

    public void shutdown() throws InterruptedException {
        synchronized (this) {
            if (!accepting) {
                return;
            }
            accepting = false;
            for (int i = 0; i < workers.size(); i++) {
                queue.add(STOP);
            }
        }
        for (Thread worker : workers) {
            worker.join();
        }
    }

    private void workLoop() {
        while (true) {
            Event event;
            try {
                event = queue.take();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
            if (event == STOP) {
                return;
            }
            resultListener.accept(engine.handle(event));
        }
    }
}
