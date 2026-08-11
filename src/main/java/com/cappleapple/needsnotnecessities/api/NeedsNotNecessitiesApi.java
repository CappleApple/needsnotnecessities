package com.cappleapple.needsnotnecessities.api;

import com.cappleapple.needsnotnecessities.api.event.ComfortChangeEvent;
import com.cappleapple.needsnotnecessities.config.ServerConfig;
import com.cappleapple.needsnotnecessities.data.ActiveMealData;
import com.cappleapple.needsnotnecessities.data.ModAttachments;
import com.cappleapple.needsnotnecessities.data.PlayerSurvivalData;
import com.cappleapple.needsnotnecessities.modifier.SurvivalModifierService;
import com.cappleapple.needsnotnecessities.network.SurvivalSnapshotService;
import com.cappleapple.needsnotnecessities.survival.SurvivalModule;
import com.cappleapple.needsnotnecessities.survival.meal.ActiveMealService;
import com.cappleapple.needsnotnecessities.survival.state.StateDefinitionManager;
import com.cappleapple.needsnotnecessities.survival.state.StateMutationService;
import com.cappleapple.needsnotnecessities.survival.state.StateTimeline;
import com.cappleapple.needsnotnecessities.survival.state.StateTrackService;
import com.cappleapple.needsnotnecessities.survival.state.SurvivalStateDefinition;
import com.cappleapple.needsnotnecessities.survival.state.SurvivalStateIds;
import java.time.Duration;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.common.NeoForge;

/** Public, server-side integration surface. Returned records and optionals are immutable snapshots. */
public final class NeedsNotNecessitiesApi {
    private NeedsNotNecessitiesApi() {
    }

    public static SurvivalStateDefinition getHungerState(ServerPlayer player) {
        return state(player, SurvivalStateIds.HUNGER);
    }

    public static double getHungerHours(ServerPlayer player) {
        return hours(player, SurvivalStateIds.HUNGER);
    }

    public static SurvivalStateDefinition getThirstState(ServerPlayer player) {
        return state(player, SurvivalStateIds.THIRST);
    }

    public static double getThirstHours(ServerPlayer player) {
        return hours(player, SurvivalStateIds.THIRST);
    }

    public static SurvivalStateDefinition getRestState(ServerPlayer player) {
        return state(player, SurvivalStateIds.REST);
    }

    public static double getRestHours(ServerPlayer player) {
        return hours(player, SurvivalStateIds.REST);
    }

    public static double getComfort(ServerPlayer player) {
        return data(player).retainedComfort();
    }

    public static Optional<ActiveMealData> getActiveMeal(ServerPlayer player) {
        return data(player).activeMeal();
    }

    public static boolean addHungerHours(ServerPlayer player, double amount) {
        return adjustState(player, SurvivalModule.HUNGER, SurvivalStateIds.HUNGER, amount);
    }

    public static boolean setHungerState(ServerPlayer player, String state) {
        return setState(player, SurvivalModule.HUNGER, SurvivalStateIds.HUNGER, state);
    }

    public static boolean adjustThirst(ServerPlayer player, double amount) {
        return adjustState(player, SurvivalModule.THIRST, SurvivalStateIds.THIRST, amount);
    }

    public static boolean setThirstState(ServerPlayer player, String state) {
        return setState(player, SurvivalModule.THIRST, SurvivalStateIds.THIRST, state);
    }

    public static boolean advanceRest(ServerPlayer player, double amount) {
        return adjustState(player, SurvivalModule.REST, SurvivalStateIds.REST, amount);
    }

    public static boolean setRestState(ServerPlayer player, String state) {
        return setState(player, SurvivalModule.REST, SurvivalStateIds.REST, state);
    }

    public static boolean setStateHours(ServerPlayer player, ResourceLocation system, double hours) {
        SurvivalModule module = SurvivalStateIds.moduleFor(system).orElse(null);
        if (module == null || !ServerConfig.INSTANCE.isEnabled(module) || !Double.isFinite(hours)) {
            return false;
        }
        PlayerSurvivalData data = data(player);
        boolean changed = StateMutationService.setPosition(player, data, system, hours);
        if (changed) {
            refresh(player, data);
        }
        return changed;
    }

    public static boolean setActiveMeal(ServerPlayer player, ActiveMealData meal) {
        if (!ServerConfig.INSTANCE.isEnabled(SurvivalModule.ACTIVE_MEAL) || meal == null) {
            return false;
        }
        return ActiveMealService.activateExternal(player, data(player), meal);
    }

    public static void clearActiveMeal(ServerPlayer player) {
        PlayerSurvivalData data = data(player);
        data.clearActiveMeal();
        refresh(player, data);
    }

    public static boolean addTemporaryComfort(ServerPlayer player, double amount, Duration duration) {
        if (!ServerConfig.INSTANCE.isEnabled(SurvivalModule.COMFORT)
                || !Double.isFinite(amount)
                || amount <= 0.0D
                || duration == null
                || duration.isNegative()) {
            return false;
        }
        PlayerSurvivalData data = data(player);
        double oldComfort = data.retainedComfort();
        long ticks = Math.max(0L, Math.round(duration.toMillis() / 50.0D));
        data.setRetainedComfort(Math.max(oldComfort, amount), Math.max(data.comfortRetentionTicks(), ticks));
        if (Double.compare(oldComfort, data.retainedComfort()) != 0
                && ServerConfig.INSTANCE.isEnabled(SurvivalModule.COMPATIBILITY)) {
            NeoForge.EVENT_BUS.post(new ComfortChangeEvent(player, oldComfort, data.retainedComfort()));
        }
        refresh(player, data);
        return true;
    }

    public static void resetToNeutral(ServerPlayer player) {
        PlayerSurvivalData data = data(player);
        for (ResourceLocation system : new ResourceLocation[]{
                SurvivalStateIds.HUNGER, SurvivalStateIds.THIRST, SurvivalStateIds.REST}) {
            SurvivalStateIds.moduleFor(system).filter(ServerConfig.INSTANCE::isEnabled).ifPresent(module -> {
                StateTimeline timeline = StateDefinitionManager.INSTANCE.require(system);
                StateMutationService.setPosition(player, data, system, timeline.neutralPosition());
            });
        }
        data.clearComfort();
        data.clearActiveMeal();
        refresh(player, data);
    }

    private static boolean adjustState(
            ServerPlayer player,
            SurvivalModule module,
            ResourceLocation system,
            double amount) {
        if (!ServerConfig.INSTANCE.isEnabled(module) || !Double.isFinite(amount)) {
            return false;
        }
        PlayerSurvivalData data = data(player);
        StateTimeline timeline = StateDefinitionManager.INSTANCE.require(system);
        boolean changed = StateMutationService.setPosition(
                player, data, system, timeline.add(data.statePosition(system), amount));
        if (changed) {
            refresh(player, data);
        }
        return changed;
    }

    private static boolean setState(
            ServerPlayer player,
            SurvivalModule module,
            ResourceLocation system,
            String requestedState) {
        if (!ServerConfig.INSTANCE.isEnabled(module) || requestedState == null) {
            return false;
        }
        StateTimeline timeline = StateDefinitionManager.INSTANCE.require(system);
        Optional<SurvivalStateDefinition> selected = timeline.states().stream()
                .filter(state -> matchesState(state, requestedState))
                .findFirst();
        if (selected.isEmpty()) {
            return false;
        }
        PlayerSurvivalData data = data(player);
        boolean changed = StateMutationService.setPosition(
                player,
                data,
                system,
                timeline.positionForState(selected.get().id(), StateTimeline.PositionAnchor.MIDDLE));
        if (changed) {
            refresh(player, data);
        }
        return changed;
    }

    private static boolean matchesState(SurvivalStateDefinition state, String requested) {
        String normalized = requested.trim();
        return state.id().toString().equalsIgnoreCase(normalized)
                || state.id().getPath().endsWith("/" + normalized.toLowerCase(java.util.Locale.ROOT))
                || state.displayName().equalsIgnoreCase(normalized);
    }

    private static SurvivalStateDefinition state(ServerPlayer player, ResourceLocation system) {
        PlayerSurvivalData data = data(player);
        return StateDefinitionManager.INSTANCE.require(system).stateAt(data.statePosition(system));
    }

    private static double hours(ServerPlayer player, ResourceLocation system) {
        return data(player).statePosition(system);
    }

    private static PlayerSurvivalData data(ServerPlayer player) {
        PlayerSurvivalData data = player.getData(ModAttachments.PLAYER_SURVIVAL);
        StateTrackService.initializeMissingTracks(data);
        return data;
    }

    private static void refresh(ServerPlayer player, PlayerSurvivalData data) {
        SurvivalModifierService.forceRecompute(player, StateTrackService.gatherAllModifiers(player, data));
        SurvivalSnapshotService.sync(player, data);
    }
}
