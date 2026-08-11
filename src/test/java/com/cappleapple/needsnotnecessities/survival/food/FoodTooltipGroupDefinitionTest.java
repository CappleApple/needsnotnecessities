package com.cappleapple.needsnotnecessities.survival.food;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

class FoodTooltipGroupDefinitionTest {
    @Test
    void groupIncludesItsConfiguredHourBoundaries() {
        FoodTooltipGroupDefinition group = new FoodTooltipGroupDefinition(
                ResourceLocation.fromNamespaceAndPath("test", "light_snack"),
                "Light Snack",
                0.0D,
                2.0D,
                0xFFFFFF,
                "Small bite");
        assertTrue(group.contains(1.0D));
        assertTrue(group.contains(2.0D));
        assertFalse(group.contains(2.001D));
    }
}
