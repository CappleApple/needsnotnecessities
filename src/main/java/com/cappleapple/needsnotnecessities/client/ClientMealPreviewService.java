package com.cappleapple.needsnotnecessities.client;

import com.cappleapple.needsnotnecessities.NeedsNotNecessities;
import com.cappleapple.needsnotnecessities.config.ServerConfig;
import com.cappleapple.needsnotnecessities.modifier.ModifierOperation;
import com.cappleapple.needsnotnecessities.modifier.SurvivalModifier;
import com.cappleapple.needsnotnecessities.modifier.SurvivalModifierService;
import com.cappleapple.needsnotnecessities.survival.meal.MealBonusAggregator;
import com.cappleapple.needsnotnecessities.survival.meal.MealGroupDiminishingTracker;
import com.cappleapple.needsnotnecessities.survival.meal.MealIngredientProfile;
import com.cappleapple.needsnotnecessities.survival.meal.MealIngredientResolver;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;

final class ClientMealPreviewService {
    private static final ResourceLocation QUALITY_COMPONENT =
            ResourceLocation.fromNamespaceAndPath("quality_food", "quality");
    private static RecipeManager indexedManager;
    private static int indexedRecipeCount = -1;
    private static Map<Item, List<RecipeHolder<?>>> recipesByOutput = Map.of();

    private ClientMealPreviewService() {
    }

    static Preview analyze(ItemStack eatenStack, double foodHours) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return new Preview(0.0D, 0, List.of());
        }
        ensureRecipeIndex(minecraft.level.getRecipeManager(), minecraft);
        Accumulator accumulator = new Accumulator();
        accumulator.accept(eatenStack, 2.0D, 1.0D);
        MealIngredientResolver<CompoundTag> ingredientResolver = new MealIngredientResolver<>(
                ClientMealPreviewService::matchingDefinitions,
                ClientMealPreviewService::recipeIngredients,
                stack -> stack.getFoodProperties(minecraft.player) != null);

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
                        ClientSurvivalCache.number(
                                "meal_same_group_diminishing_factor",
                                ServerConfig.INSTANCE.mealSameGroupDiminishingFactor.getAsDouble()));
                if (containsPreparedAlternative(alternatives)) {
                    complexity++;
                }
            }
        }

        int configuredMaximum = ClientSurvivalCache.snapshot().contains("meal_maximum_bonuses", Tag.TAG_INT)
                ? ClientSurvivalCache.snapshot().getInt("meal_maximum_bonuses")
                : ServerConfig.INSTANCE.mealMaximumBonuses.getAsInt();
        double strength = ClientSurvivalCache.number(
                "meal_effect_strength_multiplier", ServerConfig.INSTANCE.mealEffectStrengthMultiplier.getAsDouble());
        List<SurvivalModifier> selected = new ArrayList<>(MealBonusAggregator.aggregate(
                accumulator.traits,
                accumulator.bonuses,
                configuredMaximum,
                strength));
        if (selected.isEmpty() && configuredMaximum > 0) {
            ResourceLocation fallback = NeedsNotNecessities.id("meal/fallback/recovery");
            selected.add(new SurvivalModifier(
                    fallback,
                    SurvivalModifierService.PASSIVE_REGENERATION,
                    0.025D * strength,
                    ModifierOperation.MULTIPLY_TOTAL));
        }

        double baseDuration = ClientSurvivalCache.number(
                "meal_base_duration_hours", ServerConfig.INSTANCE.mealBaseDurationHours.getAsDouble());
        double durationPerComplexity = ClientSurvivalCache.number(
                "meal_duration_per_complexity", ServerConfig.INSTANCE.mealDurationPerComplexity.getAsDouble());
        double maximumDuration = ClientSurvivalCache.number(
                "meal_maximum_duration_hours", ServerConfig.INSTANCE.mealMaximumDurationHours.getAsDouble());
        double duration = Math.clamp(
                baseDuration + complexity * durationPerComplexity + accumulator.durationBonusHours,
                0.0D,
                maximumDuration);
        QualityScaling quality = qualityScaling(eatenStack);
        List<SurvivalModifier> modifiers = selected.stream()
                .map(modifier -> new SurvivalModifier(
                        modifier.id(), modifier.target(), modifier.amount() * quality.strength(), modifier.operation()))
                .toList();
        return new Preview(duration * quality.duration(), complexity, modifiers);
    }

    private static void ensureRecipeIndex(RecipeManager manager, Minecraft minecraft) {
        int count = manager.getRecipes().size();
        if (indexedManager == manager && indexedRecipeCount == count) {
            return;
        }
        Map<Item, List<RecipeHolder<?>>> rebuilt = new HashMap<>();
        for (RecipeHolder<?> holder : manager.getRecipes()) {
            ItemStack result = holder.value().getResultItem(minecraft.level.registryAccess());
            if (!result.isEmpty()) {
                rebuilt.computeIfAbsent(result.getItem(), ignored -> new ArrayList<>()).add(holder);
            }
        }
        rebuilt.replaceAll((item, holders) -> List.copyOf(holders));
        indexedManager = manager;
        indexedRecipeCount = count;
        recipesByOutput = Map.copyOf(rebuilt);
    }

    private static RecipeHolder<?> bestRecipe(Item item) {
        return recipesByOutput.getOrDefault(item, List.of()).stream()
                .max(Comparator.comparingInt(ClientMealPreviewService::directComplexity))
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
            if (recipesByOutput.containsKey(alternative.getItem())) {
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

    private static List<MealIngredientResolver.Definition<CompoundTag>> matchingDefinitions(ItemStack stack) {
        List<MealIngredientResolver.Definition<CompoundTag>> matched = new ArrayList<>();
        for (CompoundTag definition : ClientSurvivalCache.list("meal_effect_definitions")) {
            ResourceLocation groupId = ResourceLocation.tryParse(definition.getString("id"));
            if (groupId != null && matches(stack, definition)) {
                matched.add(new MealIngredientResolver.Definition<>(groupId, definition));
            }
        }
        return List.copyOf(matched);
    }

    private static boolean matches(ItemStack stack, CompoundTag definition) {
        if (definition.contains("item", Tag.TAG_STRING)) {
            ResourceLocation itemId = ResourceLocation.tryParse(definition.getString("item"));
            return itemId != null && itemId.equals(BuiltInRegistries.ITEM.getKey(stack.getItem()));
        }
        for (Tag element : definition.getList("tags", Tag.TAG_COMPOUND)) {
            ResourceLocation tagId = ResourceLocation.tryParse(((CompoundTag) element).getString("id"));
            if (tagId != null && stack.is(TagKey.create(Registries.ITEM, tagId))) {
                return true;
            }
        }
        return false;
    }

    private static QualityScaling qualityScaling(ItemStack stack) {
        CompoundTag snapshot = ClientSurvivalCache.snapshot();
        if (!snapshot.getBoolean("quality_food_enabled")) {
            return QualityScaling.NONE;
        }
        DataComponentType<?> component = BuiltInRegistries.DATA_COMPONENT_TYPE
                .getOptional(QUALITY_COMPONENT).orElse(null);
        if (component == null) {
            return QualityScaling.NONE;
        }
        try {
            Object quality = getComponent(stack, component);
            if (quality == null) {
                return QualityScaling.NONE;
            }
            int level = ((Number) invoke(quality, "level")).intValue();
            Object holder = invoke(quality, "getType");
            Object qualityType = invoke(holder, "value");
            double configuredDuration = ((Number) invoke(qualityType, "durationMultiplier")).doubleValue();
            double influence = ClientSurvivalCache.number("quality_food_duration_influence", 1.0D);
            double duration = Math.clamp(
                    1.0D + (configuredDuration - 1.0D) * influence,
                    0.0D,
                    ClientSurvivalCache.number("quality_food_maximum_duration_multiplier", 3.0D));
            double strength = Math.min(
                    ClientSurvivalCache.number("quality_food_maximum_strength_multiplier", 1.15D),
                    1.0D + Math.max(0, level)
                            * ClientSurvivalCache.number("quality_food_strength_per_level", 0.025D));
            return new QualityScaling(duration, strength);
        } catch (ReflectiveOperationException | LinkageError | ClassCastException ignored) {
            return QualityScaling.NONE;
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static Object getComponent(ItemStack stack, DataComponentType<?> type) {
        return stack.get((DataComponentType) type);
    }

    private static Object invoke(Object owner, String methodName) throws ReflectiveOperationException {
        Method method = owner.getClass().getMethod(methodName);
        return method.invoke(owner);
    }

    record Preview(double durationHours, int recipeComplexity, List<SurvivalModifier> modifiers) {
        Preview {
            modifiers = List.copyOf(modifiers);
        }
    }

    private static final class Accumulator {
        private final Map<String, Double> traits = new HashMap<>();
        private final List<MealBonusAggregator.Contribution> bonuses = new ArrayList<>();
        private final MealGroupDiminishingTracker groupDiminishing = new MealGroupDiminishingTracker();
        private double durationBonusHours;

        private void accept(ItemStack stack, double definitionWeight, double bonusWeight) {
            if (stack.isEmpty() || definitionWeight <= 0.0D || bonusWeight < 0.0D) {
                return;
            }
            for (CompoundTag definition : ClientSurvivalCache.list("meal_effect_definitions")) {
                if (!matches(stack, definition)) {
                    continue;
                }
                acceptDefinition(definition, definitionWeight, bonusWeight);
            }
        }

        private void acceptIngredient(
                MealIngredientProfile<CompoundTag> profile,
                double diminishingFactor) {
            for (MealIngredientProfile.Group<CompoundTag> group : profile.groups()) {
                for (double occurrenceWeight : group.occurrenceWeights()) {
                    double weight = occurrenceWeight
                            * groupDiminishing.nextMultiplier(group.id(), diminishingFactor);
                    acceptDefinition(group.definition(), weight, weight);
                }
            }
        }

        private void acceptDefinition(CompoundTag definition, double definitionWeight, double bonusWeight) {
            CompoundTag traitTag = definition.getCompound("traits");
            traitTag.getAllKeys().forEach(trait ->
                    traits.merge(trait, traitTag.getDouble(trait) * definitionWeight, Double::sum));
            for (Tag element : definition.getList("bonuses", Tag.TAG_COMPOUND)) {
                CompoundTag bonus = (CompoundTag) element;
                ResourceLocation id = ResourceLocation.tryParse(bonus.getString("id"));
                ResourceLocation target = ResourceLocation.tryParse(bonus.getString("target"));
                String trait = bonus.getString("trait");
                if (id == null || target == null || trait.isBlank()) {
                    continue;
                }
                bonuses.add(new MealBonusAggregator.Contribution(
                        trait,
                        target,
                        bonus.getDouble("amount"),
                        ModifierOperation.parse(bonus.getString("operation")),
                        bonusWeight));
            }
            durationBonusHours += definition.getDouble("duration_bonus_hours") * definitionWeight;
        }
    }

    private record QualityScaling(double duration, double strength) {
        private static final QualityScaling NONE = new QualityScaling(1.0D, 1.0D);
    }
}
