package com.cappleapple.needsnotnecessities.survival.meal;

import com.cappleapple.needsnotnecessities.survival.food.PlacedFoodResolver;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.block.Block;

/** One recipe graph for server analysis and client previews, including in-world serving conversions. */
public final class MealRecipeIndex {
    private final Map<Item, RecipeIngredients> recipes;

    private MealRecipeIndex(Map<Item, RecipeIngredients> recipes) {
        this.recipes = Map.copyOf(recipes);
    }

    public static MealRecipeIndex build(RecipeManager manager, HolderLookup.Provider registries) {
        Map<Item, RecipeIngredients> recipes = new HashMap<>();
        // Stable tie breaking preserves identical client/server choices.
        manager.getRecipes().stream().sorted(Comparator.comparing(RecipeHolder::id)).forEach(holder -> {
            ItemStack output = holder.value().getResultItem(registries);
            if (output.isEmpty()) {
                return;
            }
            List<ItemStack[]> ingredients = holder.value().getIngredients().stream()
                    .map(Ingredient::getItems).filter(items -> items.length > 0).toList();
            int complexity = ingredients.size() + (holder.value() instanceof AbstractCookingRecipe ? 1 : 0);
            RecipeIngredients candidate = new RecipeIngredients(ingredients, complexity);
            recipes.merge(output.getItem(), candidate,
                    (existing, replacement) -> replacement.complexity() > existing.complexity() ? replacement : existing);
        });

        Map<Item, Set<Item>> servingSources = new HashMap<>();
        for (Block block : BuiltInRegistries.BLOCK) {
            Item source = block.asItem();
            if (source == Items.AIR) {
                continue;
            }
            for (ItemStack serving : PlacedFoodResolver.servings(block)) {
                if (serving.getItem() != source) {
                    servingSources.computeIfAbsent(serving.getItem(), ignored -> new LinkedHashSet<>()).add(source);
                }
            }
        }
        servingSources.forEach((serving, sources) -> {
            // Native recipes take precedence (e.g. rice rolls can also be arranged on a medley).
            // Missing serving recipes get a single ingredient slot leading back to the full food.
            ItemStack[] alternatives = sources.stream()
                    .sorted(Comparator.comparing(BuiltInRegistries.ITEM::getKey))
                    .map(ItemStack::new).toArray(ItemStack[]::new);
            recipes.putIfAbsent(serving, new RecipeIngredients(List.<ItemStack[]>of(alternatives), 1));
        });
        return new MealRecipeIndex(recipes);
    }

    public List<ItemStack[]> ingredientsFor(Item item) {
        RecipeIngredients recipe = recipes.get(item);
        return recipe == null ? List.of() : recipe.ingredients();
    }

    public int complexity(Item item) {
        RecipeIngredients recipe = recipes.get(item);
        return recipe == null ? 0 : recipe.complexity();
    }

    public boolean contains(Item item) {
        return recipes.containsKey(item);
    }

    private record RecipeIngredients(List<ItemStack[]> ingredients, int complexity) {
    }
}
