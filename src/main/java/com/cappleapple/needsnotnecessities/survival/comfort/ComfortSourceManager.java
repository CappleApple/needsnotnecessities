package com.cappleapple.needsnotnecessities.survival.comfort;

import com.cappleapple.needsnotnecessities.NeedsNotNecessities;
import com.cappleapple.needsnotnecessities.config.ComfortAutoConfig;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import java.util.ArrayList;
import java.util.HashMap;
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
import net.minecraft.world.level.block.Block;

public final class ComfortSourceManager extends SimpleJsonResourceReloadListener {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    public static final ComfortSourceManager INSTANCE = new ComfortSourceManager();

    private volatile List<ComfortSourceDefinition> datapackDefinitions = List.of();
    private volatile List<ComfortSourceDefinition> definitions = List.of();
    private volatile Map<Block, List<ComfortSourceDefinition>> definitionsByBlock = Map.of();

    private ComfortSourceManager() {
        super(GSON, "comfort_sources");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> resources, ResourceManager manager, ProfilerFiller profiler) {
        List<ComfortSourceDefinition> loaded = resources.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> parse(
                        entry.getKey(),
                        GsonHelper.convertToJsonObject(entry.getValue(), entry.getKey().toString())))
                .toList();
        datapackDefinitions = List.copyOf(loaded);
        definitions = datapackDefinitions;
        definitionsByBlock = Map.of();
        NeedsNotNecessities.LOGGER.info(
                "Loaded {} comfort source definitions; the block cache will rebuild after tags update",
                loaded.size());
    }

    public synchronized void rebuildCache() {
        List<ComfortSourceDefinition> loaded = new ArrayList<>(datapackDefinitions);
        loaded.addAll(ComfortAutoConfig.load());
        definitions = List.copyOf(loaded);
        Map<Block, List<ComfortSourceDefinition>> indexed = new HashMap<>();
        int automaticMatches = 0;
        for (Block block : BuiltInRegistries.BLOCK) {
            ResourceLocation blockId = BuiltInRegistries.BLOCK.getKey(block);
            List<ComfortSourceDefinition> matches = new ArrayList<>();
            for (ComfortSourceDefinition definition : loaded) {
                if (definition.matches(block.defaultBlockState(), blockId)) {
                    matches.add(definition);
                }
            }
            List<ComfortSourceDefinition> selected = prioritizeExplicit(matches);
            if (!selected.isEmpty()) {
                indexed.put(block, selected);
                automaticMatches += (int) selected.stream()
                        .filter(definition -> definition.autoMatch().isPresent())
                        .count();
            }
        }
        Map<Block, List<ComfortSourceDefinition>> immutableIndex = new HashMap<>();
        indexed.forEach((block, matches) -> immutableIndex.put(block, List.copyOf(matches)));
        definitionsByBlock = Map.copyOf(immutableIndex);
        NeedsNotNecessities.LOGGER.info(
                "Rebuilt comfort cache with {} matching blocks ({} automatic assignments)",
                indexed.size(),
                automaticMatches);
    }

    public List<ComfortSourceDefinition> definitions() {
        return definitions;
    }

    public List<ComfortSourceDefinition> matching(Block block) {
        return definitionsByBlock.getOrDefault(block, List.of());
    }

    static List<ComfortSourceDefinition> prioritizeExplicit(List<ComfortSourceDefinition> matches) {
        List<ComfortSourceDefinition> explicit = matches.stream()
                .filter(definition -> definition.autoMatch().isEmpty())
                .toList();
        if (!explicit.isEmpty()) {
            return explicit;
        }
        return matches.isEmpty() ? List.of() : List.of(matches.getFirst());
    }

    private static ComfortSourceDefinition parse(ResourceLocation id, JsonObject json) {
        String type = GsonHelper.getAsString(json, "type");
        double comfort = GsonHelper.getAsDouble(json, "comfort");
        boolean hasBlock = json.has("block");
        boolean hasTag = json.has("tag");
        if (hasBlock == hasTag) {
            throw new JsonParseException("Comfort source must define exactly one of block or tag: " + id);
        }
        Optional<Block> block = Optional.empty();
        Optional<TagKey<Block>> tag = Optional.empty();
        if (hasBlock) {
            ResourceLocation blockId = parseId(GsonHelper.getAsString(json, "block"), id);
            block = BuiltInRegistries.BLOCK.getOptional(blockId);
            if (block.isEmpty()) {
                throw new JsonParseException("Unknown comfort block " + blockId + " in " + id);
            }
        } else {
            tag = Optional.of(TagKey.create(Registries.BLOCK, parseId(GsonHelper.getAsString(json, "tag"), id)));
        }
        return new ComfortSourceDefinition(
                id,
                type,
                GsonHelper.getAsString(json, "name", type),
                comfort,
                block,
                tag,
                Optional.empty());
    }

    private static ResourceLocation parseId(String value, ResourceLocation owner) {
        ResourceLocation parsed = ResourceLocation.tryParse(value);
        if (parsed == null) {
            throw new JsonParseException("Invalid resource location '" + value + "' in " + owner);
        }
        return parsed;
    }
}
