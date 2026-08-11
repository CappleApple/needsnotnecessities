package com.cappleapple.needsnotnecessities.api.event;

import com.cappleapple.needsnotnecessities.survival.state.SurvivalStateDefinition;
import com.cappleapple.needsnotnecessities.survival.state.SurvivalStateIds;
import net.minecraft.server.level.ServerPlayer;

public final class RestStateChangeEvent extends SurvivalStateChangeEvent {
    public RestStateChangeEvent(ServerPlayer player, SurvivalStateDefinition oldState, SurvivalStateDefinition newState, double currentHours) {
        super(player, SurvivalStateIds.REST, oldState, newState, currentHours);
    }
}
