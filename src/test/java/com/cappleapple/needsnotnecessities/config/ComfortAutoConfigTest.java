package com.cappleapple.needsnotnecessities.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

class ComfortAutoConfigTest {
    @Test
    void emptyObjectDisablesEveryAutomaticGroup() {
        assertTrue(ComfortAutoConfig.parse(JsonParser.parseString("{}")).isEmpty());
    }

    @Test
    void acceptsObjectAndBareArrayForms() {
        String entry = """
                {
                  "group": "chairs",
                  "name": "Chair",
                  "regex": "(?:^|_)chair(?:_|$)",
                  "comfort": 6.0
                }
                """;
        var objectDefinitions = ComfortAutoConfig.parse(
                JsonParser.parseString("{\"groups\":[" + entry + "]}"));
        var arrayDefinitions = ComfortAutoConfig.parse(JsonParser.parseString("[" + entry + "]"));

        assertEquals(1, objectDefinitions.size());
        assertEquals(objectDefinitions.get(0).type(), arrayDefinitions.get(0).type());
        assertEquals("chairs", objectDefinitions.get(0).type());
        assertEquals("Chair", objectDefinitions.get(0).displayName());
        assertEquals(6.0D, objectDefinitions.get(0).comfort());
    }

    @Test
    void rejectsDuplicateGroups() {
        String duplicate = """
                {"groups":[
                  {"group":"chairs","name":"Chair","regex":"chair","comfort":4},
                  {"group":"chairs","name":"Seat","regex":"seat","comfort":6}
                ]}
                """;
        assertThrows(JsonParseException.class, () ->
                ComfortAutoConfig.parse(JsonParser.parseString(duplicate)));
    }
}
