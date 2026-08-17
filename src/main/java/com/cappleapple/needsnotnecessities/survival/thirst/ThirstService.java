package com.cappleapple.needsnotnecessities.survival.thirst;

import com.cappleapple.needsnotnecessities.config.ServerConfig;
import com.cappleapple.needsnotnecessities.data.ModAttachments;
import com.cappleapple.needsnotnecessities.data.ModTags;
import com.cappleapple.needsnotnecessities.data.PlayerSurvivalData;
import com.cappleapple.needsnotnecessities.modifier.SurvivalModifierService;
import com.cappleapple.needsnotnecessities.survival.SurvivalModule;
import com.cappleapple.needsnotnecessities.survival.state.StateDefinitionManager;
import com.cappleapple.needsnotnecessities.survival.state.StateTimeline;
import com.cappleapple.needsnotnecessities.survival.state.StateTrackService;
import com.cappleapple.needsnotnecessities.survival.state.SurvivalStateIds;
import com.cappleapple.needsnotnecessities.survival.state.StateMutationService;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import com.cappleapple.needsnotnecessities.api.provider.SurvivalProviderRegistry;

public final class ThirstService {
    private ThirstService() {
    }

    public static void onFoodConsumed(ServerPlayer player, double foodHours) {
        if (!ServerConfig.INSTANCE.isEnabled(SurvivalModule.THIRST)
                || player.isCreative()
                || player.isSpectator()) {
            return;
        }
        double pressure = calculateFoodPressure(
                foodHours, ServerConfig.INSTANCE.thirstHoursPerFoodHour.getAsDouble());
        adjustHours(player, -pressure, true);
    }

    public static DrinkKind classify(ItemStack stack) {
        if (stack.is(ModTags.Items.ALCOHOLIC_DRINKS)) {
            return DrinkKind.ALCOHOLIC;
        }
        if (ServerConfig.INSTANCE.isEnabled(SurvivalModule.COMPATIBILITY)) {
            for (var classifier : SurvivalProviderRegistry.drinkClassifiers()) {
                DrinkKind provided = classifier.classify(stack);
                if (provided != null && provided != DrinkKind.NONE) {
                    return provided;
                }
            }
        }
        if (stack.is(ModTags.Items.DRINKS)) {
            return DrinkKind.NORMAL;
        }
        return DrinkKind.NONE;
    }

    public static void onDrinkConsumed(ServerPlayer player, ItemStack consumedStack) {
        if (!ServerConfig.INSTANCE.isEnabled(SurvivalModule.THIRST)
                || player.isCreative()
                || player.isSpectator()) {
            return;
        }
        DrinkKind kind = classify(consumedStack);
        double levelDelta = switch (kind) {
            case NORMAL -> ServerConfig.INSTANCE.normalDrinkLevelAdjustment.getAsDouble();
            case ALCOHOLIC -> ServerConfig.INSTANCE.alcoholicDrinkLevelAdjustment.getAsDouble();
            case NONE -> 0.0D;
        };
        if (levelDelta != 0.0D) {
            PlayerSurvivalData data = player.getData(ModAttachments.PLAYER_SURVIVAL);
            StateTrackService.initializeMissingTracks(data);
            StateTimeline timeline = StateDefinitionManager.INSTANCE.require(SurvivalStateIds.THIRST);
            StateMutationService.setPosition(
                    player,
                    data,
                    SurvivalStateIds.THIRST,
                    timeline.moveLevels(data.statePosition(SurvivalStateIds.THIRST), levelDelta));
            SurvivalModifierService.forceRecompute(player, StateTrackService.gatherAllModifiers(player, data));
        }
    }

    public static void adjustHours(ServerPlayer player, double amount, boolean recomputeImmediately) {
        PlayerSurvivalData data = player.getData(ModAttachments.PLAYER_SURVIVAL);
        StateTrackService.initializeMissingTracks(data);
        StateTimeline timeline = StateDefinitionManager.INSTANCE.require(SurvivalStateIds.THIRST);
        StateMutationService.setPosition(
                player,
                data,
                SurvivalStateIds.THIRST,
                timeline.add(data.statePosition(SurvivalStateIds.THIRST), amount));
        if (recomputeImmediately) {
            SurvivalModifierService.forceRecompute(player, StateTrackService.gatherAllModifiers(player, data));
        }
    }

    public static double calculateFoodPressure(double foodHours, double thirstHoursPerFoodHour) {
        if (!Double.isFinite(foodHours)
                || foodHours < 0.0D
                || !Double.isFinite(thirstHoursPerFoodHour)
                || thirstHoursPerFoodHour < 0.0D) {
            throw new IllegalArgumentException("Food-hours and thirst ratio must be finite and non-negative");
        }
        return foodHours * thirstHoursPerFoodHour;
    }

    public enum DrinkKind {
        NONE,
        NORMAL,
        ALCOHOLIC
    }
}
