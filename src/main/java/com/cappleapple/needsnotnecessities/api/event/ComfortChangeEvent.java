package com.cappleapple.needsnotnecessities.api.event;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.Event;

public final class ComfortChangeEvent extends Event {
    private final ServerPlayer player;
    private final double oldComfort;
    private final double newComfort;

    public ComfortChangeEvent(ServerPlayer player, double oldComfort, double newComfort) {
        this.player = player;
        this.oldComfort = oldComfort;
        this.newComfort = newComfort;
    }

    public ServerPlayer player() { return player; }
    public double oldComfort() { return oldComfort; }
    public double newComfort() { return newComfort; }
}
