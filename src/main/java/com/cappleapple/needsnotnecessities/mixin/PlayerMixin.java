package com.cappleapple.needsnotnecessities.mixin;

import com.cappleapple.needsnotnecessities.config.ServerConfig;
import com.cappleapple.needsnotnecessities.survival.SurvivalModule;
import com.cappleapple.needsnotnecessities.survival.hunger.HungerService;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
abstract class PlayerMixin {
    @Inject(method = "canEat", at = @At("HEAD"), cancellable = true)
    private void needsNotNecessities$allowFoodWithCustomHunger(
            boolean canAlwaysEat,
            CallbackInfoReturnable<Boolean> callback) {
        if (ServerConfig.INSTANCE.isEnabled(SurvivalModule.HUNGER)) {
            callback.setReturnValue((Object) this instanceof ServerPlayer player
                    ? HungerService.canEat(player)
                    : true);
        }
    }
}
