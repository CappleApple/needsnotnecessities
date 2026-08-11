package com.cappleapple.needsnotnecessities.mixin;

import com.cappleapple.needsnotnecessities.config.ServerConfig;
import com.cappleapple.needsnotnecessities.survival.SurvivalModule;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FoodData.class)
abstract class FoodDataMixin {
    @Shadow
    private int foodLevel;

    @Shadow
    private float saturationLevel;

    @Shadow
    private float exhaustionLevel;

    @Shadow
    private int tickTimer;

    @Shadow
    private int lastFoodLevel;

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void needsNotNecessities$disableVanillaHunger(Player player, CallbackInfo callback) {
        if (ServerConfig.INSTANCE.isEnabled(SurvivalModule.HUNGER)) {
            foodLevel = 20;
            lastFoodLevel = 20;
            saturationLevel = 0.0F;
            exhaustionLevel = 0.0F;
            tickTimer = 0;
            callback.cancel();
        }
    }
}
