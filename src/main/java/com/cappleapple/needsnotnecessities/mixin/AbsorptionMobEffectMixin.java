package com.cappleapple.needsnotnecessities.mixin;

import com.cappleapple.needsnotnecessities.survival.health.AbsorptionScalingService;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net.minecraft.world.effect.AbsorptionMobEffect")
abstract class AbsorptionMobEffectMixin {
    @Inject(method = "onEffectStarted", at = @At("RETURN"))
    private void needsNotNecessities$scaleGrantedAbsorption(
            LivingEntity entity,
            int amplifier,
            CallbackInfo callback) {
        AbsorptionScalingService.refreshGrantedAbsorption(entity, amplifier);
    }
}
