package com.cappleapple.needsnotnecessities.survival.rest;

import com.cappleapple.needsnotnecessities.config.ServerConfig;
import com.cappleapple.needsnotnecessities.data.ModAttachments;
import com.cappleapple.needsnotnecessities.data.PlayerSurvivalData;
import com.cappleapple.needsnotnecessities.modifier.SurvivalModifierService;
import com.cappleapple.needsnotnecessities.network.SurvivalSnapshotService;
import com.cappleapple.needsnotnecessities.survival.SurvivalModule;
import com.cappleapple.needsnotnecessities.survival.state.StagePercentageEligibility;
import com.cappleapple.needsnotnecessities.survival.state.StateDefinitionManager;
import com.cappleapple.needsnotnecessities.survival.state.StateTimeline;
import com.cappleapple.needsnotnecessities.survival.state.StateTrackService;
import com.cappleapple.needsnotnecessities.survival.state.SurvivalStateIds;
import com.cappleapple.needsnotnecessities.survival.state.StateMutationService;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player.BedSleepingProblem;
import net.neoforged.neoforge.event.entity.player.CanContinueSleepingEvent;
import net.neoforged.neoforge.event.entity.player.CanPlayerSleepEvent;
import net.neoforged.neoforge.event.level.SleepFinishedTimeEvent;

public final class SleepRulesService {
    private static final long TICKS_PER_DAY = 24_000L;
    private static final long NIGHT_START = 13_000L;

    private SleepRulesService() {
    }

    public static void onCanPlayerSleep(CanPlayerSleepEvent event) {
        ServerConfig config = ServerConfig.INSTANCE;
        if (!config.isEnabled(SurvivalModule.REST)) {
            return;
        }
        if (event.getProblem() == BedSleepingProblem.NOT_POSSIBLE_NOW
                && config.allowDaytimeSleep.getAsBoolean()) {
            event.setProblem(null);
        }
        if (event.getProblem() == null
                && config.requireTiredToSleep.getAsBoolean()
                && !isTiredEnough(event.getEntity())) {
            event.getEntity().displayClientMessage(
                    Component.translatable("needs_not_necessities.sleep.not_tired"), true);
            event.setProblem(BedSleepingProblem.OTHER_PROBLEM);
        }
    }

    public static void onCanContinueSleeping(CanContinueSleepingEvent event) {
        ServerConfig config = ServerConfig.INSTANCE;
        if (config.isEnabled(SurvivalModule.REST)
                && config.allowDaytimeSleep.getAsBoolean()
                && event.getProblem() == BedSleepingProblem.NOT_POSSIBLE_NOW) {
            event.setContinueSleeping(true);
        }
    }

    public static void onSleepFinished(SleepFinishedTimeEvent event) {
        ServerConfig config = ServerConfig.INSTANCE;
        if (!config.isEnabled(SurvivalModule.REST) || !(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        long target = targetWakeTime(
                level.getDayTime(),
                level.isDay(),
                config.daytimeSleepSkipsToNight.getAsBoolean());
        level.players().stream()
                .filter(ServerPlayer::isSleeping)
                .forEach(SleepRulesService::completeSleep);
        event.setTimeAddition(target);
    }

    public static boolean isTiredEnough(ServerPlayer player) {
        PlayerSurvivalData data = player.getData(ModAttachments.PLAYER_SURVIVAL);
        StateTrackService.initializeMissingTracks(data);
        StateTimeline timeline = StateDefinitionManager.INSTANCE.require(SurvivalStateIds.REST);
        return StagePercentageEligibility.isBelowConfiguredPercentage(
                timeline,
                data.statePosition(SurvivalStateIds.REST),
                ServerConfig.INSTANCE.sleepBelowStagePercentage.getAsDouble());
    }

    public static long targetWakeTime(long currentDayTime, boolean daytime, boolean daytimeSkipsToNight) {
        long dayStart = Math.floorDiv(currentDayTime, TICKS_PER_DAY) * TICKS_PER_DAY;
        if (daytime && daytimeSkipsToNight) {
            long target = dayStart + NIGHT_START;
            return target > currentDayTime ? target : target + TICKS_PER_DAY;
        }
        return dayStart + TICKS_PER_DAY;
    }

    private static void completeSleep(ServerPlayer player) {
        PlayerSurvivalData data = player.getData(ModAttachments.PLAYER_SURVIVAL);
        StateTrackService.initializeMissingTracks(data);
        StateTimeline timeline = StateDefinitionManager.INSTANCE.require(SurvivalStateIds.REST);
        StateMutationService.setPosition(
                player,
                data,
                SurvivalStateIds.REST,
                timeline.bestPosition());
        SurvivalModifierService.forceRecompute(player, StateTrackService.gatherAllModifiers(player, data));
        SurvivalSnapshotService.sync(player, data);
    }
}
