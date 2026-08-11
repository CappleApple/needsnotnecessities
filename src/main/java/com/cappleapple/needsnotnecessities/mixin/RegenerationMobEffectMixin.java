package com.cappleapple.needsnotnecessities.mixin;

import com.cappleapple.needsnotnecessities.survival.health.HealthEffectScalingService;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(targets = "net.minecraft.world.effect.RegenerationMobEffect")
abstract class RegenerationMobEffectMixin {
    @Redirect(
            method = "applyEffectTick",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;heal(F)V"))
    private void needsNotNecessities$scaleRegenerationHealing(LivingEntity entity, float amount) {
        entity.heal(HealthEffectScalingService.scaleFor(entity, amount));
    }
}
