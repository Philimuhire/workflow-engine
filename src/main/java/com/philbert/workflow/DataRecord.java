package com.philbert.workflow;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class DataRecord {

    private final String id;
    private final String type;
    private final Map<String, Object> fields = new HashMap<>();

    public DataRecord(String id, String type) {
        this.id = requireText(id, "id");
        this.type = requireText(type, "type");
    }

    public String getId() {
        return id;
    }

    public String getType() {
        return type;
    }

    public synchronized Object get(String field) {
        return fields.get(field);
    }

    public synchronized void set(String field, Object value) {
        fields.put(requireText(field, "field"), value);
    }

    public synchronized Map<String, Object> getFields() {
        return Collections.unmodifiableMap(new HashMap<>(fields));
    }

    private static String requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value;
    }
}
