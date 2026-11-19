package com.philbert.workflow;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class SetupTest {

    @Test
    void junitIsWorking() {
        assertEquals(4, 2 + 2);
    }

    @Test
    void runningOnJava21OrNewer() {
        int major = Runtime.version().feature();
        assertTrue(major >= 21, "Expected Java 21+, but was " + major);
    }
}
