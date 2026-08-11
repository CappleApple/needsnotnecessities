package com.cappleapple.needsnotnecessities.survival.comfort;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.junit.jupiter.api.Test;

class DefaultComfortDatapackTest {
    private static final Map<String, Double> DEFAULTS = Map.of(
            "beds", 6.0D,
            "chairs", 4.0D,
            "benches", 4.0D,
            "sofas", 6.0D,
            "tables", 5.0D,
            "lighting", 4.0D,
            "hearths", 8.0D);

    @Test
    void everyFurnitureTypeHasSourceTagAndAutomaticFilter() throws IOException {
        for (var entry : DEFAULTS.entrySet()) {
            String type = entry.getKey();
            JsonObject source = resource("data/needs_not_necessities/comfort_sources/" + type + ".json");
            assertEquals("needs_not_necessities:comfort/" + type, source.get("tag").getAsString());
            assertEquals(type, source.get("type").getAsString());
            assertEquals(entry.getValue(), source.get("comfort").getAsDouble());
            assertTrue(source.has("name"));
            assertFalse(source.has("auto_match"));

            JsonObject tag = resource("data/needs_not_necessities/tags/block/comfort/" + type + ".json");
            assertTrue(tag.has("values"));
        }

        JsonObject automatic = resource("default-configs/comfort_auto_classification.json");
        assertEquals(DEFAULTS.size(), automatic.getAsJsonArray("groups").size());
    }

    private static JsonObject resource(String path) throws IOException {
        try (InputStream stream = DefaultComfortDatapackTest.class.getClassLoader().getResourceAsStream(path)) {
            if (stream == null) {
                throw new IOException("Missing test resource " + path);
            }
            return JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
        }
    }
}
