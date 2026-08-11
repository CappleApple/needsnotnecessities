package com.cappleapple.needsnotnecessities.survival.comfort;

import com.cappleapple.needsnotnecessities.NeedsNotNecessities;
import com.cappleapple.needsnotnecessities.modifier.ModifierOperation;
import com.cappleapple.needsnotnecessities.modifier.SurvivalModifier;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;

public final class ComfortEffectManager extends SimpleJsonResourceReloadListener {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    public static final ComfortEffectManager INSTANCE = new ComfortEffectManager();

    private volatile List<ComfortEffectDefinition> definitions = List.of();

    private ComfortEffectManager() {
        super(GSON, "comfort_effects");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> resources, ResourceManager manager, ProfilerFiller profiler) {
        List<ComfortEffectDefinition> loaded = new ArrayList<>();
        resources.forEach((id, element) -> loaded.add(parse(id, GsonHelper.convertToJsonObject(element, id.toString()))));
        definitions = List.copyOf(loaded);
        NeedsNotNecessities.LOGGER.info("Loaded {} comfort effect definitions", loaded.size());
    }

    public List<SurvivalModifier> modifiersAt(double comfort) {
        return definitions.stream().flatMap(effect -> effect.modifiersAt(comfort).stream()).toList();
    }

    public List<ComfortEffectDefinition> definitions() {
        return definitions;
    }

    private static ComfortEffectDefinition parse(ResourceLocation id, JsonObject json) {
        double threshold = GsonHelper.getAsDouble(json, "threshold");
        boolean repeat = GsonHelper.getAsBoolean(json, "repeat", false);
        JsonArray array = GsonHelper.getAsJsonArray(json, "modifiers");
        List<SurvivalModifier> modifiers = new ArrayList<>();
        for (JsonElement element : array) {
            JsonObject modifier = GsonHelper.convertToJsonObject(element, "modifier");
            String localId = GsonHelper.getAsString(modifier, "id");
            ResourceLocation modifierId = ResourceLocation.fromNamespaceAndPath(
                    id.getNamespace(), "comfort/" + id.getPath() + "/" + localId);
            ResourceLocation target = ResourceLocation.tryParse(GsonHelper.getAsString(modifier, "target"));
            if (target == null) {
                throw new JsonParseException("Invalid comfort modifier target in " + id);
            }
            modifiers.add(new SurvivalModifier(
                    modifierId,
                    target,
                    GsonHelper.getAsDouble(modifier, "amount"),
                    ModifierOperation.parse(GsonHelper.getAsString(modifier, "operation", "ADD"))));
        }
        return new ComfortEffectDefinition(id, threshold, repeat, modifiers);
    }
}
