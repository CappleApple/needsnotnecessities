package com.cappleapple.needsnotnecessities.survival;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class RespawnPenaltyServiceTest {
    @Test
    void messageRequiresBothNeedsToHaveDropped() {
        assertTrue(RespawnPenaltyService.shouldSendMessage(20.0D, 0.0D, 16.0D, 0.0D, true, true));
        assertFalse(RespawnPenaltyService.shouldSendMessage(20.0D, 0.0D, 0.0D, 0.0D, true, true));
        assertFalse(RespawnPenaltyService.shouldSendMessage(20.0D, 0.0D, 16.0D, 0.0D, true, false));
    }
}
