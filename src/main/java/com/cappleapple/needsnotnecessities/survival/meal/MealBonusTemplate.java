package com.cappleapple.needsnotnecessities.survival.meal;

import com.cappleapple.needsnotnecessities.modifier.ModifierOperation;
import java.util.Objects;
import net.minecraft.resources.ResourceLocation;

public record MealBonusTemplate(
        ResourceLocation id,
        String trait,
        ResourceLocation target,
        double amount,
        ModifierOperation operation) {

    public MealBonusTemplate {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(trait, "trait");
        Objects.requireNonNull(target, "target");
        Objects.requireNonNull(operation, "operation");
        if (trait.isBlank() || !Double.isFinite(amount)) {
            throw new IllegalArgumentException("Meal bonus trait must be named and its amount finite: " + id);
        }
    }
}
