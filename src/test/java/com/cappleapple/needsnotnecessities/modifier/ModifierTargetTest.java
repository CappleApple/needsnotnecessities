package com.cappleapple.needsnotnecessities.modifier;

import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

class ModifierTargetTest {
    @Test
    void builtInDefaultAttributeTargetsExist() {
        assertTrue(BuiltInRegistries.ATTRIBUTE.getHolder(id("generic.movement_speed")).isPresent());
        assertTrue(BuiltInRegistries.ATTRIBUTE.getHolder(id("generic.attack_speed")).isPresent());
        assertTrue(BuiltInRegistries.ATTRIBUTE.getHolder(id("generic.attack_damage")).isPresent());
        assertTrue(BuiltInRegistries.ATTRIBUTE.getHolder(id("generic.max_health")).isPresent());
        assertTrue(BuiltInRegistries.ATTRIBUTE.getHolder(id("player.block_break_speed")).isPresent());
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath("minecraft", path);
    }
}
