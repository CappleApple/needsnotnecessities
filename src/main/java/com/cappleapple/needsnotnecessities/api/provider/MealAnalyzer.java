package com.cappleapple.needsnotnecessities.api.provider;

import com.cappleapple.needsnotnecessities.survival.meal.MealAnalysis;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

@FunctionalInterface
public interface MealAnalyzer {
    MealAnalysis modify(ServerPlayer player, ItemStack stack, double foodHours, MealAnalysis current);
}
