package com.cappleapple.needsnotnecessities.survival.meal;

import com.cappleapple.needsnotnecessities.NeedsNotNecessities;
import com.cappleapple.needsnotnecessities.modifier.ModifierOperation;
import com.cappleapple.needsnotnecessities.modifier.SurvivalModifier;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;

public final class MealBonusAggregator {
    private MealBonusAggregator() {
    }

    public static List<SurvivalModifier> aggregate(
            Map<String, Double> traitScores,
            List<Contribution> contributions,
            int maximumBonuses,
            double strengthMultiplier) {
        if (maximumBonuses < 0
                || !Double.isFinite(strengthMultiplier)
                || strengthMultiplier < 0.0D) {
            throw new IllegalArgumentException("Meal bonus limits and strength must be non-negative and finite");
        }
        if (maximumBonuses == 0) {
            return List.of();
        }

        List<String> rankedTraits = traitScores.entrySet().stream()
                .filter(entry -> entry.getValue() > 0.0D)
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed().thenComparing(Map.Entry::getKey))
                .map(Map.Entry::getKey)
                .toList();
        Map<BonusKey, Double> selected = new LinkedHashMap<>();
        for (String trait : rankedTraits) {
            Map<BonusKey, Double> forTrait = new HashMap<>();
            for (Contribution contribution : contributions) {
                if (trait.equals(contribution.trait())) {
                    forTrait.merge(
                            new BonusKey(contribution.target(), contribution.operation()),
                            contribution.amount() * contribution.weight(),
                            Double::sum);
                }
            }
            List<Map.Entry<BonusKey, Double>> rankedGroups = forTrait.entrySet().stream()
                    .filter(entry -> entry.getValue() != 0.0D)
                    .sorted((first, second) -> {
                        int strength = Double.compare(Math.abs(second.getValue()), Math.abs(first.getValue()));
                        return strength != 0
                                ? strength
                                : first.getKey().sortKey().compareTo(second.getKey().sortKey());
                    })
                    .toList();
            for (Map.Entry<BonusKey, Double> group : rankedGroups) {
                if (selected.containsKey(group.getKey())) {
                    selected.merge(group.getKey(), group.getValue(), Double::sum);
                } else if (selected.size() < maximumBonuses) {
                    selected.put(group.getKey(), group.getValue());
                }
            }
        }

        return selected.entrySet().stream()
                .filter(entry -> entry.getValue() != 0.0D)
                .map(entry -> new SurvivalModifier(
                        aggregateId(entry.getKey()),
                        entry.getKey().target(),
                        entry.getValue() * strengthMultiplier,
                        entry.getKey().operation()))
                .toList();
    }

    private static ResourceLocation aggregateId(BonusKey key) {
        String path = "meal/aggregate/"
                + key.target().getNamespace()
                + "/"
                + key.target().getPath()
                + "/"
                + key.operation().name().toLowerCase(Locale.ROOT);
        return NeedsNotNecessities.id(path);
    }

    public record Contribution(
            String trait,
            ResourceLocation target,
            double amount,
            ModifierOperation operation,
            double weight) {
        public Contribution {
            if (trait == null
                    || trait.isBlank()
                    || target == null
                    || operation == null
                    || !Double.isFinite(amount)
                    || !Double.isFinite(weight)
                    || weight < 0.0D) {
                throw new IllegalArgumentException("Meal bonus contribution values must be present and finite");
            }
        }
    }

    private record BonusKey(ResourceLocation target, ModifierOperation operation) {
        private String sortKey() {
            return target + "/" + operation.name();
        }
    }
}
