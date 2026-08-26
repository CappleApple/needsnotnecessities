package com.cappleapple.needsnotnecessities.network;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class SilentHealthAdjustmentPayloadTest {
    @Test
    void removesOnlyTheHealthLossCausedByTheMaximumHealthClamp() {
        assertEquals(20.0F, SilentHealthAdjustmentPayload.adjustedClientHealth(30.0F, 10.0F, 20.0F));
        assertEquals(22.0F, SilentHealthAdjustmentPayload.adjustedClientHealth(30.0F, 8.0F, 20.0F));
        assertEquals(20.0F, SilentHealthAdjustmentPayload.adjustedClientHealth(20.0F, 10.0F, 20.0F));
    }
}
