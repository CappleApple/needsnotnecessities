package com.cappleapple.needsnotnecessities.survival.food;

import com.cappleapple.needsnotnecessities.api.event.FoodConsumedEvent;
import com.cappleapple.needsnotnecessities.config.ServerConfig;
import com.cappleapple.needsnotnecessities.survival.SurvivalModule;
import com.cappleapple.needsnotnecessities.survival.hunger.HungerService;
import com.cappleapple.needsnotnecessities.survival.meal.ActiveMealService;
import com.cappleapple.needsnotnecessities.survival.thirst.ThirstService;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.NeoForge;

/** The common pipeline for an actual held-food consumption or placed-food bite. */
public final class FoodConsumptionService {
    private FoodConsumptionService() {
    }

    public static void consume(ServerPlayer player, ItemStack stack, int nutrition, float saturation) {
        ServerConfig config = ServerConfig.INSTANCE;
        if (!config.isEnabled(SurvivalModule.HUNGER)
                && !config.isEnabled(SurvivalModule.THIRST)
                && !config.isEnabled(SurvivalModule.ACTIVE_MEAL)) {
            return;
        }
        double hours = HungerService.calculateFoodHours(nutrition, saturation,
                config.hoursPerHungerPoint.getAsDouble(), config.hoursPerSaturationPoint.getAsDouble());
        HungerService.consume(player, hours);
        ThirstService.onFoodConsumed(player, hours);
        ActiveMealService.onFoodConsumed(player, stack, hours);
        if (config.isEnabled(SurvivalModule.COMPATIBILITY)) {
            NeoForge.EVENT_BUS.post(new FoodConsumedEvent(player, stack, nutrition, saturation, hours));
        }
    }
}
