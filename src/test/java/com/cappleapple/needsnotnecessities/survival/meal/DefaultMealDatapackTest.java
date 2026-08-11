package com.cappleapple.needsnotnecessities.survival.meal;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class DefaultMealDatapackTest {
    @Test
    void vegetablesUseCommonTagsWithoutIngredientSpecificOverrides() throws IOException {
        JsonObject vegetables = resource(
                "data/needs_not_necessities/meal_effects/vegetables.json");
        assertTrue(vegetables.getAsJsonArray("tags").asList().stream()
                .anyMatch(element -> element.getAsString()
                        .equals("c:foods/vegetable")));
        assertFalse(vegetables.getAsJsonArray("tags").asList().stream()
                .anyMatch(element -> element.getAsString().startsWith("needs_not_necessities:meal_groups/")));
    }

    private static JsonObject resource(String path) throws IOException {
        try (InputStream stream = DefaultMealDatapackTest.class.getClassLoader().getResourceAsStream(path)) {
            if (stream == null) {
                throw new IOException("Missing test resource " + path);
            }
            return JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
        }
    }
}
