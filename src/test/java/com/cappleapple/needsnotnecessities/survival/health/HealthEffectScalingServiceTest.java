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

    @Test
    void currentAbsorptionKeepsItsFilledFractionWhenMaximumHealthChanges() {
        assertEquals(4.0F, AbsorptionScalingService.rescaleAmount(8.0F, 16.0F, 8.0F));
        assertEquals(12.0F, AbsorptionScalingService.rescaleAmount(6.0F, 8.0F, 16.0F));
        assertEquals(0.0F, AbsorptionScalingService.rescaleAmount(0.0F, 0.0F, 8.0F));
    }
}
