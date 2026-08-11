package com.cappleapple.needsnotnecessities.survival.state;

import com.cappleapple.needsnotnecessities.api.event.HungerStateChangeEvent;
import com.cappleapple.needsnotnecessities.api.event.RestStateChangeEvent;
import com.cappleapple.needsnotnecessities.api.event.SurvivalStateChangeEvent;
import com.cappleapple.needsnotnecessities.api.event.ThirstStateChangeEvent;
import com.cappleapple.needsnotnecessities.data.PlayerSurvivalData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.common.NeoForge;
import com.cappleapple.needsnotnecessities.config.ServerConfig;
import com.cappleapple.needsnotnecessities.survival.SurvivalModule;

public final class StateMutationService {
    private StateMutationService() {
    }

    public static boolean setPosition(
            ServerPlayer player,
            PlayerSurvivalData data,
            ResourceLocation system,
            double requestedPosition) {
        StateTimeline timeline = StateDefinitionManager.INSTANCE.require(system);
        double oldPosition = timeline.clamp(data.statePosition(system));
        double newPosition = timeline.clamp(requestedPosition);
        SurvivalStateDefinition oldState = timeline.stateAt(oldPosition);
        SurvivalStateDefinition newState = timeline.stateAt(newPosition);
        if (!oldState.id().equals(newState.id())
                && ServerConfig.INSTANCE.isEnabled(SurvivalModule.COMPATIBILITY)) {
            SurvivalStateChangeEvent event = createEvent(player, system, oldState, newState, newPosition);
            NeoForge.EVENT_BUS.post(event);
            if (event.isCanceled()) {
                return false;
            }
        }
        data.setStatePosition(system, newPosition);
        return true;
    }

    private static SurvivalStateChangeEvent createEvent(
            ServerPlayer player,
            ResourceLocation system,
            SurvivalStateDefinition oldState,
            SurvivalStateDefinition newState,
            double newPosition) {
        if (SurvivalStateIds.HUNGER.equals(system)) {
            return new HungerStateChangeEvent(player, oldState, newState, newPosition);
        }
        if (SurvivalStateIds.THIRST.equals(system)) {
            return new ThirstStateChangeEvent(player, oldState, newState, newPosition);
        }
        if (SurvivalStateIds.REST.equals(system)) {
            return new RestStateChangeEvent(player, oldState, newState, newPosition);
        }
        throw new IllegalArgumentException("No state event type is registered for " + system);
    }
}
