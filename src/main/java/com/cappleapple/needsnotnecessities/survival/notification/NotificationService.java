package com.cappleapple.needsnotnecessities.survival.notification;

import com.cappleapple.needsnotnecessities.config.ServerConfig;
import com.cappleapple.needsnotnecessities.data.PlayerSurvivalData;
import com.cappleapple.needsnotnecessities.network.SurvivalNotificationPayload;
import com.cappleapple.needsnotnecessities.survival.SurvivalModule;
import com.cappleapple.needsnotnecessities.survival.state.StateDefinitionManager;
import com.cappleapple.needsnotnecessities.survival.state.StateTimeline;
import com.cappleapple.needsnotnecessities.survival.state.SurvivalStateDefinition;
import com.cappleapple.needsnotnecessities.survival.state.SurvivalStateIds;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.neoforged.neoforge.network.PacketDistributor;

public final class NotificationService {
    private static final Map<UUID, Map<ResourceLocation, ResourceLocation>> LAST_STATES = new HashMap<>();

    private NotificationService() {
    }

    public static void initialize(ServerPlayer player, PlayerSurvivalData data) {
        LAST_STATES.put(player.getUUID(), currentStates(data));
    }

    public static void checkTransitions(ServerPlayer player, PlayerSurvivalData data) {
        if (!ServerConfig.INSTANCE.isEnabled(SurvivalModule.NOTIFICATIONS)) {
            LAST_STATES.put(player.getUUID(), currentStates(data));
            return;
        }
        Map<ResourceLocation, ResourceLocation> current = currentStates(data);
        Map<ResourceLocation, ResourceLocation> previous = LAST_STATES.put(player.getUUID(), current);
        if (previous == null) {
            return;
        }
        current.forEach((system, stateId) -> {
            if (!stateId.equals(previous.get(system))) {
                notifyTransition(player, data, system, stateId);
            }
        });
    }

    public static void forget(ServerPlayer player) {
        LAST_STATES.remove(player.getUUID());
    }

    private static void notifyTransition(
            ServerPlayer player,
            PlayerSurvivalData data,
            ResourceLocation system,
            ResourceLocation stateId) {
        StateTimeline timeline = StateDefinitionManager.INSTANCE.require(system);
        SurvivalStateDefinition state = timeline.states().stream()
                .filter(candidate -> candidate.id().equals(stateId))
                .findFirst()
                .orElse(timeline.stateAt(data.statePosition(system)));
        List<StateNotificationDefinition> notifications = state.notifications().stream()
                .filter(definition -> definition.type() != NotificationMode.NONE)
                .toList();
        if (notifications.isEmpty()) {
            return;
        }
        ResourceLocation cooldownId = state.id();
        long now = player.level().getGameTime();
        if (data.notificationCooldown(cooldownId) > now) {
            return;
        }
        long cooldownTicks = Math.max(0L, Math.round(
                ServerConfig.INSTANCE.notificationCooldownMinutes.getAsDouble() * 60.0D * 20.0D));
        data.setNotificationCooldown(cooldownId, now + cooldownTicks);

        String label = label(system);
        String fallbackMessage = label + ": " + state.displayName();
        for (StateNotificationDefinition definition : notifications) {
            String messageText = expand(
                    definition.message().isBlank() ? fallbackMessage : definition.message(), label, state);
            Component message = Component.literal(messageText);
            switch (definition.type()) {
                case SOUND -> player.playNotifySound(
                        resolveSound(definition), SoundSource.PLAYERS, definition.volume(), definition.pitch());
                case ACTION_BAR -> player.displayClientMessage(message, true);
                case CHAT -> player.sendSystemMessage(message);
                case TOAST -> {
                    String title = definition.title().isBlank()
                            ? "Survival State Changed"
                            : expand(definition.title(), label, state);
                    PacketDistributor.sendToPlayer(player, new SurvivalNotificationPayload(title, messageText));
                }
                case NONE -> { }
            }
        }
    }

    private static SoundEvent resolveSound(StateNotificationDefinition definition) {
        return definition.sound()
                .flatMap(BuiltInRegistries.SOUND_EVENT::getOptional)
                .orElse(SoundEvents.EXPERIENCE_ORB_PICKUP);
    }

    private static String expand(String template, String label, SurvivalStateDefinition state) {
        return template
                .replace("{system}", label)
                .replace("{state}", state.displayName());
    }

    private static Map<ResourceLocation, ResourceLocation> currentStates(PlayerSurvivalData data) {
        Map<ResourceLocation, ResourceLocation> result = new LinkedHashMap<>();
        ServerConfig config = ServerConfig.INSTANCE;
        for (var entry : List.of(
                Map.entry(SurvivalStateIds.HUNGER, SurvivalModule.HUNGER),
                Map.entry(SurvivalStateIds.THIRST, SurvivalModule.THIRST),
                Map.entry(SurvivalStateIds.REST, SurvivalModule.REST))) {
            if (config.isEnabled(entry.getValue())) {
                StateDefinitionManager.INSTANCE.find(entry.getKey()).ifPresent(timeline ->
                        result.put(entry.getKey(), timeline.stateAt(data.statePosition(entry.getKey())).id()));
            }
        }
        return Map.copyOf(result);
    }

    private static String label(ResourceLocation system) {
        if (SurvivalStateIds.HUNGER.equals(system)) {
            return "Hunger";
        }
        if (SurvivalStateIds.THIRST.equals(system)) {
            return "Thirst";
        }
        if (SurvivalStateIds.REST.equals(system)) {
            return "Rest";
        }
        return system.getPath();
    }
}
