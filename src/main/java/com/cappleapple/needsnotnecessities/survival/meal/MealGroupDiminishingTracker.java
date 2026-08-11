package com.cappleapple.needsnotnecessities.survival.meal;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;

/** Tracks successive direct recipe ingredients matched by each meal-effect definition. */
public final class MealGroupDiminishingTracker {
    private final Map<ResourceLocation, Integer> occurrences = new HashMap<>();

    public double nextMultiplier(ResourceLocation groupId, double diminishingFactor) {
        if (groupId == null
                || !Double.isFinite(diminishingFactor)
                || diminishingFactor < 0.0D
                || diminishingFactor > 1.0D) {
            throw new IllegalArgumentException("Meal group and diminishing factor must be valid");
        }
        int priorOccurrences = occurrences.getOrDefault(groupId, 0);
        occurrences.put(groupId, priorOccurrences + 1);
        return Math.pow(diminishingFactor, priorOccurrences);
    }
}
