package com.cappleapple.needsnotnecessities.mixin;

import com.cappleapple.needsnotnecessities.survival.health.AbsorptionScalingService;
import com.cappleapple.needsnotnecessities.survival.health.BaseHealthService;
import com.cappleapple.needsnotnecessities.survival.health.MaxHealthTransitionService;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
abstract class LivingEntityMixin {
    @Unique
    private MaxHealthTransitionService.Snapshot needsNotNecessities$maxHealthTransition;

    @Inject(method = "getMaxAbsorption", at = @At("RETURN"), cancellable = true)
    private void needsNotNecessities$scaleMaximumAbsorption(CallbackInfoReturnable<Float> callback) {
        LivingEntity entity = (LivingEntity) (Object) this;
        callback.setReturnValue(AbsorptionScalingService.scaledMaximum(entity, callback.getReturnValue()));
    }

    @Inject(method = "onAttributeUpdated", at = @At("HEAD"))
    private void needsNotNecessities$captureMaxHealthTransition(
            Holder<Attribute> attribute,
            CallbackInfo callback) {
        if (attribute.is(BaseHealthService.MAX_HEALTH_ATTRIBUTE) && (Object) this instanceof ServerPlayer player) {
            needsNotNecessities$maxHealthTransition = MaxHealthTransitionService.capture(player);
        }
    }

    @Inject(method = "onAttributeUpdated", at = @At("RETURN"))
    private void needsNotNecessities$finishMaxHealthTransition(
            Holder<Attribute> attribute,
            CallbackInfo callback) {
        if (!attribute.is(BaseHealthService.MAX_HEALTH_ATTRIBUTE)
                || !((Object) this instanceof ServerPlayer player)
                || needsNotNecessities$maxHealthTransition == null) {
            return;
        }
        MaxHealthTransitionService.Snapshot transition = needsNotNecessities$maxHealthTransition;
        needsNotNecessities$maxHealthTransition = null;
        MaxHealthTransitionService.finishVanillaAttributeUpdate(player, transition);
    }
}
