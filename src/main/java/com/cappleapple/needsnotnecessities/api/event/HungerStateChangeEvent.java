package com.cappleapple.needsnotnecessities.api.event;

import com.cappleapple.needsnotnecessities.survival.state.SurvivalStateDefinition;
import com.cappleapple.needsnotnecessities.survival.state.SurvivalStateIds;
import net.minecraft.server.level.ServerPlayer;

public final class HungerStateChangeEvent extends SurvivalStateChangeEvent {
    public HungerStateChangeEvent(ServerPlayer player, SurvivalStateDefinition oldState, SurvivalStateDefinition newState, double currentHours) {
        super(player, SurvivalStateIds.HUNGER, oldState, newState, currentHours);
    }
}
