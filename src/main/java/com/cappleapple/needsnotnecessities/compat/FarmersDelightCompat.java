package com.cappleapple.needsnotnecessities.compat;

import com.cappleapple.needsnotnecessities.config.ServerConfig;
import com.cappleapple.needsnotnecessities.survival.SurvivalModule;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public final class FarmersDelightCompat {
    private static final ResourceLocation NOURISHMENT =
            ResourceLocation.fromNamespaceAndPath("farmersdelight", "nourishment");

    private FarmersDelightCompat() {
    }

    public static boolean pausesHunger(ServerPlayer player) {
        ServerConfig config = ServerConfig.INSTANCE;
        if (!config.isEnabled(SurvivalModule.COMPATIBILITY)
                || !config.farmersDelightNourishmentPausesHunger.getAsBoolean()) {
            return false;
        }
        return player.getActiveEffects().stream().anyMatch(instance ->
                instance.getEffect().unwrapKey()
                        .map(key -> key.location().equals(NOURISHMENT))
                        .orElse(false));
    }
}
