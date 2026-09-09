package com.cappleapple.needsnotnecessities.survival.rest;

import com.cappleapple.needsnotnecessities.config.ServerConfig;
import com.cappleapple.needsnotnecessities.data.ModAttachments;
import com.cappleapple.needsnotnecessities.data.PlayerSurvivalData;
import com.cappleapple.needsnotnecessities.survival.SurvivalModule;
import com.cappleapple.needsnotnecessities.survival.state.StateDefinitionManager;
import com.cappleapple.needsnotnecessities.survival.state.StateTimeline;
import com.cappleapple.needsnotnecessities.survival.state.StateTrackService;
import com.cappleapple.needsnotnecessities.survival.state.SurvivalStateIds;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.entity.player.PlayerSpawnPhantomsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerSpawnPhantomsEvent.Result;

public final class PhantomSpawnService {
    private PhantomSpawnService() {
    }

    public static void onPlayerSpawnPhantoms(PlayerSpawnPhantomsEvent event) {
        if (!ServerConfig.INSTANCE.isEnabled(SurvivalModule.REST)
                || !(event.getEntity() instanceof ServerPlayer player)
                || event.getResult() == Result.DENY) {
            return;
        }
        PlayerSurvivalData data = player.getData(ModAttachments.PLAYER_SURVIVAL);
        StateTrackService.initializeMissingTracks(data);
        StateTimeline timeline = StateDefinitionManager.INSTANCE.require(SurvivalStateIds.REST);
        if (player.isCreative() || player.isSpectator() || !player.isAlive()
                || !isLowestRestState(timeline, data.statePosition(SurvivalStateIds.REST))) {
            event.setResult(Result.DENY);
            return;
        }
        if (event.getResult() == Result.DEFAULT) {
            ServerLevel level = player.serverLevel();
            BlockPos position = player.blockPosition();
            // ALLOW also bypasses vanilla's sky, altitude, and local-difficulty checks.
            // Evaluate those first so only the insomnia requirement is replaced by Rest.
            boolean canSpawn = event.shouldSpawnPhantoms(level, position)
                    && level.getCurrentDifficultyAt(position).isHarderThan(level.random.nextFloat() * 3.0F);
            event.setResult(canSpawn ? Result.ALLOW : Result.DENY);
        }
    }

    public static boolean isLowestRestState(StateTimeline timeline, double positionHours) {
        return timeline.stateAt(positionHours).id().equals(timeline.states().getFirst().id());
    }
}
