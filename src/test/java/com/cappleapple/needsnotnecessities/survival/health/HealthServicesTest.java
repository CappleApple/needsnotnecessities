package com.cappleapple.needsnotnecessities.survival.health;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.cappleapple.needsnotnecessities.config.BaseHealthMode;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import org.junit.jupiter.api.Test;

class HealthServicesTest {
    @Test
    void baseHealthModesTransformTheAttributeBaseDirectly() {
        assertEquals(30.0D, BaseHealthService.configuredBaseValue(20.0D, BaseHealthMode.ADD, 10.0D));
        assertEquals(30.0D, BaseHealthService.configuredBaseValue(20.0D, BaseHealthMode.MULTIPLY, 1.5D));
    }

    @Test
    void repeatedReloadsReuseTheOriginalBaseWithoutCompounding() {
        assertEquals(20.0D, BaseHealthService.originalBaseValue(30.0D, true, 20.0D, 30.0D));
        assertEquals(30.0D, BaseHealthService.configuredBaseValue(
                BaseHealthService.originalBaseValue(30.0D, true, 20.0D, 30.0D),
                BaseHealthMode.ADD,
                10.0D));
    }

    @Test
    void externallyReplacedBaseBecomesTheNewUnadjustedBase() {
        assertEquals(40.0D, BaseHealthService.originalBaseValue(40.0D, true, 20.0D, 30.0D));
    }

    @Test
    void configuredBaseIsVisibleBeforeVanillaModifierCalculation() {
        Holder<Attribute> attribute = Holder.direct(new RangedAttribute("test.max_health", 20.0D, 1.0D, 1024.0D));
        AttributeInstance instance = new AttributeInstance(attribute, ignored -> {
        });

        instance.setBaseValue(BaseHealthService.configuredBaseValue(20.0D, BaseHealthMode.ADD, 10.0D));
        instance.addTransientModifier(new AttributeModifier(
                ResourceLocation.fromNamespaceAndPath("test", "percentage_bonus"),
                0.5D,
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));

        assertEquals(30.0D, instance.getBaseValue());
        assertEquals(45.0D, instance.getValue());
    }

    @Test
    void combatCooldownHandlesFreshExpiredAndClockRollbackCases() {
        assertFalse(PassiveRegenerationService.isCombatCooldownActive(-1L, 100L, 200L));
        assertTrue(PassiveRegenerationService.isCombatCooldownActive(100L, 250L, 200L));
        assertFalse(PassiveRegenerationService.isCombatCooldownActive(100L, 300L, 200L));
        assertTrue(PassiveRegenerationService.isCombatCooldownActive(300L, 100L, 200L));
    }
}
