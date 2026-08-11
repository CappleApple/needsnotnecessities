package com.cappleapple.needsnotnecessities.survival.health;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class HealthEffectScalingServiceTest {
    @Test
    void healingPreservesItsPercentageOfReferenceMaximumHealth() {
        assertEquals(6.0F, HealthEffectScalingService.scaleAmount(6.0F, 20.0F, 20.0D));
        assertEquals(12.0F, HealthEffectScalingService.scaleAmount(6.0F, 40.0F, 20.0D));
        assertEquals(3.0F, HealthEffectScalingService.scaleAmount(6.0F, 10.0F, 20.0D));
    }

    @Test
    void invalidReferenceHealthIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> HealthEffectScalingService.scaleAmount(6.0F, 40.0F, 0.0D));
    }
}
