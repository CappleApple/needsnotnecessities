package com.cappleapple.needsnotnecessities.survival.health;

import com.cappleapple.needsnotnecessities.config.ServerConfig;
import com.cappleapple.needsnotnecessities.data.PlayerSurvivalData;
import net.minecraft.server.level.ServerPlayer;

public final class RespawnHealthService {
    private RespawnHealthService() {
    }

    public static void applyPending(ServerPlayer player, PlayerSurvivalData data) {
        if (!data.pendingDeathHealthReset() || !player.isAlive()) {
            return;
        }
        data.setPendingDeathHealthReset(false);
        if (!ServerConfig.INSTANCE.deathPenaltiesEnabled.getAsBoolean()) {
            return;
        }

        // All respawn handlers have finished by this point. Run after our regeneration tick and before
        // ServerPlayer sends its health packet, using the replacement player's finalized maximum.
        player.setHealth(healthAfterRespawn(player.getMaxHealth(), ServerConfig.INSTANCE.respawnHealthPercentage.getAsDouble()));
    }

    static float healthAfterRespawn(float maximumHealth, double percentage) {
        return Math.clamp((float) (maximumHealth * percentage), Math.min(1.0F, maximumHealth), maximumHealth);
    }
}
