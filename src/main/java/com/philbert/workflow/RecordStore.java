package com.philbert.workflow;

import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Consumer;

public class RecordStore {

    private final ConcurrentMap<String, DataRecord> records = new ConcurrentHashMap<>();

    public void add(DataRecord record) {
        if (record == null) {
            throw new IllegalArgumentException("record must not be null");
        }
        // putIfAbsent checks and inserts in one step, so two threads can't both add the same id.
        DataRecord existing = records.putIfAbsent(record.getId(), record);
        if (existing != null) {
            throw new IllegalStateException("record already exists: " + record.getId());
        }
    }

    public Optional<DataRecord> findById(String id) {
        if (id == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(records.get(id));
    }

    public void update(String id, Consumer<DataRecord> change) {
        DataRecord record = findById(id)
                .orElseThrow(() -> new NoSuchElementException("no record with id: " + id));
        synchronized (record) {
            change.accept(record);
        }
    }

    public int size() {
        return records.size();
    }
}
