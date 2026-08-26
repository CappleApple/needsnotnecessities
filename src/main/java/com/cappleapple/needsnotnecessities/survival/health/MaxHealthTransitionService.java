package com.cappleapple.needsnotnecessities.survival.health;

import com.cappleapple.needsnotnecessities.network.SilentHealthAdjustmentPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

public final class MaxHealthTransitionService {
    private MaxHealthTransitionService() {
    }

    public static Snapshot capture(ServerPlayer player) {
        return new Snapshot(player.getHealth(), player.getMaxAbsorption(), player.getAbsorptionAmount());
    }

    public static void finish(ServerPlayer player, Snapshot snapshot) {
        AbsorptionScalingService.rescaleAfterMaxHealthChange(
                player,
                snapshot.maximumAbsorption(),
                snapshot.absorption());

        float healthBeforeClamp = player.getHealth();
        float clampedHealth = Math.min(healthBeforeClamp, player.getMaxHealth());
        if (clampedHealth >= healthBeforeClamp) {
            return;
        }

        player.setHealth(clampedHealth);
        sendSilentReduction(player, healthBeforeClamp, clampedHealth);
    }

    public static void finishVanillaAttributeUpdate(ServerPlayer player, Snapshot snapshot) {
        // The attribute callback runs after the max-health value has already changed, so the prior
        // absorption maximum is unavailable here. Enforce the newly scaled cap without refilling it.
        player.setAbsorptionAmount(Math.min(player.getAbsorptionAmount(), player.getMaxAbsorption()));
        sendSilentReduction(player, snapshot.health(), player.getHealth());
    }

    private static void sendSilentReduction(ServerPlayer player, float previousHealth, float currentHealth) {
        if (currentHealth >= previousHealth || player.connection == null) {
            return;
        }
        PacketDistributor.sendToPlayer(
                player,
                new SilentHealthAdjustmentPayload(previousHealth - currentHealth, currentHealth));
    }

    public record Snapshot(float health, float maximumAbsorption, float absorption) {
    }
}
