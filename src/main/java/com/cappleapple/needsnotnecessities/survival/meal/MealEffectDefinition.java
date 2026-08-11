package com.cappleapple.needsnotnecessities.survival.meal;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public record MealEffectDefinition(
        ResourceLocation id,
        Optional<Item> item,
        List<TagKey<Item>> tags,
        Map<String, Double> traits,
        List<MealBonusTemplate> bonuses,
        double scoreBonus,
        double durationBonusHours) {

    public MealEffectDefinition {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(item, "item");
        Objects.requireNonNull(tags, "tags");
        Objects.requireNonNull(traits, "traits");
        Objects.requireNonNull(bonuses, "bonuses");
        if (item.isPresent() == !tags.isEmpty()
                || traits.values().stream().anyMatch(value -> !Double.isFinite(value))
                || !Double.isFinite(scoreBonus)
                || !Double.isFinite(durationBonusHours)) {
            throw new IllegalArgumentException("Meal effect needs exactly one item selector or tag list and finite values: " + id);
        }
        tags = List.copyOf(tags);
        traits = Map.copyOf(traits);
        bonuses = List.copyOf(bonuses);
    }

    public boolean matches(ItemStack stack) {
        return item.map(stack::is).orElse(false) || tags.stream().anyMatch(stack::is);
    }
}
