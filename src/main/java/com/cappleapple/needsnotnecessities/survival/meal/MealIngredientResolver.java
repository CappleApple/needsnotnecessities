package com.cappleapple.needsnotnecessities.survival.meal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** Resolves food ingredients through every nested prepared-food recipe. */
public final class MealIngredientResolver<T> {
    private final Function<ItemStack, List<Definition<T>>> definitionMatcher;
    private final RecipeIngredientProvider recipeIngredients;
    private final Predicate<ItemStack> expandableFood;
    private final Map<Item, MealIngredientProfile<T>> cache = new HashMap<>();
    private final Set<Item> visiting = new HashSet<>();

    public MealIngredientResolver(
            Function<ItemStack, List<Definition<T>>> definitionMatcher,
            RecipeIngredientProvider recipeIngredients,
            Predicate<ItemStack> expandableFood) {
        this.definitionMatcher = Objects.requireNonNull(definitionMatcher, "definitionMatcher");
        this.recipeIngredients = Objects.requireNonNull(recipeIngredients, "recipeIngredients");
        this.expandableFood = Objects.requireNonNull(expandableFood, "expandableFood");
    }

    public MealIngredientProfile<T> resolveIngredient(ItemStack[] alternatives) {
        return resolveIngredientInternal(alternatives).profile();
    }

    private Resolution<T> resolveIngredientInternal(ItemStack[] alternatives) {
        if (alternatives.length == 0) {
            return new Resolution<>(MealIngredientProfile.empty(), false);
        }
        List<MealIngredientProfile<T>> resolved = new ArrayList<>(alternatives.length);
        boolean cyclic = false;
        for (ItemStack alternative : alternatives) {
            Resolution<T> item = resolveItem(alternative);
            resolved.add(item.profile());
            cyclic |= item.cyclic();
        }
        return new Resolution<>(MealIngredientProfile.average(resolved), cyclic);
    }

    private Resolution<T> resolveItem(ItemStack stack) {
        if (stack.isEmpty()) {
            return new Resolution<>(MealIngredientProfile.empty(), false);
        }
        Item item = stack.getItem();
        MealIngredientProfile<T> cached = cache.get(item);
        if (cached != null) {
            return new Resolution<>(cached, false);
        }

        MealIngredientProfile.Builder<T> result = MealIngredientProfile.builder();
        for (Definition<T> definition : definitionMatcher.apply(stack)) {
            result.add(definition.groupId(), definition.value(), 1.0D);
        }
        MealIngredientProfile<T> direct = result.build();
        if (!expandableFood.test(stack)) {
            cache.put(item, direct);
            return new Resolution<>(direct, false);
        }
        if (!visiting.add(item)) {
            return new Resolution<>(direct, true);
        }
        boolean cyclic = false;
        try {
            for (ItemStack[] ingredient : recipeIngredients.ingredientsFor(item)) {
                Resolution<T> resolvedIngredient = resolveIngredientInternal(ingredient);
                result.append(resolvedIngredient.profile());
                cyclic |= resolvedIngredient.cyclic();
            }
        } finally {
            visiting.remove(item);
        }
        MealIngredientProfile<T> resolved = result.build();
        if (!cyclic) {
            cache.put(item, resolved);
        }
        return new Resolution<>(resolved, cyclic);
    }

    public record Definition<T>(ResourceLocation groupId, T value) {
        public Definition {
            Objects.requireNonNull(groupId, "groupId");
            Objects.requireNonNull(value, "value");
        }
    }

    @FunctionalInterface
    public interface RecipeIngredientProvider {
        List<ItemStack[]> ingredientsFor(Item item);
    }

    private record Resolution<T>(MealIngredientProfile<T> profile, boolean cyclic) {
    }
}
