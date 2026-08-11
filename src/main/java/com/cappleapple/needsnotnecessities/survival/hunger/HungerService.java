package com.cappleapple.needsnotnecessities.survival.hunger;

import com.cappleapple.needsnotnecessities.config.ServerConfig;
import com.cappleapple.needsnotnecessities.compat.FarmersDelightCompat;
import com.cappleapple.needsnotnecessities.data.ModAttachments;
import com.cappleapple.needsnotnecessities.data.PlayerSurvivalData;
import com.cappleapple.needsnotnecessities.modifier.SurvivalModifierService;
import com.cappleapple.needsnotnecessities.survival.BiologicalTimeService;
import com.cappleapple.needsnotnecessities.survival.SurvivalModule;
import com.cappleapple.needsnotnecessities.survival.state.StateDefinitionManager;
import com.cappleapple.needsnotnecessities.survival.state.StagePercentageEligibility;
import com.cappleapple.needsnotnecessities.survival.state.StateTimeline;
import com.cappleapple.needsnotnecessities.survival.state.StateTrackService;
import com.cappleapple.needsnotnecessities.survival.state.SurvivalStateIds;
import com.cappleapple.needsnotnecessities.survival.state.StateMutationService;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.food.FoodProperties;

public final class HungerService {
    private HungerService() {
    }

    public static void tick(ServerPlayer player, PlayerSurvivalData data) {
        if (!ServerConfig.INSTANCE.isEnabled(SurvivalModule.HUNGER)) {
            return;
        }
        neutralizeVanillaHunger(player);
        if (player.isCreative() || player.isSpectator() || !player.isAlive()) {
            return;
        }
        if (FarmersDelightCompat.pausesHunger(player)) {
            return;
        }

        StateTimeline timeline = StateDefinitionManager.INSTANCE.require(SurvivalStateIds.HUNGER);
        double elapsedHours = BiologicalTimeService.INSTANCE.ticksToBiologicalHours(1.0D)
                * decayMultiplier(
                        player.hasEffect(MobEffects.HUNGER),
                        ServerConfig.INSTANCE.hungerEffectDecayMultiplier.getAsDouble());
        StateMutationService.setPosition(
                player,
                data,
                SurvivalStateIds.HUNGER,
                decayPosition(timeline, data.statePosition(SurvivalStateIds.HUNGER), elapsedHours));
    }

    public static void consume(ServerPlayer player, FoodProperties foodProperties) {
        consume(player, calculateFoodHours(foodProperties));
    }

    public static boolean canEat(ServerPlayer player) {
        if (player.isCreative() || player.isSpectator()) {
            return true;
        }
        PlayerSurvivalData data = player.getData(ModAttachments.PLAYER_SURVIVAL);
        StateTrackService.initializeMissingTracks(data);
        StateTimeline timeline = StateDefinitionManager.INSTANCE.require(SurvivalStateIds.HUNGER);
        return StagePercentageEligibility.isBelowConfiguredPercentage(
                timeline,
                data.statePosition(SurvivalStateIds.HUNGER),
                ServerConfig.INSTANCE.hungerEatBelowStagePercentage.getAsDouble());
    }

    public static void consume(ServerPlayer player, double foodHours) {
        if (!ServerConfig.INSTANCE.isEnabled(SurvivalModule.HUNGER)
                || player.isCreative()
                || player.isSpectator()) {
            return;
        }
        PlayerSurvivalData data = player.getData(ModAttachments.PLAYER_SURVIVAL);
        StateTrackService.initializeMissingTracks(data);
        StateTimeline timeline = StateDefinitionManager.INSTANCE.require(SurvivalStateIds.HUNGER);
        StateMutationService.setPosition(
                player,
                data,
                SurvivalStateIds.HUNGER,
                timeline.add(data.statePosition(SurvivalStateIds.HUNGER), foodHours));
        neutralizeVanillaHunger(player);
        SurvivalModifierService.forceRecompute(player, StateTrackService.gatherAllModifiers(player, data));
    }

    public static double calculateFoodHours(FoodProperties foodProperties) {
        ServerConfig config = ServerConfig.INSTANCE;
        return calculateFoodHours(
                foodProperties.nutrition(),
                foodProperties.saturation(),
                config.hoursPerHungerPoint.getAsDouble(),
                config.hoursPerSaturationPoint.getAsDouble());
    }

    public static double calculateFoodHours(
            int nutrition,
            float saturation,
            double hoursPerHungerPoint,
            double hoursPerSaturationPoint) {
        if (nutrition < 0 || saturation < 0.0F || hoursPerHungerPoint < 0.0D || hoursPerSaturationPoint < 0.0D) {
            throw new IllegalArgumentException("Food values and hunger conversion factors cannot be negative");
        }
        double result = nutrition * hoursPerHungerPoint + saturation * hoursPerSaturationPoint;
        if (!Double.isFinite(result)) {
            throw new IllegalArgumentException("Calculated food hunger-hours must be finite");
        }
        return result;
    }

    public static double decayPosition(StateTimeline timeline, double positionHours, double elapsedBiologicalHours) {
        if (!Double.isFinite(elapsedBiologicalHours) || elapsedBiologicalHours < 0.0D) {
            throw new IllegalArgumentException("Elapsed biological time must be finite and non-negative");
        }
        return timeline.add(positionHours, -elapsedBiologicalHours);
    }

    public static double decayMultiplier(boolean hungerEffectActive, double hungerEffectMultiplier) {
        if (!Double.isFinite(hungerEffectMultiplier) || hungerEffectMultiplier < 0.0D) {
            throw new IllegalArgumentException("Hunger effect decay multiplier must be finite and non-negative");
        }
        return hungerEffectActive ? hungerEffectMultiplier : 1.0D;
    }

    public static void neutralizeVanillaHunger(Player player) {
        FoodData foodData = player.getFoodData();
        foodData.setFoodLevel(20);
        foodData.setSaturation(0.0F);
        foodData.setExhaustion(0.0F);
    }
}
