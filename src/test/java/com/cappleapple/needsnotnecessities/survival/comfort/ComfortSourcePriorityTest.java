package com.cappleapple.needsnotnecessities.survival.comfort;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.cappleapple.needsnotnecessities.NeedsNotNecessities;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import org.junit.jupiter.api.Test;

class ComfortSourcePriorityTest {
    @Test
    void anyExplicitSelectorSuppressesAllRegexClassificationsForThatBlock() {
        ComfortSourceDefinition automaticChair = automatic("chairs", 20.0D);
        ComfortSourceDefinition automaticTable = automatic("tables", 30.0D);
        ComfortSourceDefinition explicitLighting = new ComfortSourceDefinition(
                NeedsNotNecessities.id("lighting"),
                "lighting",
                "Lighting",
                1.0D,
                Optional.empty(),
                Optional.of(TagKey.create(Registries.BLOCK, NeedsNotNecessities.id("comfort/lighting"))),
                Optional.empty());

        assertEquals(
                List.of(explicitLighting),
                ComfortSourceManager.prioritizeExplicit(
                        List.of(automaticChair, explicitLighting, automaticTable)));
    }

    @Test
    void firstAutomaticMatchWinsInConfigurationOrder() {
        ComfortSourceDefinition chairFirst = automatic("chairs", 4.0D);
        ComfortSourceDefinition tableSecond = automatic("tables", 30.0D);

        assertEquals(
                List.of(chairFirst),
                ComfortSourceManager.prioritizeExplicit(List.of(chairFirst, tableSecond)));
    }

    private static ComfortSourceDefinition automatic(String group, double comfort) {
        return new ComfortSourceDefinition(
                NeedsNotNecessities.id("automatic/" + group),
                group,
                group,
                comfort,
                Optional.empty(),
                Optional.empty(),
                Optional.of(ComfortBlockNameFilter.compile(".+", group, Optional.empty())));
    }
}
