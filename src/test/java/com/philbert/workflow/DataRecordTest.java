package com.philbert.workflow;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Map;
import org.junit.jupiter.api.Test;

class DataRecordTest {

    @Test
    void storesIdAndType() {
        DataRecord sale = new DataRecord("sale-1", "Sale");

        assertEquals("sale-1", sale.getId());
        assertEquals("Sale", sale.getType());
    }

    @Test
    void setAndGetField() {
        DataRecord sale = new DataRecord("sale-1", "Sale");

        sale.set("status", "APPROVED");

        assertEquals("APPROVED", sale.get("status"));
    }

    @Test
    void settingFieldAgainOverwritesIt() {
        DataRecord sale = new DataRecord("sale-1", "Sale");

        sale.set("status", "PENDING");
        sale.set("status", "APPROVED");

        assertEquals("APPROVED", sale.get("status"));
    }

    @Test
    void missingFieldReturnsNull() {
        DataRecord sale = new DataRecord("sale-1", "Sale");

        assertNull(sale.get("amount"));
    }

    @Test
    void fieldsMapCannotBeChangedFromOutside() {
        DataRecord sale = new DataRecord("sale-1", "Sale");
        Map<String, Object> fields = sale.getFields();

        assertThrows(UnsupportedOperationException.class, () -> fields.put("status", "HACKED"));
    }

    @Test
    void rejectsBlankIdOrType() {
        assertThrows(IllegalArgumentException.class, () -> new DataRecord(null, "Sale"));
        assertThrows(IllegalArgumentException.class, () -> new DataRecord("sale-1", " "));
    }
}
