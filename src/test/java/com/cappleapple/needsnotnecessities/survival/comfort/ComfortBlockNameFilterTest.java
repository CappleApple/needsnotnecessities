package com.cappleapple.needsnotnecessities.survival.comfort;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

class ComfortBlockNameFilterTest {
    @Test
    void matchesPathTokensInAllowedNamespaces() {
        ComfortBlockNameFilter filter = ComfortBlockNameFilter.compile(
                "^(?!minecraft$).+$",
                "(?:^|_)(?:chair|armchair|stool)(?:_|$)",
                Optional.empty());

        assertTrue(filter.matches(id("handcrafted", "oak_chair")));
        assertTrue(filter.matches(id("furniture", "blue_armchair_left")));
        assertFalse(filter.matches(id("minecraft", "oak_chair")));
        assertFalse(filter.matches(id("example", "wheelchair_ramp")));
    }

    @Test
    void exclusionsPreventKnownUtilityFalsePositives() {
        ComfortBlockNameFilter filter = ComfortBlockNameFilter.compile(
                ".+",
                "(?:^|_)(?:table|desk)(?:_|$)",
                Optional.of("(?:^|_)(?:crafting|lamp)(?:_|$)"));

        assertTrue(filter.matches(id("example", "oak_desk")));
        assertFalse(filter.matches(id("example", "oak_crafting_table")));
        assertFalse(filter.matches(id("example", "table_lamp")));
    }

    @Test
    void rejectsInvalidExpressionsDuringReloadPreparation() {
        assertThrows(IllegalArgumentException.class, () ->
                ComfortBlockNameFilter.compile(".+", "[", Optional.empty()));
    }

    private static ResourceLocation id(String namespace, String path) {
        return ResourceLocation.fromNamespaceAndPath(namespace, path);
    }
}
