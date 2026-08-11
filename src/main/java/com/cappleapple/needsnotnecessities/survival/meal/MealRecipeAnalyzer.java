package com.cappleapple.needsnotnecessities.survival.meal;

import com.cappleapple.needsnotnecessities.NeedsNotNecessities;
import com.cappleapple.needsnotnecessities.config.ServerConfig;
import com.cappleapple.needsnotnecessities.compat.QualityFoodCompat;
import com.cappleapple.needsnotnecessities.modifier.ModifierOperation;
import com.cappleapple.needsnotnecessities.modifier.SurvivalModifier;
import com.cappleapple.needsnotnecessities.modifier.SurvivalModifierService;
import com.cappleapple.needsnotnecessities.api.provider.SurvivalProviderRegistry;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;

/**
 * Builds a hidden meal score from the eaten item, its best known recipe and
 * datapack-defined traits. Recipe indexing is rebuilt after data reloads.
 */
public final class MealRecipeAnalyzer {
    private static RecipeManager indexedManager;
    private static Map<Item, List<RecipeHolder<?>>> recipesByOutput = Map.of();

    private MealRecipeAnalyzer() {
    }

    public static MealAnalysis analyze(ServerPlayer player, ItemStack eatenStack, double foodHours) {
        ensureRecipeIndex(player);
        ServerConfig config = ServerConfig.INSTANCE;
        Accumulator accumulator = new Accumulator();
        accumulator.accept(eatenStack, 2.0D, 1.0D);
        MealIngredientResolver<MealEffectDefinition> ingredientResolver = new MealIngredientResolver<>(
                stack -> MealEffectManager.INSTANCE.matching(stack).stream()
                        .map(definition -> new MealIngredientResolver.Definition<>(definition.id(), definition))
                        .toList(),
                MealRecipeAnalyzer::recipeIngredients,
                stack -> stack.getFoodProperties(player) != null);

        RecipeHolder<?> recipe = bestRecipe(eatenStack.getItem());
        int complexity = 0;
        if (recipe != null) {
            complexity = directComplexity(recipe);
            for (Ingredient ingredient : recipe.value().getIngredients()) {
                ItemStack[] alternatives = ingredient.getItems();
                if (alternatives.length == 0) {
                    continue;
                }
                accumulator.acceptIngredient(
                        ingredientResolver.resolveIngredient(alternatives),
                        config.mealSameGroupDiminishingFactor.getAsDouble());
                if (containsPreparedAlternative(alternatives)) {
                    complexity++;
                }
            }
        }

        int maximumBonuses = config.mealMaximumBonuses.getAsInt();
        List<Map.Entry<String, Double>> strongestTraits = accumulator.traits.entrySet().stream()
                .filter(entry -> entry.getValue() > 0.0D)
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed().thenComparing(Map.Entry::getKey))
                .limit(maximumBonuses)
                .toList();

        double strength = config.mealEffectStrengthMultiplier.getAsDouble();
        List<SurvivalModifier> selectedModifiers = new ArrayList<>(MealBonusAggregator.aggregate(
                accumulator.traits,
                accumulator.bonuses,
                maximumBonuses,
                strength));

        if (selectedModifiers.isEmpty() && maximumBonuses > 0) {
            selectedModifiers.add(new SurvivalModifier(
                    NeedsNotNecessities.id("meal/fallback/recovery"),
                    SurvivalModifierService.PASSIVE_REGENERATION,
                    0.025D * strength,
                    ModifierOperation.MULTIPLY_TOTAL));
            accumulator.traits.putIfAbsent("recovery", 0.25D);
        }

        double score = Math.max(0.0D,
                foodHours
                        + complexity * config.mealScorePerComplexity.getAsDouble()
                        + accumulator.scoreBonus
                        + strongestTraits.stream().mapToDouble(Map.Entry::getValue).sum());
        double duration = config.mealBaseDurationHours.getAsDouble()
                + complexity * config.mealDurationPerComplexity.getAsDouble()
                + accumulator.durationBonusHours;
        duration = Math.clamp(duration, 0.0D, config.mealMaximumDurationHours.getAsDouble());

        MealAnalysis baseAnalysis = new MealAnalysis(
                score,
                duration,
                complexity,
                accumulator.traits,
                List.copyOf(selectedModifiers),
                0.0D);
        MealAnalysis result = QualityFoodCompat.apply(eatenStack, baseAnalysis);
        if (config.isEnabled(com.cappleapple.needsnotnecessities.survival.SurvivalModule.COMPATIBILITY)) {
            for (var analyzer : SurvivalProviderRegistry.mealAnalyzers()) {
                MealAnalysis replacement = analyzer.modify(player, eatenStack, foodHours, result);
                if (replacement != null) {
                    result = replacement;
                }
            }
        }
        return result;
    }

    public static synchronized void clearCache() {
        indexedManager = null;
        recipesByOutput = Map.of();
    }

    private static synchronized void ensureRecipeIndex(ServerPlayer player) {
        RecipeManager manager = player.server.getRecipeManager();
        if (indexedManager == manager) {
            return;
        }
        Map<Item, List<RecipeHolder<?>>> index = new HashMap<>();
        for (RecipeHolder<?> holder : manager.getRecipes()) {
            ItemStack result = holder.value().getResultItem(player.registryAccess());
            if (!result.isEmpty()) {
                index.computeIfAbsent(result.getItem(), ignored -> new ArrayList<>()).add(holder);
            }
        }
        index.replaceAll((item, recipes) -> List.copyOf(recipes));
        recipesByOutput = Map.copyOf(index);
        indexedManager = manager;
        NeedsNotNecessities.LOGGER.debug("Indexed {} recipe outputs for Active Meal analysis", index.size());
    }

    private static RecipeHolder<?> bestRecipe(Item item) {
        return recipesByOutput.getOrDefault(item, List.of()).stream()
                .max(Comparator.comparingInt(MealRecipeAnalyzer::directComplexity))
                .orElse(null);
    }

    private static int directComplexity(RecipeHolder<?> holder) {
        int ingredients = (int) holder.value().getIngredients().stream()
                .filter(ingredient -> !ingredient.hasNoItems())
                .count();
        return ingredients + (holder.value() instanceof AbstractCookingRecipe ? 1 : 0);
    }

    private static boolean containsPreparedAlternative(ItemStack[] alternatives) {
        for (ItemStack alternative : alternatives) {
            if (!recipesByOutput.getOrDefault(alternative.getItem(), List.of()).isEmpty()) {
                return true;
            }
        }
        return false;
    }

    private static List<ItemStack[]> recipeIngredients(Item item) {
        RecipeHolder<?> recipe = bestRecipe(item);
        if (recipe == null) {
            return List.of();
        }
        return recipe.value().getIngredients().stream()
                .map(Ingredient::getItems)
                .filter(items -> items.length > 0)
                .toList();
    }

    private static final class Accumulator {
        private final Map<String, Double> traits = new HashMap<>();
        private final List<MealBonusAggregator.Contribution> bonuses = new ArrayList<>();
        private final MealGroupDiminishingTracker groupDiminishing = new MealGroupDiminishingTracker();
        private double scoreBonus;
        private double durationBonusHours;

        private void accept(ItemStack stack, double definitionWeight, double bonusWeight) {
            if (stack.isEmpty() || definitionWeight <= 0.0D || bonusWeight < 0.0D) {
                return;
            }
            for (MealEffectDefinition definition : MealEffectManager.INSTANCE.matching(stack)) {
                acceptDefinition(definition, definitionWeight, bonusWeight);
            }
        }

        private void acceptIngredient(
                MealIngredientProfile<MealEffectDefinition> profile,
                double diminishingFactor) {
            for (MealIngredientProfile.Group<MealEffectDefinition> group : profile.groups()) {
                for (double occurrenceWeight : group.occurrenceWeights()) {
                    double weight = occurrenceWeight
                            * groupDiminishing.nextMultiplier(group.id(), diminishingFactor);
                    acceptDefinition(group.definition(), weight, weight);
                }
            }
        }

        private void acceptDefinition(MealEffectDefinition definition, double definitionWeight, double bonusWeight) {
            definition.traits().forEach((trait, value) ->
                    traits.merge(trait, value * definitionWeight, Double::sum));
            for (MealBonusTemplate bonus : definition.bonuses()) {
                bonuses.add(new MealBonusAggregator.Contribution(
                        bonus.trait(), bonus.target(), bonus.amount(), bonus.operation(), bonusWeight));
            }
            scoreBonus += definition.scoreBonus() * definitionWeight;
            durationBonusHours += definition.durationBonusHours() * definitionWeight;
        }
    }
}
