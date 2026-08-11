package com.cappleapple.needsnotnecessities.survival.meal;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.Test;

class MealIngredientResolverTest {
    private static final ResourceLocation GRAIN = id("grain");
    private static final ResourceLocation VEGETABLES = id("vegetables");

    @Test
    void inheritsDefinitionsThroughEveryPreparedFoodLayer() {
        MealIngredientResolver<String> resolver = resolver(
                Map.of(
                        Items.BREAD, GRAIN,
                        Items.WHEAT, GRAIN),
                Map.of(
                        Items.COOKIE, List.<ItemStack[]>of(slot(Items.BREAD)),
                        Items.BREAD, List.<ItemStack[]>of(slot(Items.WHEAT))),
                Set.of(Items.COOKIE, Items.BREAD));

        MealIngredientProfile<String> profile = resolver.resolveIngredient(slot(Items.COOKIE));
        MealIngredientProfile.Group<String> grain = group(profile, GRAIN);

        assertEquals(List.of(1.0D, 1.0D), grain.occurrenceWeights());
        MealGroupDiminishingTracker diminishing = new MealGroupDiminishingTracker();
        double totalWeight = grain.occurrenceWeights().stream()
                .mapToDouble(weight -> weight * diminishing.nextMultiplier(GRAIN, 0.5D))
                .sum();
        assertEquals(1.5D, totalWeight, 1.0E-9D);
    }

    @Test
    void nestedTagAlternativesRemainAveragedInsteadOfDoubleCounted() {
        MealIngredientResolver<String> resolver = resolver(
                Map.of(
                        Items.BREAD, GRAIN,
                        Items.WHEAT, GRAIN,
                        Items.CARROT, VEGETABLES),
                Map.of(
                        Items.COOKIE, List.<ItemStack[]>of(slot(Items.BREAD)),
                        Items.BREAD, List.<ItemStack[]>of(slot(Items.WHEAT))),
                Set.of(Items.COOKIE, Items.BREAD));

        MealIngredientProfile<String> profile = resolver.resolveIngredient(
                new ItemStack[]{new ItemStack(Items.COOKIE), new ItemStack(Items.CARROT)});

        assertEquals(List.of(0.5D, 0.5D), group(profile, GRAIN).occurrenceWeights());
        assertEquals(List.of(0.5D), group(profile, VEGETABLES).occurrenceWeights());
    }

    @Test
    void cyclicPreparedFoodRecipesStopAtTheRepeatedItem() {
        MealIngredientResolver<String> resolver = resolver(
                Map.of(
                        Items.APPLE, GRAIN,
                        Items.COOKIE, VEGETABLES),
                Map.of(
                        Items.APPLE, List.<ItemStack[]>of(slot(Items.COOKIE)),
                        Items.COOKIE, List.<ItemStack[]>of(slot(Items.APPLE))),
                Set.of(Items.APPLE, Items.COOKIE));

        MealIngredientProfile<String> profile = resolver.resolveIngredient(slot(Items.APPLE));

        assertEquals(List.of(1.0D, 1.0D), group(profile, GRAIN).occurrenceWeights());
        assertEquals(List.of(1.0D), group(profile, VEGETABLES).occurrenceWeights());

        MealIngredientProfile<String> reversed = resolver.resolveIngredient(slot(Items.COOKIE));
        assertEquals(List.of(1.0D), group(reversed, GRAIN).occurrenceWeights());
        assertEquals(List.of(1.0D, 1.0D), group(reversed, VEGETABLES).occurrenceWeights());
    }

    private static MealIngredientResolver<String> resolver(
            Map<Item, ResourceLocation> definitions,
            Map<Item, List<ItemStack[]>> recipes,
            Set<Item> expandable) {
        return new MealIngredientResolver<>(
                stack -> {
                    ResourceLocation group = definitions.get(stack.getItem());
                    return group == null
                            ? List.of()
                            : List.of(new MealIngredientResolver.Definition<>(group, group.toString()));
                },
                item -> recipes.getOrDefault(item, List.of()),
                stack -> expandable.contains(stack.getItem()));
    }

    private static ItemStack[] slot(Item item) {
        return new ItemStack[]{new ItemStack(item)};
    }

    private static MealIngredientProfile.Group<String> group(
            MealIngredientProfile<String> profile,
            ResourceLocation id) {
        return profile.groups().stream()
                .filter(group -> group.id().equals(id))
                .findFirst()
                .orElseThrow();
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath("needs_not_necessities", path);
    }
}
