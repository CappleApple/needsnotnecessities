package com.cappleapple.needsnotnecessities.survival.meal;

import com.cappleapple.needsnotnecessities.config.MealEqualPolicy;
import com.cappleapple.needsnotnecessities.config.ServerConfig;
import com.cappleapple.needsnotnecessities.data.ActiveMealData;
import com.cappleapple.needsnotnecessities.data.ModAttachments;
import com.cappleapple.needsnotnecessities.data.PlayerSurvivalData;
import com.cappleapple.needsnotnecessities.modifier.SurvivalModifierService;
import com.cappleapple.needsnotnecessities.survival.BiologicalTimeService;
import com.cappleapple.needsnotnecessities.survival.SurvivalModule;
import com.cappleapple.needsnotnecessities.survival.state.StateTrackService;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import com.cappleapple.needsnotnecessities.api.event.MealActivatedEvent;
import com.cappleapple.needsnotnecessities.api.event.MealExpiredEvent;
import net.neoforged.neoforge.common.NeoForge;

public final class ActiveMealService {
    private static final double SCORE_EPSILON = 1.0E-6D;

    private ActiveMealService() {
    }

    public static void onFoodConsumed(ServerPlayer player, ItemStack stack, double foodHours) {
        if (!ServerConfig.INSTANCE.isEnabled(SurvivalModule.ACTIVE_MEAL) || player.isCreative()) {
            return;
        }
        MealAnalysis analysis = MealRecipeAnalyzer.analyze(player, stack, foodHours);
        if (analysis.durationBiologicalHours() <= 0.0D) {
            return;
        }
        ActiveMealData candidate = new ActiveMealData(
                BuiltInRegistries.ITEM.getKey(stack.getItem()),
                stack.getHoverName().getString(),
                analysis.score(),
                analysis.durationBiologicalHours(),
                analysis.modifiers(),
                analysis.recipeComplexity(),
                analysis.traits(),
                analysis.qualityValue());
        PlayerSurvivalData data = player.getData(ModAttachments.PLAYER_SURVIVAL);
        ActiveMealData active = data.activeMeal().orElse(null);
        if (active == null || candidate.score() > active.score() + SCORE_EPSILON) {
            setActive(player, data, candidate);
            return;
        }
        if (Math.abs(candidate.score() - active.score()) > SCORE_EPSILON) {
            return;
        }

        MealEqualPolicy policy = ServerConfig.INSTANCE.mealEqualPolicy.get();
        if (policy == MealEqualPolicy.REPLACE) {
            setActive(player, data, candidate);
        } else if (policy == MealEqualPolicy.REFRESH) {
            ActiveMealData refreshed = new ActiveMealData(
                    active.sourceItem(),
                    active.displayName(),
                    active.score(),
                    Math.max(active.remainingBiologicalHours(), candidate.remainingBiologicalHours()),
                    active.modifiers(),
                    active.recipeComplexity(),
                    active.traits(),
                    active.qualityValue());
            setActive(player, data, refreshed);
        }
    }

    public static void tick(ServerPlayer player, PlayerSurvivalData data) {
        if (!ServerConfig.INSTANCE.isEnabled(SurvivalModule.ACTIVE_MEAL)) {
            return;
        }
        data.activeMeal().ifPresent(active -> {
            double elapsed = BiologicalTimeService.INSTANCE.ticksToBiologicalHours(1.0D);
            double remaining = Math.max(0.0D, active.remainingBiologicalHours() - elapsed);
            if (remaining <= 0.0D) {
                data.clearActiveMeal();
                if (ServerConfig.INSTANCE.isEnabled(SurvivalModule.COMPATIBILITY)) {
                    NeoForge.EVENT_BUS.post(new MealExpiredEvent(player, active));
                }
                SurvivalModifierService.forceRecompute(player, StateTrackService.gatherAllModifiers(player, data));
            } else {
                data.setActiveMeal(new ActiveMealData(
                        active.sourceItem(),
                        active.displayName(),
                        active.score(),
                        remaining,
                        active.modifiers(),
                        active.recipeComplexity(),
                        active.traits(),
                        active.qualityValue()));
            }
        });
    }

    public static boolean activateExternal(ServerPlayer player, PlayerSurvivalData data, ActiveMealData meal) {
        return setActive(player, data, meal);
    }

    private static boolean setActive(ServerPlayer player, PlayerSurvivalData data, ActiveMealData meal) {
        ActiveMealData previous = data.activeMeal().orElse(null);
        if (ServerConfig.INSTANCE.isEnabled(SurvivalModule.COMPATIBILITY)) {
            MealActivatedEvent event = new MealActivatedEvent(player, previous, meal);
            NeoForge.EVENT_BUS.post(event);
            if (event.isCanceled()) {
                return false;
            }
        }
        data.setActiveMeal(meal);
        SurvivalModifierService.forceRecompute(player, StateTrackService.gatherAllModifiers(player, data));
        return true;
    }
}
