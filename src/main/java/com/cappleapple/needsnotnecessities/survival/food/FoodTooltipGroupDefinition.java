package com.cappleapple.needsnotnecessities.survival.food;

import java.util.Objects;
import net.minecraft.resources.ResourceLocation;

public record FoodTooltipGroupDefinition(
        ResourceLocation id,
        String displayName,
        double minimumHours,
        double maximumHours,
        int color,
        String description) {

    public FoodTooltipGroupDefinition {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(displayName, "displayName");
        description = description == null ? "" : description;
        if (displayName.isBlank()
                || !Double.isFinite(minimumHours)
                || minimumHours < 0.0D
                || !Double.isFinite(maximumHours)
                || maximumHours < minimumHours) {
            throw new IllegalArgumentException("Invalid food tooltip grouping: " + id);
        }
    }

    public boolean contains(double foodHours) {
        return foodHours >= minimumHours && foodHours <= maximumHours;
    }
}
