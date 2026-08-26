package com.cappleapple.needsnotnecessities.survival.health;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

public final class AbsorptionScalingService {
    private AbsorptionScalingService() {
    }

    public static float scaledMaximum(LivingEntity entity, float vanillaMaximum) {
        return HealthEffectScalingService.scaleFor(entity, vanillaMaximum);
    }

    public static void refreshGrantedAbsorption(LivingEntity entity, int amplifier) {
        if (entity.level().isClientSide() || !(entity instanceof Player)) {
            return;
        }
        float scaledGrant = HealthEffectScalingService.scaleFor(entity, 4.0F * (amplifier + 1));
        entity.setAbsorptionAmount(Math.max(entity.getAbsorptionAmount(), scaledGrant));
    }

    public static void rescaleAfterMaxHealthChange(
            Player player,
            float previousMaximumAbsorption,
            float previousAbsorption) {
        player.setAbsorptionAmount(rescaleAmount(
                previousAbsorption,
                previousMaximumAbsorption,
                player.getMaxAbsorption()));
    }

    static float rescaleAmount(float amount, float previousMaximum, float newMaximum) {
        if (!Float.isFinite(amount)
                || !Float.isFinite(previousMaximum)
                || !Float.isFinite(newMaximum)
                || amount < 0.0F
                || previousMaximum < 0.0F
                || newMaximum < 0.0F) {
            throw new IllegalArgumentException("Absorption values must be finite and non-negative");
        }
        if (previousMaximum <= 0.0F) {
            return Math.min(amount, newMaximum);
        }
        return Math.min(newMaximum, amount * newMaximum / previousMaximum);
    }
}
