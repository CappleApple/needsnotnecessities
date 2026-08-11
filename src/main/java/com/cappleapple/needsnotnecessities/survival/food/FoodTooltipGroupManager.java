package com.cappleapple.needsnotnecessities.survival.food;

import com.cappleapple.needsnotnecessities.NeedsNotNecessities;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;

public final class FoodTooltipGroupManager extends SimpleJsonResourceReloadListener {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    public static final FoodTooltipGroupManager INSTANCE = new FoodTooltipGroupManager();

    private volatile List<FoodTooltipGroupDefinition> definitions = List.of();

    private FoodTooltipGroupManager() {
        super(GSON, "food_tooltip_groups");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> resources, ResourceManager manager, ProfilerFiller profiler) {
        List<FoodTooltipGroupDefinition> loaded = new ArrayList<>();
        resources.forEach((id, element) -> loaded.add(parse(
                id, GsonHelper.convertToJsonObject(element, id.toString()))));
        loaded.sort(Comparator
                .comparingDouble(FoodTooltipGroupDefinition::minimumHours)
                .thenComparing(definition -> definition.id().toString()));
        definitions = List.copyOf(loaded);
        NeedsNotNecessities.LOGGER.info("Loaded {} food tooltip group definitions", loaded.size());
    }

    public Optional<FoodTooltipGroupDefinition> classify(double foodHours) {
        return definitions.stream().filter(definition -> definition.contains(foodHours)).findFirst();
    }

    public List<FoodTooltipGroupDefinition> definitions() {
        return definitions;
    }

    static FoodTooltipGroupDefinition parse(ResourceLocation id, JsonObject json) {
        String colorValue = GsonHelper.getAsString(json, "color", "#E8E8E8");
        return new FoodTooltipGroupDefinition(
                id,
                GsonHelper.getAsString(json, "name"),
                GsonHelper.getAsDouble(json, "minimum_hours"),
                json.has("maximum_hours")
                        ? GsonHelper.getAsDouble(json, "maximum_hours")
                        : Double.MAX_VALUE,
                parseColor(colorValue, id),
                GsonHelper.getAsString(json, "description", ""));
    }

    private static int parseColor(String value, ResourceLocation owner) {
        String normalized = value.startsWith("#") ? value.substring(1) : value;
        try {
            return Integer.parseUnsignedInt(normalized, 16) & 0xFFFFFF;
        } catch (NumberFormatException exception) {
            throw new JsonParseException("Invalid RGB food tooltip color '" + value + "' in " + owner, exception);
        }
    }
}
