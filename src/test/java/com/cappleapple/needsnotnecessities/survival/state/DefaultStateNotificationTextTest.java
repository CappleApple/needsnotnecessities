package com.cappleapple.needsnotnecessities.survival.state;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class DefaultStateNotificationTextTest {
    @Test
    void highestStatesUseTheirFlavorMessages() throws IOException {
        assertEquals("You feel stuffed", actionBarMessage("hunger.json", "stuffed"));
        assertEquals("You feel hydrated", actionBarMessage("thirst.json", "refreshed"));
        assertEquals("You feel well rested", actionBarMessage("rest.json", "well_rested"));
    }

    private static String actionBarMessage(String file, String stateId) throws IOException {
        JsonObject timeline = resource("data/needs_not_necessities/survival_states/" + file);
        for (var state : timeline.getAsJsonArray("states")) {
            JsonObject stateObject = state.getAsJsonObject();
            if (!stateId.equals(stateObject.get("id").getAsString())) {
                continue;
            }
            for (var notification : stateObject.getAsJsonArray("notifications")) {
                JsonObject notificationObject = notification.getAsJsonObject();
                if ("ACTION_BAR".equals(notificationObject.get("type").getAsString())) {
                    return notificationObject.get("message").getAsString();
                }
            }
        }
        throw new IOException("Missing ACTION_BAR notification for state " + stateId + " in " + file);
    }

    private static JsonObject resource(String path) throws IOException {
        try (InputStream stream = DefaultStateNotificationTextTest.class.getClassLoader().getResourceAsStream(path)) {
            if (stream == null) {
                throw new IOException("Missing test resource " + path);
            }
            return JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
        }
    }
}
