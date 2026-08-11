package com.cappleapple.needsnotnecessities.api.event;

import com.cappleapple.needsnotnecessities.survival.thirst.ThirstService;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.Event;

public final class DrinkConsumedEvent extends Event {
    private final ServerPlayer player;
    private final ItemStack stack;
    private final ThirstService.DrinkKind kind;
    private final double levelAdjustment;

    public DrinkConsumedEvent(ServerPlayer player, ItemStack stack, ThirstService.DrinkKind kind, double levelAdjustment) {
        this.player = player;
        this.stack = stack.copy();
        this.kind = kind;
        this.levelAdjustment = levelAdjustment;
    }

    public ServerPlayer player() { return player; }
    public ItemStack stack() { return stack.copy(); }
    public ThirstService.DrinkKind kind() { return kind; }
    public double levelAdjustment() { return levelAdjustment; }
}
