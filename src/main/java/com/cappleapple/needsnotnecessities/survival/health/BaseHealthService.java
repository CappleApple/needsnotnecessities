package com.cappleapple.needsnotnecessities.survival.health;

import com.cappleapple.needsnotnecessities.NeedsNotNecessities;
import com.cappleapple.needsnotnecessities.config.BaseHealthMode;
import com.cappleapple.needsnotnecessities.config.ServerConfig;
import com.cappleapple.needsnotnecessities.data.ModAttachments;
import com.cappleapple.needsnotnecessities.data.PlayerSurvivalData;
import com.cappleapple.needsnotnecessities.survival.SurvivalModule;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;

public final class BaseHealthService {
    /**
     * The ID used by versions through 1.1.4 for the old transient base-health modifier.
     */
    public static final ResourceLocation MODIFIER_ID = NeedsNotNecessities.id("base_health");
    public static final ResourceLocation MAX_HEALTH_ATTRIBUTE = ResourceLocation.withDefaultNamespace("generic.max_health");

    private BaseHealthService() {
    }

    public static void applyConfiguredBase(ServerPlayer player) {
        AttributeInstance maxHealth = player.getAttribute(Attributes.MAX_HEALTH);
        if (maxHealth == null) {
            return;
        }

        // Remove the transient modifier used before base health became a true base-value adjustment.
        maxHealth.removeModifier(MODIFIER_ID);

        PlayerSurvivalData data = player.getData(ModAttachments.PLAYER_SURVIVAL);
        ServerConfig config = ServerConfig.INSTANCE;
        if (!config.isEnabled(SurvivalModule.BASE_HEALTH)) {
            restoreOriginalBase(maxHealth, data);
            clampCurrentHealth(player);
            return;
        }

        double currentBase = maxHealth.getBaseValue();
        double originalBase = originalBaseValue(
                currentBase,
                data.hasBaseHealthAdjustment(),
                data.originalBaseHealth(),
                data.appliedBaseHealth());
        double configuredBase = maxHealth.getAttribute().value().sanitizeValue(configuredBaseValue(
                originalBase,
                config.baseHealthMode.get(),
                config.baseHealthAmount.getAsDouble()));

        maxHealth.setBaseValue(configuredBase);
        data.setBaseHealthAdjustment(originalBase, configuredBase);
        clampCurrentHealth(player);
    }

    public static void applyConfiguredBaseToOnlinePlayers(MinecraftServer server) {
        server.getPlayerList().getPlayers().forEach(BaseHealthService::applyConfiguredBase);
    }

    static double configuredBaseValue(double originalBase, BaseHealthMode mode, double configuredAmount) {
        return switch (mode) {
            case ADD -> originalBase + configuredAmount;
            case MULTIPLY -> originalBase * configuredAmount;
        };
    }

    static double originalBaseValue(
            double currentBase,
            boolean hasPreviousAdjustment,
            double previousOriginalBase,
            double previousAppliedBase) {
        if (hasPreviousAdjustment && sameValue(currentBase, previousAppliedBase)) {
            return previousOriginalBase;
        }
        return currentBase;
    }

    private static void restoreOriginalBase(AttributeInstance maxHealth, PlayerSurvivalData data) {
        if (!data.hasBaseHealthAdjustment()) {
            return;
        }
        if (sameValue(maxHealth.getBaseValue(), data.appliedBaseHealth())) {
            maxHealth.setBaseValue(data.originalBaseHealth());
        }
        data.clearBaseHealthAdjustment();
    }

    private static boolean sameValue(double first, double second) {
        double scale = Math.max(1.0D, Math.max(Math.abs(first), Math.abs(second)));
        return Math.abs(first - second) <= 1.0E-9D * scale;
    }

    private static void clampCurrentHealth(ServerPlayer player) {
        player.setHealth(Math.min(player.getHealth(), player.getMaxHealth()));
    }
}
