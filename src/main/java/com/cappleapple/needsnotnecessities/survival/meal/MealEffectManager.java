package com.cappleapple.needsnotnecessities.survival.meal;

import com.cappleapple.needsnotnecessities.NeedsNotNecessities;
import com.cappleapple.needsnotnecessities.modifier.ModifierOperation;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.tags.TagKey;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public final class MealEffectManager extends SimpleJsonResourceReloadListener {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    public static final MealEffectManager INSTANCE = new MealEffectManager();

    private volatile List<MealEffectDefinition> definitions = List.of();

    private MealEffectManager() {
        super(GSON, "meal_effects");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> resources, ResourceManager manager, ProfilerFiller profiler) {
        List<MealEffectDefinition> loaded = new ArrayList<>();
        resources.forEach((id, element) -> loaded.add(parse(id, GsonHelper.convertToJsonObject(element, id.toString()))));
        definitions = List.copyOf(loaded);
        MealRecipeAnalyzer.clearCache();
        NeedsNotNecessities.LOGGER.info("Loaded {} item/tag meal effect definitions", loaded.size());
    }

    public List<MealEffectDefinition> matching(ItemStack stack) {
        return definitions.stream().filter(definition -> definition.matches(stack)).toList();
    }

    public List<MealEffectDefinition> definitions() {
        return definitions;
    }

    private static MealEffectDefinition parse(ResourceLocation id, JsonObject json) {
        boolean hasItem = json.has("item");
        boolean hasTags = json.has("tags");
        if (hasItem == hasTags) {
            throw new JsonParseException("Meal effect must define exactly one item or tags selector: " + id);
        }
        Optional<Item> item = Optional.empty();
        List<TagKey<Item>> tags = new ArrayList<>();
        if (hasItem) {
            ResourceLocation itemId = parseId(GsonHelper.getAsString(json, "item"), id);
            item = BuiltInRegistries.ITEM.getOptional(itemId);
            if (item.isEmpty()) {
                throw new JsonParseException("Unknown meal-effect item " + itemId + " in " + id);
            }
        } else {
            for (JsonElement element : GsonHelper.getAsJsonArray(json, "tags")) {
                tags.add(TagKey.create(Registries.ITEM, parseId(element.getAsString(), id)));
            }
            if (tags.isEmpty()) {
                throw new JsonParseException("Meal effect tag selector cannot be empty: " + id);
            }
        }

        Map<String, Double> traits = new LinkedHashMap<>();
        if (json.has("traits")) {
            GsonHelper.getAsJsonObject(json, "traits").entrySet().forEach(entry -> traits.put(entry.getKey(), entry.getValue().getAsDouble()));
        }
        List<MealBonusTemplate> bonuses = new ArrayList<>();
        if (json.has("bonuses")) {
            JsonArray bonusArray = GsonHelper.getAsJsonArray(json, "bonuses");
            for (JsonElement element : bonusArray) {
                JsonObject bonus = GsonHelper.convertToJsonObject(element, "bonus");
                String localId = GsonHelper.getAsString(bonus, "id");
                bonuses.add(new MealBonusTemplate(
                        ResourceLocation.fromNamespaceAndPath(id.getNamespace(), "meal/" + id.getPath() + "/" + localId),
                        GsonHelper.getAsString(bonus, "trait"),
                        parseId(GsonHelper.getAsString(bonus, "target"), id),
                        GsonHelper.getAsDouble(bonus, "amount"),
                        ModifierOperation.parse(GsonHelper.getAsString(bonus, "operation", "ADD"))));
            }
        }
        return new MealEffectDefinition(
                id,
                item,
                tags,
                traits,
                bonuses,
                GsonHelper.getAsDouble(json, "score_bonus", 0.0D),
                GsonHelper.getAsDouble(json, "duration_bonus_hours", 0.0D));
    }

    private static ResourceLocation parseId(String value, ResourceLocation owner) {
        ResourceLocation parsed = ResourceLocation.tryParse(value);
        if (parsed == null) {
            throw new JsonParseException("Invalid resource location '" + value + "' in " + owner);
        }
        return parsed;
    }
}
