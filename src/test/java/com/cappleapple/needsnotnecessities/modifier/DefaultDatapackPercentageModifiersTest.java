package com.cappleapple.needsnotnecessities.modifier;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class DefaultDatapackPercentageModifiersTest {
    @ParameterizedTest
    @MethodSource("bundledGameplayDefinitions")
    void bundledGameplayModifiersNeverUseFlatAdditions(String path) throws IOException {
        assertModifiersArePercentageBased(resource(path), path);
    }

    private static Stream<String> bundledGameplayDefinitions() {
        return Stream.of(
                "data/needs_not_necessities/meal_effects/fish.json",
                "data/needs_not_necessities/meal_effects/fruit.json",
                "data/needs_not_necessities/meal_effects/golden_food.json",
                "data/needs_not_necessities/meal_effects/grain.json",
                "data/needs_not_necessities/meal_effects/meat.json",
                "data/needs_not_necessities/meal_effects/soups.json",
                "data/needs_not_necessities/meal_effects/vegetables.json",
                "data/needs_not_necessities/comfort_effects/health.json",
                "data/needs_not_necessities/comfort_effects/knockback.json",
                "data/needs_not_necessities/comfort_effects/regeneration.json",
                "data/needs_not_necessities/survival_states/hunger.json",
                "data/needs_not_necessities/survival_states/thirst.json",
                "data/needs_not_necessities/survival_states/rest.json");
    }

    private static void assertModifiersArePercentageBased(JsonElement element, String path) {
        if (element.isJsonObject()) {
            JsonObject object = element.getAsJsonObject();
            if (object.has("operation")) {
                assertTrue(object.get("operation").getAsString().startsWith("MULTIPLY_"),
                        () -> path + " contains a non-percentage modifier: " + object);
            }
            object.entrySet().forEach(entry -> assertModifiersArePercentageBased(entry.getValue(), path));
        } else if (element.isJsonArray()) {
            JsonArray array = element.getAsJsonArray();
            array.forEach(child -> assertModifiersArePercentageBased(child, path));
        }
    }

    private static JsonObject resource(String path) throws IOException {
        try (InputStream stream = DefaultDatapackPercentageModifiersTest.class
                .getClassLoader()
                .getResourceAsStream(path)) {
            if (stream == null) {
                throw new IOException("Missing test resource " + path);
            }
            return JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
        }
    }
}
