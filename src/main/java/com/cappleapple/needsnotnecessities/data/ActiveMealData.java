package com.cappleapple.needsnotnecessities.data;

import com.cappleapple.needsnotnecessities.modifier.ModifierOperation;
import com.cappleapple.needsnotnecessities.modifier.SurvivalModifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Map;
import java.util.LinkedHashMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;

public record ActiveMealData(
        ResourceLocation sourceItem,
        String displayName,
        double score,
        double remainingBiologicalHours,
        List<SurvivalModifier> modifiers,
        int recipeComplexity,
        Map<String, Double> traits,
        double qualityValue) {

    public ActiveMealData {
        Objects.requireNonNull(sourceItem, "sourceItem");
        Objects.requireNonNull(displayName, "displayName");
        Objects.requireNonNull(modifiers, "modifiers");
        Objects.requireNonNull(traits, "traits");
        if (!Double.isFinite(score)
                || !Double.isFinite(remainingBiologicalHours)
                || remainingBiologicalHours < 0.0D
                || recipeComplexity < 0
                || !Double.isFinite(qualityValue)) {
            throw new IllegalArgumentException("Meal score and remaining time must be finite; time cannot be negative");
        }
        modifiers = List.copyOf(modifiers);
        traits = Map.copyOf(traits);
    }

    public ActiveMealData(
            ResourceLocation sourceItem,
            String displayName,
            double score,
            double remainingBiologicalHours,
            List<SurvivalModifier> modifiers) {
        this(sourceItem, displayName, score, remainingBiologicalHours, modifiers, 0, Map.of(), 0.0D);
    }

    public CompoundTag toTag() {
        CompoundTag tag = new CompoundTag();
        tag.putString("source_item", sourceItem.toString());
        tag.putString("display_name", displayName);
        tag.putDouble("score", score);
        tag.putDouble("remaining_biological_hours", remainingBiologicalHours);
        ListTag modifierTags = new ListTag();
        for (SurvivalModifier modifier : modifiers) {
            CompoundTag modifierTag = new CompoundTag();
            modifierTag.putString("id", modifier.id().toString());
            modifierTag.putString("target", modifier.target().toString());
            modifierTag.putDouble("amount", modifier.amount());
            modifierTag.putString("operation", modifier.operation().name());
            modifierTags.add(modifierTag);
        }
        tag.put("modifiers", modifierTags);
        tag.putInt("recipe_complexity", recipeComplexity);
        ListTag traitTags = new ListTag();
        traits.forEach((trait, value) -> {
            CompoundTag traitTag = new CompoundTag();
            traitTag.putString("trait", trait);
            traitTag.putDouble("value", value);
            traitTags.add(traitTag);
        });
        tag.put("traits", traitTags);
        tag.putDouble("quality_value", qualityValue);
        return tag;
    }

    public static Optional<ActiveMealData> fromTag(CompoundTag tag) {
        ResourceLocation sourceItem = ResourceLocation.tryParse(tag.getString("source_item"));
        if (sourceItem == null) {
            return Optional.empty();
        }
        List<SurvivalModifier> modifiers = new ArrayList<>();
        ListTag modifierTags = tag.getList("modifiers", Tag.TAG_COMPOUND);
        for (Tag element : modifierTags) {
            CompoundTag modifierTag = (CompoundTag) element;
            ResourceLocation id = ResourceLocation.tryParse(modifierTag.getString("id"));
            ResourceLocation target = ResourceLocation.tryParse(modifierTag.getString("target"));
            if (id == null || target == null) {
                continue;
            }
            try {
                modifiers.add(new SurvivalModifier(
                        id,
                        target,
                        modifierTag.getDouble("amount"),
                        ModifierOperation.parse(modifierTag.getString("operation"))));
            } catch (IllegalArgumentException ignored) {
                // Skip only the malformed persisted modifier; the meal itself remains recoverable.
            }
        }
        Map<String, Double> traits = new LinkedHashMap<>();
        for (Tag element : tag.getList("traits", Tag.TAG_COMPOUND)) {
            CompoundTag traitTag = (CompoundTag) element;
            String trait = traitTag.getString("trait");
            double value = traitTag.getDouble("value");
            if (!trait.isBlank() && Double.isFinite(value)) {
                traits.put(trait, value);
            }
        }
        try {
            return Optional.of(new ActiveMealData(
                    sourceItem,
                    tag.getString("display_name"),
                    tag.getDouble("score"),
                    Math.max(0.0D, tag.getDouble("remaining_biological_hours")),
                    modifiers,
                    Math.max(0, tag.getInt("recipe_complexity")),
                    traits,
                    tag.getDouble("quality_value")));
        } catch (IllegalArgumentException ignored) {
            return Optional.empty();
        }
    }
}
