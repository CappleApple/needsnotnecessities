package com.cappleapple.needsnotnecessities.config;

import com.cappleapple.needsnotnecessities.NeedsNotNecessities;
import com.cappleapple.needsnotnecessities.survival.comfort.ComfortBlockNameFilter;
import com.cappleapple.needsnotnecessities.survival.comfort.ComfortSourceDefinition;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.neoforged.fml.loading.FMLPaths;

/** Standalone, server-owned automatic furniture classification configuration. */
public final class ComfortAutoConfig {
    public static final String FILE_NAME = "comfort_auto_classification.json";
    private static final String DEFAULT_RESOURCE = "/default-configs/" + FILE_NAME;

    private ComfortAutoConfig() {
    }

    public static List<ComfortSourceDefinition> load() {
        Path path = FMLPaths.CONFIGDIR.get()
                .resolve(NeedsNotNecessities.MOD_ID)
                .resolve(FILE_NAME);
        try {
            seedIfMissing(path);
            try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                List<ComfortSourceDefinition> definitions = parse(JsonParser.parseReader(reader));
                NeedsNotNecessities.LOGGER.info(
                        "Loaded {} automatic comfort groups from {}",
                        definitions.size(),
                        path.toAbsolutePath());
                return definitions;
            }
        } catch (IOException | RuntimeException exception) {
            NeedsNotNecessities.LOGGER.error(
                    "Could not load automatic comfort config {}; automatic classification is disabled",
                    path.toAbsolutePath(),
                    exception);
            return List.of();
        }
    }

    static List<ComfortSourceDefinition> parse(JsonElement root) {
        JsonArray groups;
        if (root.isJsonArray()) {
            groups = root.getAsJsonArray();
        } else if (root.isJsonObject()) {
            JsonObject object = root.getAsJsonObject();
            if (!object.has("groups")) {
                return List.of();
            }
            groups = GsonHelper.convertToJsonArray(object.get("groups"), "groups");
        } else {
            throw new JsonParseException("Automatic comfort config must be an object or array");
        }

        List<ComfortSourceDefinition> definitions = new ArrayList<>();
        Set<String> usedGroups = new HashSet<>();
        for (int index = 0; index < groups.size(); index++) {
            JsonObject entry = GsonHelper.convertToJsonObject(groups.get(index), "groups[" + index + "]");
            String group = GsonHelper.getAsString(entry, "group");
            if (!usedGroups.add(group)) {
                throw new JsonParseException("Duplicate automatic comfort group '" + group + "'");
            }
            ResourceLocation id = ResourceLocation.tryParse(
                    NeedsNotNecessities.MOD_ID + ":automatic/" + group);
            if (id == null) {
                throw new JsonParseException("Invalid automatic comfort group '" + group + "'");
            }
            String name = GsonHelper.getAsString(entry, "name");
            if (name.isBlank()) {
                throw new JsonParseException("Automatic comfort group name cannot be blank: " + group);
            }
            double comfort = GsonHelper.getAsDouble(entry, "comfort");
            if (!Double.isFinite(comfort) || comfort <= 0.0D) {
                throw new JsonParseException("Automatic comfort must be positive and finite: " + group);
            }
            ComfortBlockNameFilter filter;
            try {
                filter = ComfortBlockNameFilter.compile(
                        GsonHelper.getAsString(entry, "namespace_regex", ".+"),
                        GsonHelper.getAsString(entry, "regex"),
                        entry.has("exclude_regex")
                                ? Optional.of(GsonHelper.getAsString(entry, "exclude_regex"))
                                : Optional.empty());
            } catch (IllegalArgumentException exception) {
                throw new JsonParseException(
                        "Invalid regex in automatic comfort group '" + group + "': " + exception.getMessage(),
                        exception);
            }
            definitions.add(new ComfortSourceDefinition(
                    id,
                    group,
                    name,
                    comfort,
                    Optional.empty(),
                    Optional.empty(),
                    Optional.of(filter)));
        }
        return List.copyOf(definitions);
    }

    private static void seedIfMissing(Path path) throws IOException {
        if (Files.exists(path)) {
            return;
        }
        Files.createDirectories(path.getParent());
        try (InputStream defaults = ComfortAutoConfig.class.getResourceAsStream(DEFAULT_RESOURCE)) {
            if (defaults == null) {
                throw new IOException("Missing bundled automatic comfort defaults " + DEFAULT_RESOURCE);
            }
            Files.copy(defaults, path);
        }
    }
}
