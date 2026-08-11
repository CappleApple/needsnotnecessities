package com.cappleapple.needsnotnecessities.api.event;

import com.cappleapple.needsnotnecessities.modifier.SurvivalModifier;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.Event;

public final class SurvivalModifierGatherEvent extends Event {
    private final ServerPlayer player;
    private final List<SurvivalModifier> modifiers;

    public SurvivalModifierGatherEvent(ServerPlayer player, List<SurvivalModifier> modifiers) {
        this.player = player;
        this.modifiers = new ArrayList<>(modifiers);
    }

    public ServerPlayer player() { return player; }
    public List<SurvivalModifier> modifiers() { return modifiers; }
}
