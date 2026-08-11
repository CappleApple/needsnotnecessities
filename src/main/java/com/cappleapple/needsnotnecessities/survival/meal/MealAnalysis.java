package com.cappleapple.needsnotnecessities.survival.meal;

import com.cappleapple.needsnotnecessities.modifier.SurvivalModifier;
import java.util.List;
import java.util.Map;

public record MealAnalysis(
        double score,
        double durationBiologicalHours,
        int recipeComplexity,
        Map<String, Double> traits,
        List<SurvivalModifier> modifiers,
        double qualityValue) {

    public MealAnalysis {
        if (!Double.isFinite(score)
                || !Double.isFinite(durationBiologicalHours)
                || durationBiologicalHours < 0.0D
                || recipeComplexity < 0
                || !Double.isFinite(qualityValue)) {
            throw new IllegalArgumentException("Meal analysis values must be finite and non-negative where applicable");
        }
        traits = Map.copyOf(traits);
        modifiers = List.copyOf(modifiers);
    }
}
