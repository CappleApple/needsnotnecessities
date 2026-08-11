package com.cappleapple.needsnotnecessities.survival.rest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class RestServiceTest {
    @Test
    void partialSleepProducesProportionalTimelineRecovery() {
        assertEquals(20.0D, RestService.sleepingRecoveryDelta(40.0D, 0.5D, 1.0D));
        assertEquals(10.0D, RestService.sleepingRecoveryDelta(40.0D, 0.25D, 1.0D));
    }

    @Test
    void invalidRecoveryDurationsAreRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> RestService.sleepingRecoveryDelta(40.0D, 0.5D, 0.0D));
    }
}
