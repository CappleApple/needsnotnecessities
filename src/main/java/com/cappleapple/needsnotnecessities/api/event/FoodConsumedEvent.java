package com.cappleapple.needsnotnecessities.api.event;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.Event;

public final class FoodConsumedEvent extends Event {
    private final ServerPlayer player;
    private final ItemStack stack;
    private final int nutrition;
    private final float saturation;
    private final double hungerHours;

    public FoodConsumedEvent(ServerPlayer player, ItemStack stack, int nutrition, float saturation, double hungerHours) {
        this.player = player;
        this.stack = stack.copy();
        this.nutrition = nutrition;
        this.saturation = saturation;
        this.hungerHours = hungerHours;
    }

    public ServerPlayer player() { return player; }
    public ItemStack stack() { return stack.copy(); }
    public int nutrition() { return nutrition; }
    public float saturation() { return saturation; }
    public double hungerHours() { return hungerHours; }
}
