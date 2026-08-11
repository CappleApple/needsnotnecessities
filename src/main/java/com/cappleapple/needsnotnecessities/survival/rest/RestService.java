package com.cappleapple.needsnotnecessities.survival.rest;

import com.cappleapple.needsnotnecessities.config.ServerConfig;
import com.cappleapple.needsnotnecessities.data.PlayerSurvivalData;
import com.cappleapple.needsnotnecessities.survival.BiologicalTimeService;
import com.cappleapple.needsnotnecessities.survival.SurvivalModule;
import com.cappleapple.needsnotnecessities.survival.state.StateDefinitionManager;
import com.cappleapple.needsnotnecessities.survival.state.StateTimeline;
import com.cappleapple.needsnotnecessities.survival.state.SurvivalStateIds;
import com.cappleapple.needsnotnecessities.survival.state.StateMutationService;
import net.minecraft.server.level.ServerPlayer;

public final class RestService {
    private RestService() {
    }

    public static void tick(ServerPlayer player, PlayerSurvivalData data) {
        if (!ServerConfig.INSTANCE.isEnabled(SurvivalModule.REST)
                || player.isCreative()
                || player.isSpectator()
                || !player.isAlive()) {
            return;
        }
        StateTimeline timeline = StateDefinitionManager.INSTANCE.require(SurvivalStateIds.REST);
        double elapsedBiologicalHours = BiologicalTimeService.INSTANCE.ticksToBiologicalHours(1.0D);
        double delta = player.isSleeping()
                ? sleepingRecoveryDelta(
                        timeline.totalHours(),
                        elapsedBiologicalHours,
                        ServerConfig.INSTANCE.restFullRecoveryHours.getAsDouble())
                : -elapsedBiologicalHours;
        StateMutationService.setPosition(
                player,
                data,
                SurvivalStateIds.REST,
                timeline.add(data.statePosition(SurvivalStateIds.REST), delta));
    }

    public static double sleepingRecoveryDelta(
            double totalTimelineHours,
            double elapsedBiologicalHours,
            double fullRecoveryBiologicalHours) {
        if (!Double.isFinite(totalTimelineHours)
                || !Double.isFinite(elapsedBiologicalHours)
                || !Double.isFinite(fullRecoveryBiologicalHours)
                || totalTimelineHours < 0.0D
                || elapsedBiologicalHours < 0.0D
                || fullRecoveryBiologicalHours <= 0.0D) {
            throw new IllegalArgumentException("Rest recovery inputs must be finite and non-negative; full recovery time must be positive");
        }
        return totalTimelineHours * elapsedBiologicalHours / fullRecoveryBiologicalHours;
    }
}
