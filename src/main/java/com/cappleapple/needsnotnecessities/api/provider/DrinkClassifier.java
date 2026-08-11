package com.cappleapple.needsnotnecessities.api.provider;

import com.cappleapple.needsnotnecessities.survival.thirst.ThirstService;
import net.minecraft.world.item.ItemStack;

@FunctionalInterface
public interface DrinkClassifier {
    ThirstService.DrinkKind classify(ItemStack stack);
}
