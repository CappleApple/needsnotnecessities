package com.cappleapple.needsnotnecessities.modifier;

import java.util.Objects;
import net.minecraft.resources.ResourceLocation;

public record SurvivalModifier(
        ResourceLocation id,
        ResourceLocation target,
        double amount,
        ModifierOperation operation) {

    public SurvivalModifier {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(target, "target");
        Objects.requireNonNull(operation, "operation");
        if (!Double.isFinite(amount)) {
            throw new IllegalArgumentException("Modifier amount must be finite: " + id);
        }
    }
}
