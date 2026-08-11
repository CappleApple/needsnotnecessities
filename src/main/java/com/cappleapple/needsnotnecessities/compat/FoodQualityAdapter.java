package com.cappleapple.needsnotnecessities.compat;

import com.cappleapple.needsnotnecessities.survival.meal.MealAnalysis;
import net.minecraft.world.item.ItemStack;

@FunctionalInterface
public interface FoodQualityAdapter {
    MealAnalysis apply(ItemStack stack, MealAnalysis analysis);
}
