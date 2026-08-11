package com.cappleapple.needsnotnecessities.survival.health;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.cappleapple.needsnotnecessities.config.BaseHealthMode;
import com.cappleapple.needsnotnecessities.modifier.ModifierOperation;
import org.junit.jupiter.api.Test;

class HealthServicesTest {
    @Test
    void baseHealthModesProduceCorrectAttributeOperations() {
        var additive = BaseHealthService.createModifier(BaseHealthMode.ADD, 10.0D);
        assertEquals(10.0D, additive.amount());
        assertEquals(ModifierOperation.ADD, additive.operation());

        var multiplied = BaseHealthService.createModifier(BaseHealthMode.MULTIPLY, 1.5D);
        assertEquals(0.5D, multiplied.amount());
        assertEquals(ModifierOperation.MULTIPLY_BASE, multiplied.operation());
    }

    @Test
    void combatCooldownHandlesFreshExpiredAndClockRollbackCases() {
        assertFalse(PassiveRegenerationService.isCombatCooldownActive(-1L, 100L, 200L));
        assertTrue(PassiveRegenerationService.isCombatCooldownActive(100L, 250L, 200L));
        assertFalse(PassiveRegenerationService.isCombatCooldownActive(100L, 300L, 200L));
        assertTrue(PassiveRegenerationService.isCombatCooldownActive(300L, 100L, 200L));
    }
}
