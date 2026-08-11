package com.cappleapple.needsnotnecessities.survival.health;

import com.cappleapple.needsnotnecessities.config.ServerConfig;
import com.cappleapple.needsnotnecessities.data.ModAttachments;
import com.cappleapple.needsnotnecessities.data.PlayerSurvivalData;
import com.cappleapple.needsnotnecessities.modifier.SurvivalModifierService;
import com.cappleapple.needsnotnecessities.survival.SurvivalModule;
import net.minecraft.server.level.ServerPlayer;

public final class PassiveRegenerationService {
    private PassiveRegenerationService() {
    }

    public static void tick(ServerPlayer player, PlayerSurvivalData data) {
        ServerConfig config = ServerConfig.INSTANCE;
        if (!config.isEnabled(SurvivalModule.PASSIVE_REGENERATION)
                || !player.isAlive()
                || player.getHealth() >= player.getMaxHealth()
                || player.tickCount % config.regenerationIntervalTicks.getAsInt() != 0) {
            return;
        }

        long gameTick = player.level().getGameTime();
        if (!config.regenerateDuringCombat.getAsBoolean()
                && isCombatCooldownActive(data.lastCombatGameTick(), gameTick, config.regenerationCombatCooldownTicks.getAsInt())) {
            return;
        }

        double multiplier = SurvivalModifierService.scalarValue(data, SurvivalModifierService.PASSIVE_REGENERATION);
        float amount = (float) (config.regenerationAmount.getAsDouble() * multiplier);
        if (Float.isFinite(amount) && amount > 0.0F) {
            player.heal(amount);
        }
    }

    public static void markCombat(ServerPlayer player) {
        if (ServerConfig.INSTANCE.isEnabled(SurvivalModule.PASSIVE_REGENERATION)) {
            player.getData(ModAttachments.PLAYER_SURVIVAL).markCombat(player.level().getGameTime());
        }
    }

    public static boolean isCombatCooldownActive(long lastCombatTick, long currentGameTick, long cooldownTicks) {
        if (lastCombatTick < 0L || cooldownTicks <= 0L) {
            return false;
        }
        long elapsed = currentGameTick - lastCombatTick;
        return elapsed < 0L || elapsed < cooldownTicks;
    }
}
