package com.cappleapple.needsnotnecessities.survival.state;

import com.cappleapple.needsnotnecessities.NeedsNotNecessities;
import com.cappleapple.needsnotnecessities.modifier.ModifierOperation;
import com.cappleapple.needsnotnecessities.modifier.SurvivalModifier;
import com.cappleapple.needsnotnecessities.survival.notification.NotificationMode;
import com.cappleapple.needsnotnecessities.survival.notification.StateNotificationDefinition;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;

public final class StateDefinitionManager extends SimpleJsonResourceReloadListener {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    public static final StateDefinitionManager INSTANCE = new StateDefinitionManager();

    private volatile Map<ResourceLocation, StateTimeline> timelines = Map.of();
    private volatile long generation;

    private StateDefinitionManager() {
        super(GSON, "survival_states");
    }

    @Override
    protected void apply(
            Map<ResourceLocation, JsonElement> resources,
            ResourceManager resourceManager,
            ProfilerFiller profiler) {
        Map<ResourceLocation, StateTimeline> loaded = new HashMap<>();
        resources.forEach((id, json) -> {
            StateTimeline timeline = parseTimeline(id, GsonHelper.convertToJsonObject(json, id.toString()));
            if (loaded.put(id, timeline) != null) {
                throw new JsonParseException("Duplicate survival state timeline: " + id);
            }
        });
        requireBuiltInTimeline(loaded, SurvivalStateIds.HUNGER);
        requireBuiltInTimeline(loaded, SurvivalStateIds.THIRST);
        requireBuiltInTimeline(loaded, SurvivalStateIds.REST);
        timelines = Map.copyOf(loaded);
        generation++;
        NeedsNotNecessities.LOGGER.info("Loaded {} survival state timelines", loaded.size());
    }

    public Optional<StateTimeline> find(ResourceLocation id) {
        return Optional.ofNullable(timelines.get(id));
    }

    public StateTimeline require(ResourceLocation id) {
        StateTimeline timeline = timelines.get(id);
        if (timeline == null) {
            throw new IllegalStateException("Missing survival state timeline: " + id);
        }
        return timeline;
    }

    public Map<ResourceLocation, StateTimeline> snapshot() {
        return timelines;
    }

    public long generation() {
        return generation;
    }

    private static StateTimeline parseTimeline(ResourceLocation timelineId, JsonObject root) {
        String neutralName = GsonHelper.getAsString(root, "neutral_state");
        JsonArray stateArray = GsonHelper.getAsJsonArray(root, "states");
        List<SurvivalStateDefinition> states = new ArrayList<>();
        ResourceLocation neutralId = stateId(timelineId, neutralName);

        for (JsonElement element : stateArray) {
            JsonObject stateJson = GsonHelper.convertToJsonObject(element, "state");
            String localStateId = GsonHelper.getAsString(stateJson, "id");
            ResourceLocation stateId = stateId(timelineId, localStateId);
            String displayName = GsonHelper.getAsString(stateJson, "name");
            int order = GsonHelper.getAsInt(stateJson, "order");
            double duration = GsonHelper.getAsDouble(stateJson, "duration_hours");
            double regenMultiplier = GsonHelper.getAsDouble(stateJson, "passive_regeneration_multiplier", 1.0D);
            String description = GsonHelper.getAsString(stateJson, "description", "");
            List<SurvivalModifier> modifiers = parseModifiers(timelineId, localStateId, stateJson);
            List<StateNotificationDefinition> notifications = parseNotifications(stateJson, stateId);
            states.add(new SurvivalStateDefinition(
                    stateId,
                    displayName,
                    order,
                    duration,
                    regenMultiplier,
                    modifiers,
                    notifications,
                    description));
        }
        return new StateTimeline(timelineId, neutralId, states);
    }

    private static List<StateNotificationDefinition> parseNotifications(JsonObject stateJson, ResourceLocation stateId) {
        if (!stateJson.has("notifications")) {
            return List.of();
        }
        List<StateNotificationDefinition> notifications = new ArrayList<>();
        for (JsonElement element : GsonHelper.getAsJsonArray(stateJson, "notifications")) {
            JsonObject json;
            if (element.isJsonPrimitive()) {
                json = new JsonObject();
                json.addProperty("type", element.getAsString());
            } else {
                json = GsonHelper.convertToJsonObject(element, "notification");
            }
            NotificationMode type = NotificationMode.parse(GsonHelper.getAsString(json, "type"))
                    .orElseThrow(() -> new JsonParseException("Unsupported notification type in " + stateId));
            Optional<ResourceLocation> sound = Optional.empty();
            if (json.has("sound")) {
                sound = Optional.of(parseResourceLocation(GsonHelper.getAsString(json, "sound"), "notification sound"));
            }
            notifications.add(new StateNotificationDefinition(
                    type,
                    sound,
                    GsonHelper.getAsString(json, "message", ""),
                    GsonHelper.getAsString(json, "title", ""),
                    GsonHelper.getAsFloat(json, "volume", 0.65F),
                    GsonHelper.getAsFloat(json, "pitch", 1.0F)));
        }
        return List.copyOf(notifications);
    }

    private static List<SurvivalModifier> parseModifiers(
            ResourceLocation timelineId, String localStateId, JsonObject stateJson) {
        if (!stateJson.has("modifiers")) {
            return List.of();
        }
        List<SurvivalModifier> modifiers = new ArrayList<>();
        JsonArray array = GsonHelper.getAsJsonArray(stateJson, "modifiers");
        for (JsonElement element : array) {
            JsonObject modifierJson = GsonHelper.convertToJsonObject(element, "modifier");
            String localModifierId = GsonHelper.getAsString(modifierJson, "id");
            ResourceLocation modifierId = ResourceLocation.fromNamespaceAndPath(
                    timelineId.getNamespace(),
                    "state/" + timelineId.getPath() + "/" + localStateId + "/" + localModifierId);
            ResourceLocation target = parseResourceLocation(GsonHelper.getAsString(modifierJson, "target"), "modifier target");
            double amount = GsonHelper.getAsDouble(modifierJson, "amount");
            ModifierOperation operation = ModifierOperation.parse(
                    GsonHelper.getAsString(modifierJson, "operation", "ADD"));
            modifiers.add(new SurvivalModifier(modifierId, target, amount, operation));
        }
        return List.copyOf(modifiers);
    }

    private static ResourceLocation stateId(ResourceLocation timelineId, String value) {
        if (value.indexOf(':') >= 0) {
            return parseResourceLocation(value, "state ID");
        }
        return ResourceLocation.fromNamespaceAndPath(
                timelineId.getNamespace(), timelineId.getPath() + "/" + value);
    }

    private static ResourceLocation parseResourceLocation(String value, String label) {
        ResourceLocation parsed = ResourceLocation.tryParse(value);
        if (parsed == null) {
            throw new JsonParseException("Invalid " + label + ": " + value);
        }
        return parsed;
    }

    private static void requireBuiltInTimeline(Map<ResourceLocation, StateTimeline> loaded, ResourceLocation id) {
        if (!loaded.containsKey(id)) {
            throw new JsonParseException("Required built-in survival state timeline was removed: " + id);
        }
    }
}
