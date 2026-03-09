package com.example.lottery.model;

import static org.junit.Assert.*;
import org.junit.Test;

/**
 * Unit tests for the Event model.
 */
public class EventTest {

    @Test
    public void testWaitingListLimitSetterGetter() {
        Event event = new Event();
        
        // Test setting a specific limit (US 02.03.01)
        event.setWaitingListLimit(50);
        assertEquals(Integer.valueOf(50), event.getWaitingListLimit());
        
        // Test setting null (Unlimited state)
        event.setWaitingListLimit(null);
        assertNull(event.getWaitingListLimit());
    }

    @Test
    public void testEventConstructorWithLimit() {
        // Verify constructor correctly maps the waiting list limit
        Event event = new Event("1", "Title", null, null, 10, "Details", null, null, "Org1", false, 100);
        assertEquals(Integer.valueOf(100), event.getWaitingListLimit());
    }
}
