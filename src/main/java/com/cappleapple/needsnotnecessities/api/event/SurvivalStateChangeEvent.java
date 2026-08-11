package com.cappleapple.needsnotnecessities.api.event;

import com.cappleapple.needsnotnecessities.survival.state.SurvivalStateDefinition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;

public abstract class SurvivalStateChangeEvent extends Event implements ICancellableEvent {
    private final ServerPlayer player;
    private final ResourceLocation system;
    private final SurvivalStateDefinition oldState;
    private final SurvivalStateDefinition newState;
    private final double currentHours;

    protected SurvivalStateChangeEvent(
            ServerPlayer player,
            ResourceLocation system,
            SurvivalStateDefinition oldState,
            SurvivalStateDefinition newState,
            double currentHours) {
        this.player = player;
        this.system = system;
        this.oldState = oldState;
        this.newState = newState;
        this.currentHours = currentHours;
    }

    public ServerPlayer player() { return player; }
    public ResourceLocation system() { return system; }
    public SurvivalStateDefinition oldState() { return oldState; }
    public SurvivalStateDefinition newState() { return newState; }
    public double currentHours() { return currentHours; }
}
