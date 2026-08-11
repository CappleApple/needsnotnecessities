package com.cappleapple.needsnotnecessities.mixin;

import com.cappleapple.needsnotnecessities.survival.health.HealthEffectScalingService;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(targets = "net.minecraft.world.effect.HealOrHarmMobEffect")
abstract class HealOrHarmMobEffectMixin {
    @Redirect(
            method = "applyEffectTick",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;heal(F)V"))
    private void needsNotNecessities$scaleTickHealing(LivingEntity entity, float amount) {
        entity.heal(HealthEffectScalingService.scaleFor(entity, amount));
    }

    @Redirect(
            method = "applyInstantenousEffect",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;heal(F)V"))
    private void needsNotNecessities$scaleInstantHealing(LivingEntity entity, float amount) {
        entity.heal(HealthEffectScalingService.scaleFor(entity, amount));
    }
}
