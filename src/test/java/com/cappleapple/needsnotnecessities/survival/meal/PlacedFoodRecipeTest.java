package com.cappleapple.needsnotnecessities.survival.meal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.cappleapple.needsnotnecessities.survival.food.PlacedFoodResolver;
import java.util.List;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import org.junit.jupiter.api.Test;

class PlacedFoodRecipeTest {
    @Test
    void cakeBiteNutritionIsPerServingAndDoesNotMakeTheItemHeldEdible() {
        ItemStack cake = new ItemStack(Items.CAKE);
        assertNull(cake.getFoodProperties(null));
        assertTrue(PlacedFoodResolver.isPlacedFood(cake));
        assertFalse(PlacedFoodResolver.isPlacedFood(new ItemStack(Items.STONE)));
        var food = PlacedFoodResolver.foodProperties(cake, null);
        assertEquals(2, food.nutrition());
        assertEquals(0.4F, food.saturation(), 0.00001F);
    }

    @Test
    void nestedCakeKeepsTheExistingGroupDiminishingWeights() {
        RecipeManager manager = new RecipeManager(RegistryAccess.EMPTY);
        manager.replaceRecipes(List.of(
                recipe("cake", Items.CAKE, Items.BREAD, Items.BREAD),
                recipe("bread", Items.BREAD, Items.WHEAT)));
        MealRecipeIndex index = MealRecipeIndex.build(manager, RegistryAccess.EMPTY);
        var grain = ResourceLocation.withDefaultNamespace("grain");
        MealIngredientResolver<String> resolver = new MealIngredientResolver<>(
                stack -> stack.is(Items.WHEAT)
                        ? List.of(new MealIngredientResolver.Definition<>(grain, "grain")) : List.of(),
                index::ingredientsFor, stack -> stack.getFoodProperties(null) != null || PlacedFoodResolver.isPlacedFood(stack));
        var groups = resolver.resolveIngredient(new ItemStack[]{new ItemStack(Items.CAKE)}).groups();
        assertEquals(1, groups.size());
        assertEquals(List.of(1.0D, 1.0D), groups.getFirst().occurrenceWeights());
        MealGroupDiminishingTracker diminishing = new MealGroupDiminishingTracker();
        assertEquals(1.5D, groups.getFirst().occurrenceWeights().stream()
                .mapToDouble(weight -> weight * diminishing.nextMultiplier(grain, 0.5D)).sum(), 0.00001D);
    }

    @Test
    void equalComplexityChoicesHaveStableRecipeIdOrdering() {
        RecipeManager manager = new RecipeManager(RegistryAccess.EMPTY);
        manager.replaceRecipes(List.of(
                recipe("z", Items.CAKE, Items.CARROT),
                recipe("a", Items.CAKE, Items.APPLE)));
        MealRecipeIndex index = MealRecipeIndex.build(manager, RegistryAccess.EMPTY);
        assertTrue(index.ingredientsFor(Items.CAKE).getFirst()[0].is(Items.APPLE));
    }

    @Test
    void rebuiltIndexUsesChangedRecipesEvenWhenRecipeCountIsUnchanged() {
        RecipeManager manager = new RecipeManager(RegistryAccess.EMPTY);
        manager.replaceRecipes(List.of(recipe("cake", Items.CAKE, Items.APPLE)));
        MealRecipeIndex oldIndex = MealRecipeIndex.build(manager, RegistryAccess.EMPTY);
        manager.replaceRecipes(List.of(recipe("cake", Items.CAKE, Items.CARROT)));
        MealRecipeIndex newIndex = MealRecipeIndex.build(manager, RegistryAccess.EMPTY);
        assertTrue(oldIndex.ingredientsFor(Items.CAKE).getFirst()[0].is(Items.APPLE));
        assertTrue(newIndex.ingredientsFor(Items.CAKE).getFirst()[0].is(Items.CARROT));
    }

    private static RecipeHolder<?> recipe(String id, Item result, Item... ingredients) {
        NonNullList<Ingredient> inputs = NonNullList.create();
        for (Item ingredient : ingredients) {
            inputs.add(Ingredient.of(ingredient));
        }
        return new RecipeHolder<>(ResourceLocation.withDefaultNamespace(id),
                new ShapelessRecipe("", CraftingBookCategory.MISC, new ItemStack(result), inputs));
    }
}
