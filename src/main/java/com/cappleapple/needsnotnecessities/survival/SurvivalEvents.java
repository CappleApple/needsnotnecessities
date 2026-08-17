package com.cappleapple.needsnotnecessities.survival;

import com.cappleapple.needsnotnecessities.config.DeathStatePolicy;
import com.cappleapple.needsnotnecessities.config.ServerConfig;
import com.cappleapple.needsnotnecessities.data.ModAttachments;
import com.cappleapple.needsnotnecessities.data.PlayerSurvivalData;
import com.cappleapple.needsnotnecessities.modifier.SurvivalModifierService;
import com.cappleapple.needsnotnecessities.survival.state.StateDefinitionManager;
import com.cappleapple.needsnotnecessities.survival.state.StateTrackService;
import com.cappleapple.needsnotnecessities.survival.state.SurvivalStateIds;
import com.cappleapple.needsnotnecessities.survival.health.PassiveRegenerationService;
import com.cappleapple.needsnotnecessities.survival.hunger.HungerService;
import com.cappleapple.needsnotnecessities.survival.rest.RestService;
import com.cappleapple.needsnotnecessities.survival.rest.SleepRulesService;
import com.cappleapple.needsnotnecessities.survival.thirst.ThirstService;
import com.cappleapple.needsnotnecessities.survival.comfort.ComfortEffectManager;
import com.cappleapple.needsnotnecessities.survival.comfort.ComfortService;
import com.cappleapple.needsnotnecessities.survival.comfort.ComfortSourceManager;
import com.cappleapple.needsnotnecessities.survival.meal.ActiveMealService;
import com.cappleapple.needsnotnecessities.survival.meal.MealEffectManager;
import com.cappleapple.needsnotnecessities.survival.food.FoodTooltipGroupManager;
import com.cappleapple.needsnotnecessities.network.SurvivalSnapshotService;
import com.cappleapple.needsnotnecessities.survival.notification.NotificationService;
import com.cappleapple.needsnotnecessities.api.event.DrinkConsumedEvent;
import com.cappleapple.needsnotnecessities.api.event.FoodConsumedEvent;
import com.cappleapple.needsnotnecessities.api.event.PlayerSurvivalRespawnEvent;
import com.cappleapple.needsnotnecessities.api.event.SurvivalStateUpdateEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.TagsUpdatedEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import com.cappleapple.needsnotnecessities.command.SurvivalCommands;

public final class SurvivalEvents {
    private SurvivalEvents() {
    }

    public static void register(IEventBus gameBus) {
        gameBus.addListener(SurvivalEvents::onAddReloadListeners);
        gameBus.addListener(SurvivalEvents::onTagsUpdated);
        gameBus.addListener(SurvivalEvents::onPlayerLogin);
        gameBus.addListener(SurvivalEvents::onPlayerLogout);
        gameBus.addListener(SurvivalEvents::onPlayerClone);
        gameBus.addListener(SurvivalEvents::onPlayerRespawn);
        gameBus.addListener(SurvivalEvents::onPlayerChangedDimension);
        gameBus.addListener(SurvivalEvents::onPlayerTick);
        gameBus.addListener(SurvivalEvents::onFoodFinished);
        gameBus.addListener(SleepRulesService::onCanPlayerSleep);
        gameBus.addListener(SleepRulesService::onCanContinueSleeping);
        gameBus.addListener(SleepRulesService::onSleepFinished);
        gameBus.addListener(SurvivalEvents::onLivingDamaged);
        gameBus.addListener(SurvivalEvents::onPlayerAttack);
        gameBus.addListener(SurvivalCommands::register);
    }

    private static void onAddReloadListeners(AddReloadListenerEvent event) {
        event.addListener(StateDefinitionManager.INSTANCE);
        event.addListener(ComfortSourceManager.INSTANCE);
        event.addListener(ComfortEffectManager.INSTANCE);
        event.addListener(MealEffectManager.INSTANCE);
        event.addListener(FoodTooltipGroupManager.INSTANCE);
    }

    private static void onTagsUpdated(TagsUpdatedEvent event) {
        if (event.getUpdateCause() == TagsUpdatedEvent.UpdateCause.SERVER_DATA_LOAD) {
            ComfortSourceManager.INSTANCE.rebuildCache();
        }
    }

    private static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            PlayerSurvivalData data = player.getData(ModAttachments.PLAYER_SURVIVAL);
            boolean firstInitialization = ensureInitialized(data);
            StateTrackService.clampEnabledTracks(data);
            SurvivalModifierService.forceRecompute(player, StateTrackService.gatherAllModifiers(player, data));
            if (firstInitialization) {
                float startingHealth = (float) (player.getMaxHealth() * ServerConfig.INSTANCE.initialHealthPercentage.getAsDouble());
                player.setHealth(Math.clamp(startingHealth, Math.min(1.0F, player.getMaxHealth()), player.getMaxHealth()));
            }
            NotificationService.initialize(player, data);
            SurvivalSnapshotService.sync(player, data);
        }
    }

    private static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            SurvivalModifierService.forget(player);
            ComfortService.forget(player);
            NotificationService.forget(player);
        }
    }

    private static void onPlayerClone(PlayerEvent.Clone event) {
        if (!(event.getEntity() instanceof ServerPlayer replacement)) {
            return;
        }
        PlayerSurvivalData copied = event.getOriginal().hasData(ModAttachments.PLAYER_SURVIVAL)
                ? event.getOriginal().getData(ModAttachments.PLAYER_SURVIVAL).copy()
                : new PlayerSurvivalData();
        ensureInitialized(copied);
        if (event.isWasDeath() && ServerConfig.INSTANCE.deathPenaltiesEnabled.getAsBoolean()) {
            double previousHunger = copied.statePosition(SurvivalStateIds.HUNGER);
            double previousThirst = copied.statePosition(SurvivalStateIds.THIRST);
            applyDeathPolicies(copied);
            copied.setPendingDeathHealthReset(true);
            copied.setPendingRespawnPenaltyMessage(RespawnPenaltyService.shouldSendMessage(
                    previousHunger,
                    copied.statePosition(SurvivalStateIds.HUNGER),
                    previousThirst,
                    copied.statePosition(SurvivalStateIds.THIRST),
                    ServerConfig.INSTANCE.isEnabled(SurvivalModule.HUNGER),
                    ServerConfig.INSTANCE.isEnabled(SurvivalModule.THIRST)));
        }
        replacement.setData(ModAttachments.PLAYER_SURVIVAL, copied);
        SurvivalModifierService.forget(replacement);
    }

    private static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        PlayerSurvivalData data = player.getData(ModAttachments.PLAYER_SURVIVAL);
        ensureInitialized(data);
        SurvivalModifierService.forceRecompute(player, StateTrackService.gatherAllModifiers(player, data));
        if (data.pendingDeathHealthReset()) {
            float health = (float) (player.getMaxHealth() * ServerConfig.INSTANCE.respawnHealthPercentage.getAsDouble());
            float minimum = Math.min(1.0F, player.getMaxHealth());
            player.setHealth(Math.clamp(health, minimum, player.getMaxHealth()));
            data.setPendingDeathHealthReset(false);
        }
        if (data.pendingRespawnPenaltyMessage()) {
            String message = ServerConfig.INSTANCE.respawnPenaltyMessage.get();
            if (!message.isBlank()) {
                player.sendSystemMessage(Component.literal(message));
            }
            data.setPendingRespawnPenaltyMessage(false);
        }
        NotificationService.initialize(player, data);
        SurvivalSnapshotService.sync(player, data);
        if (ServerConfig.INSTANCE.isEnabled(SurvivalModule.COMPATIBILITY)) {
            NeoForge.EVENT_BUS.post(new PlayerSurvivalRespawnEvent(player, !event.isEndConquered()));
        }
    }

    private static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            PlayerSurvivalData data = player.getData(ModAttachments.PLAYER_SURVIVAL);
            ensureInitialized(data);
            SurvivalModifierService.forceRecompute(player, StateTrackService.gatherAllModifiers(player, data));
            NotificationService.initialize(player, data);
            SurvivalSnapshotService.sync(player, data);
        }
    }

    private static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        PlayerSurvivalData data = player.getData(ModAttachments.PLAYER_SURVIVAL);
        ensureInitialized(data);
        if (ServerConfig.INSTANCE.anyBiologicalModuleEnabled()) {
            data.advanceBiologicalTicks(1L);
        }
        HungerService.tick(player, data);
        RestService.tick(player, data);
        ActiveMealService.tick(player, data);
        ComfortService.tick(player, data);
        int interval = ServerConfig.INSTANCE.stateUpdateIntervalTicks.getAsInt();
        if (player.tickCount % interval == 0) {
            StateTrackService.clampEnabledTracks(data);
            SurvivalModifierService.recomputeIfChanged(player, StateTrackService.gatherAllModifiers(player, data));
            NotificationService.checkTransitions(player, data);
            SurvivalSnapshotService.sync(player, data);
            if (ServerConfig.INSTANCE.isEnabled(SurvivalModule.COMPATIBILITY)) {
                NeoForge.EVENT_BUS.post(new SurvivalStateUpdateEvent(player, data.biologicalAgeTicks()));
            }
        }
        PassiveRegenerationService.tick(player, data);
    }

    private static void onFoodFinished(LivingEntityUseItemEvent.Finish event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        FoodProperties food = event.getItem().getFoodProperties(player);
        boolean foodModuleEnabled = ServerConfig.INSTANCE.isEnabled(SurvivalModule.HUNGER)
                || ServerConfig.INSTANCE.isEnabled(SurvivalModule.THIRST)
                || ServerConfig.INSTANCE.isEnabled(SurvivalModule.ACTIVE_MEAL);
        if (food != null && foodModuleEnabled) {
            double foodHours = HungerService.calculateFoodHours(food);
            HungerService.consume(player, foodHours);
            ThirstService.onFoodConsumed(player, foodHours);
            ActiveMealService.onFoodConsumed(player, event.getItem(), foodHours);
            if (ServerConfig.INSTANCE.isEnabled(SurvivalModule.COMPATIBILITY)) {
                NeoForge.EVENT_BUS.post(new FoodConsumedEvent(
                        player, event.getItem(), food.nutrition(), food.saturation(), foodHours));
            }
        }
        if (ServerConfig.INSTANCE.isEnabled(SurvivalModule.THIRST)) {
            ThirstService.DrinkKind drinkKind = ThirstService.classify(event.getItem());
            ThirstService.onDrinkConsumed(player, event.getItem());
            double adjustment = drinkKind == ThirstService.DrinkKind.NORMAL
                    ? ServerConfig.INSTANCE.normalDrinkLevelAdjustment.getAsDouble()
                    : ServerConfig.INSTANCE.alcoholicDrinkLevelAdjustment.getAsDouble();
            if (drinkKind != ThirstService.DrinkKind.NONE
                    && ServerConfig.INSTANCE.isEnabled(SurvivalModule.COMPATIBILITY)) {
                NeoForge.EVENT_BUS.post(new DrinkConsumedEvent(player, event.getItem(), drinkKind, adjustment));
            }
        }
    }

    private static void onLivingDamaged(LivingDamageEvent.Post event) {
        if (event.getNewDamage() <= 0.0F) {
            return;
        }
        if (event.getEntity() instanceof ServerPlayer victim && event.getSource().getEntity() != null) {
            PassiveRegenerationService.markCombat(victim);
        }
        Entity attacker = event.getSource().getEntity();
        if (attacker instanceof ServerPlayer playerAttacker) {
            PassiveRegenerationService.markCombat(playerAttacker);
        }
    }

    private static void onPlayerAttack(AttackEntityEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            PassiveRegenerationService.markCombat(player);
        }
    }

    private static boolean ensureInitialized(PlayerSurvivalData data) {
        boolean wasInitialized = data.initialized();
        StateTrackService.initializeMissingTracks(data);
        return !wasInitialized;
    }

    private static void applyDeathPolicies(PlayerSurvivalData data) {
        ServerConfig config = ServerConfig.INSTANCE;
        StateTrackService.applyDeathPolicy(data, SurvivalStateIds.HUNGER, config.hungerDeathPolicy.get());
        StateTrackService.applyDeathPolicy(data, SurvivalStateIds.THIRST, config.thirstDeathPolicy.get());
        StateTrackService.applyDeathPolicy(data, SurvivalStateIds.REST, config.restDeathPolicy.get());

        if (config.isEnabled(SurvivalModule.COMFORT) && config.comfortDeathPolicy.get() != DeathStatePolicy.KEEP) {
            data.clearComfort();
        }
        if (config.isEnabled(SurvivalModule.ACTIVE_MEAL) && config.mealDeathPolicy.get() != DeathStatePolicy.KEEP) {
            data.clearActiveMeal();
        }
    }
}
