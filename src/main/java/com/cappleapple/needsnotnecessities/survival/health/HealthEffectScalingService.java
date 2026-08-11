package com.cappleapple.needsnotnecessities.survival.health;

import com.cappleapple.needsnotnecessities.config.ServerConfig;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

public final class HealthEffectScalingService {
    private HealthEffectScalingService() {
    }

    public static float scaleFor(LivingEntity entity, float vanillaAmount) {
        ServerConfig config = ServerConfig.INSTANCE;
        if (!(entity instanceof Player) || !config.scaleHealthEffectsWithMaxHealth.getAsBoolean()) {
            return vanillaAmount;
        }
        return scaleAmount(
                vanillaAmount,
                entity.getMaxHealth(),
                config.healthEffectReferenceMaxHealth.getAsDouble());
    }

    public static float scaleAmount(float vanillaAmount, float maximumHealth, double referenceMaximumHealth) {
        if (!Float.isFinite(vanillaAmount)
                || !Float.isFinite(maximumHealth)
                || maximumHealth < 0.0F
                || !Double.isFinite(referenceMaximumHealth)
                || referenceMaximumHealth <= 0.0D) {
            throw new IllegalArgumentException("Health-effect scaling inputs must be finite and valid");
        }
        if (vanillaAmount <= 0.0F) {
            return vanillaAmount;
        }
        double scaled = vanillaAmount * maximumHealth / referenceMaximumHealth;
        return (float) Math.min(Float.MAX_VALUE, scaled);
    }
}
