package com.cappleapple.needsnotnecessities.survival.meal;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.Test;

class MealEffectDefinitionTest {
    @Test
    void exactItemSelectorMatchesOnlyItsItem() {
        MealEffectDefinition definition = new MealEffectDefinition(
                id("beef"), Optional.of(Items.BEEF), List.of(), Map.of("power", 2.0D), List.of(), 1.0D, 2.0D);
        assertTrue(definition.matches(new ItemStack(Items.BEEF)));
        assertFalse(definition.matches(new ItemStack(Items.CARROT)));
    }

    @Test
    void selectorMustBeExactlyItemOrTagList() {
        TagKey<Item> meat = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("c", "food/meat"));
        assertThrows(IllegalArgumentException.class, () -> new MealEffectDefinition(
                id("invalid"), Optional.of(Items.BEEF), List.of(meat), Map.of(), List.of(), 0.0D, 0.0D));
        assertThrows(IllegalArgumentException.class, () -> new MealEffectDefinition(
                id("invalid_empty"), Optional.empty(), List.of(), Map.of(), List.of(), 0.0D, 0.0D));
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath("test", path);
    }
}
