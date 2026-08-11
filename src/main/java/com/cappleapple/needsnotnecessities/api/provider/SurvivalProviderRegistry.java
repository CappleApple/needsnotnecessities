package com.cappleapple.needsnotnecessities.api.provider;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public final class SurvivalProviderRegistry {
    private static final CopyOnWriteArrayList<ComfortProvider> COMFORT = new CopyOnWriteArrayList<>();
    private static final CopyOnWriteArrayList<MealAnalyzer> MEALS = new CopyOnWriteArrayList<>();
    private static final CopyOnWriteArrayList<DrinkClassifier> DRINKS = new CopyOnWriteArrayList<>();
    private static final CopyOnWriteArrayList<SurvivalModifierProvider> MODIFIERS = new CopyOnWriteArrayList<>();

    private SurvivalProviderRegistry() {
    }

    public static void registerComfortProvider(ComfortProvider provider) {
        COMFORT.addIfAbsent(provider);
    }

    public static void registerMealAnalyzer(MealAnalyzer provider) {
        MEALS.addIfAbsent(provider);
    }

    public static void registerDrinkClassifier(DrinkClassifier provider) {
        DRINKS.addIfAbsent(provider);
    }

    public static void registerModifierProvider(SurvivalModifierProvider provider) {
        MODIFIERS.addIfAbsent(provider);
    }

    public static boolean unregister(Object provider) {
        return COMFORT.remove(provider) | MEALS.remove(provider) | DRINKS.remove(provider) | MODIFIERS.remove(provider);
    }

    public static List<ComfortProvider> comfortProviders() {
        return List.copyOf(COMFORT);
    }

    public static List<MealAnalyzer> mealAnalyzers() {
        return List.copyOf(MEALS);
    }

    public static List<DrinkClassifier> drinkClassifiers() {
        return List.copyOf(DRINKS);
    }

    public static List<SurvivalModifierProvider> modifierProviders() {
        return List.copyOf(MODIFIERS);
    }
}
