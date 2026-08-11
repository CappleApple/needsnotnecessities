package com.cappleapple.needsnotnecessities.api.event;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.Event;

public final class PlayerSurvivalRespawnEvent extends Event {
    private final ServerPlayer player;
    private final boolean deathRespawn;

    public PlayerSurvivalRespawnEvent(ServerPlayer player, boolean deathRespawn) {
        this.player = player;
        this.deathRespawn = deathRespawn;
    }

    public ServerPlayer player() { return player; }
    public boolean deathRespawn() { return deathRespawn; }
}
