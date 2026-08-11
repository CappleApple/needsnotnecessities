package com.cappleapple.needsnotnecessities.api.event;

import com.cappleapple.needsnotnecessities.data.ActiveMealData;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.Event;

public final class MealExpiredEvent extends Event {
    private final ServerPlayer player;
    private final ActiveMealData expiredMeal;

    public MealExpiredEvent(ServerPlayer player, ActiveMealData expiredMeal) {
        this.player = player;
        this.expiredMeal = expiredMeal;
    }

    public ServerPlayer player() { return player; }
    public ActiveMealData expiredMeal() { return expiredMeal; }
}
