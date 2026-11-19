package com.philbert.workflow;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class EventTest {

    @Test
    void storesTypeAndRecordId() {
        Event event = new Event("sale.approved", "sale-1");

        assertEquals("sale.approved", event.type());
        assertEquals("sale-1", event.recordId());
    }

    @Test
    void eventsWithSameValuesAreEqual() {
        assertEquals(new Event("sale.approved", "sale-1"), new Event("sale.approved", "sale-1"));
    }

    @Test
    void rejectsBlankTypeOrRecordId() {
        assertThrows(IllegalArgumentException.class, () -> new Event("", "sale-1"));
        assertThrows(IllegalArgumentException.class, () -> new Event("sale.approved", null));
    }
}
