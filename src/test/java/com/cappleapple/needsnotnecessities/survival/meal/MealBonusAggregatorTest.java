package com.cappleapple.needsnotnecessities.survival.meal;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.cappleapple.needsnotnecessities.modifier.ModifierOperation;
import com.cappleapple.needsnotnecessities.modifier.SurvivalModifier;
import com.cappleapple.needsnotnecessities.modifier.SurvivalModifierService;
import java.util.List;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

class MealBonusAggregatorTest {
    private static final ResourceLocation ARMOR = id("minecraft", "generic.armor");
    private static final ResourceLocation ATTACK_DAMAGE = id("minecraft", "generic.attack_damage");

    @Test
    void sumsEveryIngredientContributionByTargetAndOperation() {
        List<SurvivalModifier> result = MealBonusAggregator.aggregate(
                Map.of("defense", 4.0D, "recovery", 3.0D, "power", 2.0D),
                List.of(
                        contribution("defense", ARMOR, 1.0D, ModifierOperation.ADD),
                        contribution("recovery", SurvivalModifierService.PASSIVE_REGENERATION, 0.08D, ModifierOperation.MULTIPLY_TOTAL),
                        contribution("power", ATTACK_DAMAGE, 0.05D, ModifierOperation.MULTIPLY_TOTAL),
                        contribution("defense", ARMOR, 1.0D, ModifierOperation.ADD),
                        contribution("recovery", SurvivalModifierService.PASSIVE_REGENERATION, 0.025D, ModifierOperation.MULTIPLY_TOTAL)),
                5,
                1.0D);

        assertEquals(3, result.size());
        assertEquals(2.0D, amountFor(result, ARMOR), 1.0E-9D);
        assertEquals(0.105D, amountFor(result, SurvivalModifierService.PASSIVE_REGENERATION), 1.0E-9D);
        assertEquals(0.05D, amountFor(result, ATTACK_DAMAGE), 1.0E-9D);
    }

    @Test
    void sameTargetFromDistinctFoodGroupsAddsNumerically() {
        List<SurvivalModifier> result = MealBonusAggregator.aggregate(
                Map.of("vegetable_defense", 1.0D, "grain_defense", 1.0D),
                List.of(
                        contribution("vegetable_defense", ARMOR, 1.0D, ModifierOperation.ADD),
                        contribution("grain_defense", ARMOR, 1.0D, ModifierOperation.ADD)),
                5,
                1.0D);

        assertEquals(1, result.size());
        assertEquals(2.0D, amountFor(result, ARMOR), 1.0E-9D);
    }

    @Test
    void steakAndPotatoesCombinesDiminishedVegetablesAndRiceRecovery() {
        List<SurvivalModifier> result = MealBonusAggregator.aggregate(
                Map.of("defense", 2.25D, "recovery", 2.5D),
                List.of(
                        new MealBonusAggregator.Contribution(
                                "defense", ARMOR, 1.0D, ModifierOperation.ADD, 1.0D),
                        new MealBonusAggregator.Contribution(
                                "recovery", SurvivalModifierService.PASSIVE_REGENERATION,
                                0.08D, ModifierOperation.MULTIPLY_TOTAL, 1.0D),
                        new MealBonusAggregator.Contribution(
                                "defense", ARMOR, 1.0D, ModifierOperation.ADD, 0.5D),
                        new MealBonusAggregator.Contribution(
                                "recovery", SurvivalModifierService.PASSIVE_REGENERATION,
                                0.08D, ModifierOperation.MULTIPLY_TOTAL, 0.5D),
                        new MealBonusAggregator.Contribution(
                                "recovery", SurvivalModifierService.PASSIVE_REGENERATION,
                                0.025D, ModifierOperation.MULTIPLY_TOTAL, 1.0D)),
                5,
                1.0D);

        assertEquals(1.5D, amountFor(result, ARMOR), 1.0E-9D);
        assertEquals(0.145D, amountFor(result, SurvivalModifierService.PASSIVE_REGENERATION), 1.0E-9D);
    }

    @Test
    void alternativeIngredientWeightsArePreservedDuringSummation() {
        List<SurvivalModifier> result = MealBonusAggregator.aggregate(
                Map.of("defense", 1.0D),
                List.of(
                        new MealBonusAggregator.Contribution(
                                "defense", ARMOR, 2.0D, ModifierOperation.ADD, 0.5D),
                        new MealBonusAggregator.Contribution(
                                "defense", ARMOR, 4.0D, ModifierOperation.ADD, 0.5D)),
                5,
                1.0D);

        assertEquals(3.0D, amountFor(result, ARMOR), 1.0E-9D);
    }

    private static MealBonusAggregator.Contribution contribution(
            String trait,
            ResourceLocation target,
            double amount,
            ModifierOperation operation) {
        return new MealBonusAggregator.Contribution(trait, target, amount, operation, 1.0D);
    }

    private static double amountFor(List<SurvivalModifier> modifiers, ResourceLocation target) {
        return modifiers.stream()
                .filter(modifier -> modifier.target().equals(target))
                .findFirst()
                .orElseThrow()
                .amount();
    }

    private static ResourceLocation id(String namespace, String path) {
        return ResourceLocation.fromNamespaceAndPath(namespace, path);
    }
}
