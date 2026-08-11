package com.cappleapple.needsnotnecessities.api.event;

import com.cappleapple.needsnotnecessities.data.ActiveMealData;
import java.util.Optional;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;

public final class MealActivatedEvent extends Event implements ICancellableEvent {
    private final ServerPlayer player;
    private final ActiveMealData previousMeal;
    private final ActiveMealData newMeal;

    public MealActivatedEvent(ServerPlayer player, ActiveMealData previousMeal, ActiveMealData newMeal) {
        this.player = player;
        this.previousMeal = previousMeal;
        this.newMeal = newMeal;
    }

    public ServerPlayer player() { return player; }
    public Optional<ActiveMealData> previousMeal() { return Optional.ofNullable(previousMeal); }
    public ActiveMealData newMeal() { return newMeal; }
    public double calculatedScore() { return newMeal.score(); }
    public double durationBiologicalHours() { return newMeal.remainingBiologicalHours(); }
}
