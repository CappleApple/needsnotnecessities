package com.cappleapple.needsnotnecessities.api.event;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.Event;

public final class SurvivalStateUpdateEvent extends Event {
    private final ServerPlayer player;
    private final long biologicalAgeTicks;

    public SurvivalStateUpdateEvent(ServerPlayer player, long biologicalAgeTicks) {
        this.player = player;
        this.biologicalAgeTicks = biologicalAgeTicks;
    }

    public ServerPlayer player() { return player; }
    public long biologicalAgeTicks() { return biologicalAgeTicks; }
}
