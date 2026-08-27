package com.cappleapple.needsnotnecessities.survival.health;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class RespawnHealthServiceTest {
    @Test
    void percentageUsesTheCurrentMaximumHealth() {
        assertEquals(5.0F, RespawnHealthService.healthAfterRespawn(20.0F, 0.25D));
        assertEquals(15.0F, RespawnHealthService.healthAfterRespawn(60.0F, 0.25D));
        assertEquals(30.0F, RespawnHealthService.healthAfterRespawn(60.0F, 0.5D));
    }

    @Test
    void lowPercentageRetainsAtLeastOneHealthPoint() {
        assertEquals(1.0F, RespawnHealthService.healthAfterRespawn(20.0F, 0.01D));
        assertEquals(1.0F, RespawnHealthService.healthAfterRespawn(1.0F, 0.25D));
    }

    @Test
    void minimumNeverExceedsAnUnusuallyLowMaximum() {
        assertEquals(0.5F, RespawnHealthService.healthAfterRespawn(0.5F, 0.25D));
    }

    @Test
    void fullPercentageRestoresExactlyTheMaximum() {
        assertEquals(60.0F, RespawnHealthService.healthAfterRespawn(60.0F, 1.0D));
    }
}
