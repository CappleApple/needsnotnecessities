package com.cappleapple.needsnotnecessities.survival.food;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

/** Captures actual nutrition calls inside block interactions, even when vanilla hunger is full. */
public final class PlacedFoodConsumption {
    private static final ThreadLocal<Interaction> CURRENT = new ThreadLocal<>();

    private PlacedFoodConsumption() {
    }

    public static <T> T interact(Player player, BlockState state, Supplier<T> action) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return action.get();
        }
        Interaction previous = CURRENT.get();
        Interaction interaction = new Interaction(serverPlayer, state);
        CURRENT.set(interaction);
        T result;
        try {
            result = action.get();
        } finally {
            if (previous == null) {
                CURRENT.remove();
            } else {
                CURRENT.set(previous);
            }
        }
        for (Bite bite : interaction.bites) {
            FoodConsumptionService.consume(serverPlayer, bite.stack(), bite.nutrition(), bite.saturation());
        }
        return result;
    }

    public static void record(FoodData foodData, int nutrition, float saturation) {
        Interaction interaction = CURRENT.get();
        if (interaction != null && interaction.player.getFoodData() == foodData) {
            interaction.bites.add(new Bite(PlacedFoodResolver.serving(interaction.state), nutrition, saturation));
        }
    }

    /** If a block also fires the held-food event, retain its actual stack without counting twice. */
    public static boolean captureFinished(ServerPlayer player, ItemStack stack) {
        Interaction interaction = CURRENT.get();
        if (interaction == null || interaction.player != player || interaction.bites.isEmpty()) {
            return false;
        }
        int last = interaction.bites.size() - 1;
        Bite bite = interaction.bites.get(last);
        interaction.bites.set(last, new Bite(stack.copy(), bite.nutrition(), bite.saturation()));
        return true;
    }

    private static final class Interaction {
        private final ServerPlayer player;
        private final BlockState state;
        private final List<Bite> bites = new ArrayList<>();

        private Interaction(ServerPlayer player, BlockState state) {
            this.player = player;
            this.state = state;
        }
    }

    private record Bite(ItemStack stack, int nutrition, float saturation) {
    }
}
