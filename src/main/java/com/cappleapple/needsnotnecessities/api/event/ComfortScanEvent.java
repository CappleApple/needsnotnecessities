package com.cappleapple.needsnotnecessities.api.event;

import com.cappleapple.needsnotnecessities.survival.comfort.ComfortService;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.Event;

public final class ComfortScanEvent extends Event {
    private final ServerPlayer player;
    private final List<ComfortService.ComfortContributor> contributors;

    public ComfortScanEvent(ServerPlayer player, List<ComfortService.ComfortContributor> contributors) {
        this.player = player;
        this.contributors = new ArrayList<>(contributors);
    }

    public ServerPlayer player() { return player; }
    public List<ComfortService.ComfortContributor> contributors() { return contributors; }
}
