package com.cappleapple.needsnotnecessities.survival.meal;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import net.minecraft.resources.ResourceLocation;

/** Ordered per-group occurrences gathered from a possibly nested recipe tree. */
public final class MealIngredientProfile<T> {
    private final List<Group<T>> groups;

    private MealIngredientProfile(List<Group<T>> groups) {
        this.groups = List.copyOf(groups);
    }

    public static <T> MealIngredientProfile<T> empty() {
        return new MealIngredientProfile<>(List.of());
    }

    public static <T> Builder<T> builder() {
        return new Builder<>();
    }

    public static <T> MealIngredientProfile<T> average(List<MealIngredientProfile<T>> alternatives) {
        if (alternatives.isEmpty()) {
            return empty();
        }
        Map<ResourceLocation, AveragedGroup<T>> averaged = new LinkedHashMap<>();
        for (MealIngredientProfile<T> alternative : alternatives) {
            for (Group<T> group : alternative.groups()) {
                averaged.compute(group.id(), (id, current) -> {
                    AveragedGroup<T> result = current == null
                            ? new AveragedGroup<>(group.definition(), new ArrayList<>())
                            : current;
                    while (result.weightSums().size() < group.occurrenceWeights().size()) {
                        result.weightSums().add(0.0D);
                    }
                    for (int index = 0; index < group.occurrenceWeights().size(); index++) {
                        result.weightSums().set(
                                index,
                                result.weightSums().get(index) + group.occurrenceWeights().get(index));
                    }
                    return result;
                });
            }
        }
        Builder<T> result = builder();
        averaged.forEach((id, group) -> group.weightSums().forEach(sum ->
                result.add(id, group.definition(), sum / alternatives.size())));
        return result.build();
    }

    public List<Group<T>> groups() {
        return groups;
    }

    public record Group<T>(ResourceLocation id, T definition, List<Double> occurrenceWeights) {
        public Group {
            Objects.requireNonNull(id, "id");
            Objects.requireNonNull(definition, "definition");
            occurrenceWeights = List.copyOf(occurrenceWeights);
        }
    }

    public static final class Builder<T> {
        private final Map<ResourceLocation, MutableGroup<T>> groups = new LinkedHashMap<>();

        public Builder<T> add(ResourceLocation id, T definition, double weight) {
            Objects.requireNonNull(id, "id");
            Objects.requireNonNull(definition, "definition");
            if (!Double.isFinite(weight) || weight < 0.0D) {
                throw new IllegalArgumentException("Meal ingredient weights must be non-negative and finite");
            }
            if (weight == 0.0D) {
                return this;
            }
            MutableGroup<T> group = groups.computeIfAbsent(id, ignored ->
                    new MutableGroup<>(definition, new ArrayList<>()));
            group.occurrenceWeights().add(weight);
            return this;
        }

        public Builder<T> append(MealIngredientProfile<T> profile) {
            for (Group<T> group : profile.groups()) {
                group.occurrenceWeights().forEach(weight -> add(group.id(), group.definition(), weight));
            }
            return this;
        }

        public MealIngredientProfile<T> build() {
            return new MealIngredientProfile<>(groups.entrySet().stream()
                    .map(entry -> new Group<>(
                            entry.getKey(),
                            entry.getValue().definition(),
                            entry.getValue().occurrenceWeights()))
                    .toList());
        }
    }

    private record MutableGroup<T>(T definition, List<Double> occurrenceWeights) {
    }

    private record AveragedGroup<T>(T definition, List<Double> weightSums) {
    }
}
