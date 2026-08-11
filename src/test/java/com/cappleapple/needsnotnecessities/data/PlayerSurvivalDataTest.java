package com.cappleapple.needsnotnecessities.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.cappleapple.needsnotnecessities.modifier.ModifierOperation;
import com.cappleapple.needsnotnecessities.modifier.SurvivalModifier;
import java.util.List;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

class PlayerSurvivalDataTest {
    @Test
    void persistentFieldsRoundTripThroughNbt() {
        ResourceLocation hunger = id("hunger");
        ResourceLocation cooldown = id("notification/hungry");
        ResourceLocation modifierId = id("modifier/test");
        ResourceLocation attributeId = ResourceLocation.fromNamespaceAndPath("minecraft", "generic.max_health");
        ResourceLocation scalarId = id("passive_regeneration");

        PlayerSurvivalData original = new PlayerSurvivalData();
        original.markInitialized();
        original.advanceBiologicalTicks(1234L);
        original.setStatePosition(hunger, 17.5D);
        original.setRetainedComfort(24.25D, 6000L);
        original.setNotificationCooldown(cooldown, 9000L);
        original.replaceAppliedAttributeModifiers(Map.of(modifierId, attributeId));
        original.replaceComputedScalarModifiers(Map.of(scalarId, 1.25D));
        original.markCombat(777L);
        original.setPendingDeathHealthReset(true);
        original.setPendingRespawnPenaltyMessage(true);
        original.setActiveMeal(new ActiveMealData(
                ResourceLocation.fromNamespaceAndPath("minecraft", "beef"),
                "Steak",
                4.5D,
                12.25D,
                List.of(new SurvivalModifier(modifierId, attributeId, 2.0D, ModifierOperation.ADD)),
                3,
                Map.of("power", 2.5D, "recovery", 1.0D),
                2.0D));

        PlayerSurvivalData restored = new PlayerSurvivalData();
        restored.deserializeNBT(null, original.serializeNBT(null));

        assertTrue(restored.initialized());
        assertEquals(1234L, restored.biologicalAgeTicks());
        assertEquals(17.5D, restored.statePosition(hunger));
        assertEquals(24.25D, restored.retainedComfort());
        assertEquals(6000L, restored.comfortRetentionTicks());
        assertEquals(9000L, restored.notificationCooldown(cooldown));
        assertEquals(attributeId, restored.appliedAttributeModifiers().get(modifierId));
        assertEquals(1.25D, restored.computedScalarModifiers().get(scalarId));
        assertTrue(restored.pendingDeathHealthReset());
        assertTrue(restored.pendingRespawnPenaltyMessage());
        assertEquals(777L, restored.lastCombatGameTick());
        assertTrue(restored.activeMeal().isPresent());
        assertEquals("Steak", restored.activeMeal().orElseThrow().displayName());
        assertEquals(3, restored.activeMeal().orElseThrow().recipeComplexity());
        assertEquals(2.5D, restored.activeMeal().orElseThrow().traits().get("power"));
        assertEquals(2.0D, restored.activeMeal().orElseThrow().qualityValue());

        restored.advanceBiologicalTicks(Long.MAX_VALUE);
        assertEquals(Long.MAX_VALUE, restored.biologicalAgeTicks());

        restored.clearActiveMeal();
        restored.clearComfort();
        assertFalse(restored.activeMeal().isPresent());
        assertEquals(0.0D, restored.retainedComfort());
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath("needs_not_necessities", path);
    }
}
