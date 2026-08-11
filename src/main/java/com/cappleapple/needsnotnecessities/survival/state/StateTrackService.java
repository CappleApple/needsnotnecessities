package com.cappleapple.needsnotnecessities.survival.state;

import com.cappleapple.needsnotnecessities.NeedsNotNecessities;
import com.cappleapple.needsnotnecessities.config.DeathStatePolicy;
import com.cappleapple.needsnotnecessities.config.ServerConfig;
import com.cappleapple.needsnotnecessities.data.PlayerSurvivalData;
import com.cappleapple.needsnotnecessities.modifier.ModifierOperation;
import com.cappleapple.needsnotnecessities.modifier.SurvivalModifier;
import com.cappleapple.needsnotnecessities.survival.SurvivalModule;
import com.cappleapple.needsnotnecessities.survival.health.BaseHealthService;
import com.cappleapple.needsnotnecessities.survival.comfort.ComfortService;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import com.cappleapple.needsnotnecessities.api.provider.SurvivalProviderRegistry;
import com.cappleapple.needsnotnecessities.api.event.SurvivalModifierGatherEvent;
import net.neoforged.neoforge.common.NeoForge;

public final class StateTrackService {
    private StateTrackService() {
    }

    public static void initializeMissingTracks(PlayerSurvivalData data) {
        for (ResourceLocation systemId : List.of(
                SurvivalStateIds.HUNGER,
                SurvivalStateIds.THIRST,
                SurvivalStateIds.REST)) {
            Optional<StateTimeline> timeline = StateDefinitionManager.INSTANCE.find(systemId);
            if (enabled(systemId) && timeline.isPresent() && !data.hasStatePosition(systemId)) {
                data.setStatePosition(systemId, timeline.get().neutralPosition());
            }
        }
        if (!data.initialized()) {
            data.markInitialized();
        }
    }

    public static void clampEnabledTracks(PlayerSurvivalData data) {
        for (var entry : StateDefinitionManager.INSTANCE.snapshot().entrySet()) {
            if (enabled(entry.getKey())) {
                data.setStatePosition(entry.getKey(), entry.getValue().clamp(data.statePosition(entry.getKey())));
            }
        }
    }

    public static SurvivalStateDefinition currentState(PlayerSurvivalData data, ResourceLocation systemId) {
        StateTimeline timeline = StateDefinitionManager.INSTANCE.require(systemId);
        return timeline.stateAt(data.statePosition(systemId));
    }

    public static void applyDeathPolicy(
            PlayerSurvivalData data,
            ResourceLocation systemId,
            DeathStatePolicy policy) {
        if (!enabled(systemId) || policy == DeathStatePolicy.KEEP) {
            return;
        }
        StateTimeline timeline = StateDefinitionManager.INSTANCE.require(systemId);
        double replacement = switch (policy) {
            case KEEP -> data.statePosition(systemId);
            case RESET_NEUTRAL, CLEAR -> timeline.neutralPosition();
            case RESET_WORST -> timeline.worstPosition();
            case RESET_BEST -> timeline.bestPosition();
        };
        data.setStatePosition(systemId, replacement);
    }

    public static List<SurvivalModifier> gatherAllModifiers(PlayerSurvivalData data) {
        return gatherBuiltInModifiers(data);
    }

    public static List<SurvivalModifier> gatherAllModifiers(ServerPlayer player, PlayerSurvivalData data) {
        List<SurvivalModifier> result = new ArrayList<>(gatherBuiltInModifiers(data));
        if (ServerConfig.INSTANCE.isEnabled(SurvivalModule.COMPATIBILITY)) {
            SurvivalProviderRegistry.modifierProviders().forEach(provider -> {
                List<SurvivalModifier> provided = provider.gather(player);
                if (provided != null) {
                    result.addAll(provided);
                }
            });
            SurvivalModifierGatherEvent event = new SurvivalModifierGatherEvent(player, result);
            NeoForge.EVENT_BUS.post(event);
            return List.copyOf(event.modifiers());
        }
        return List.copyOf(result);
    }

    private static List<SurvivalModifier> gatherBuiltInModifiers(PlayerSurvivalData data) {
        List<SurvivalModifier> result = new ArrayList<>();
        for (var entry : StateDefinitionManager.INSTANCE.snapshot().entrySet()) {
            ResourceLocation systemId = entry.getKey();
            if (!enabled(systemId)) {
                continue;
            }
            SurvivalStateDefinition state = entry.getValue().stateAt(data.statePosition(systemId));
            result.addAll(state.modifiers());
            if (ServerConfig.INSTANCE.isEnabled(SurvivalModule.PASSIVE_REGENERATION)
                    && state.passiveRegenerationMultiplier() != 1.0D) {
                ResourceLocation modifierId = ResourceLocation.fromNamespaceAndPath(
                        state.id().getNamespace(), state.id().getPath() + "/passive_regeneration");
                result.add(new SurvivalModifier(
                        modifierId,
                        NeedsNotNecessities.id("passive_regeneration"),
                        state.passiveRegenerationMultiplier() - 1.0D,
                        ModifierOperation.MULTIPLY_TOTAL));
            }
        }
        if (ServerConfig.INSTANCE.isEnabled(SurvivalModule.ACTIVE_MEAL)) {
            data.activeMeal().ifPresent(meal -> result.addAll(meal.modifiers()));
        }
        BaseHealthService.configuredModifier().ifPresent(result::add);
        result.addAll(ComfortService.gatherModifiers(data));
        return List.copyOf(result);
    }

    private static boolean enabled(ResourceLocation systemId) {
        return SurvivalStateIds.moduleFor(systemId)
                .map(ServerConfig.INSTANCE::isEnabled)
                .orElse(true);
    }
}
