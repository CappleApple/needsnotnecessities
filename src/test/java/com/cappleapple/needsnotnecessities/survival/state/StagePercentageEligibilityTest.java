package com.cappleapple.needsnotnecessities.survival.state;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

class StagePercentageEligibilityTest {
    @Test
    void percentagesSelectWholeOrderedStagesFromWorst() {
        StateTimeline timeline = timeline();

        assertTrue(StagePercentageEligibility.isBelowConfiguredPercentage(timeline, 1.0D, 50.0D));
        assertTrue(StagePercentageEligibility.isBelowConfiguredPercentage(timeline, 11.0D, 50.0D));
        assertFalse(StagePercentageEligibility.isBelowConfiguredPercentage(timeline, 21.0D, 50.0D));
        assertTrue(StagePercentageEligibility.isBelowConfiguredPercentage(timeline, 31.0D, 90.0D));
        assertFalse(StagePercentageEligibility.isBelowConfiguredPercentage(timeline, 41.0D, 90.0D));
    }

    @Test
    void zeroDisablesAndOneHundredAllowsEveryStage() {
        StateTimeline timeline = timeline();
        assertFalse(StagePercentageEligibility.isBelowConfiguredPercentage(timeline, 1.0D, 0.0D));
        assertTrue(StagePercentageEligibility.isBelowConfiguredPercentage(timeline, 41.0D, 100.0D));
    }

    private static StateTimeline timeline() {
        List<SurvivalStateDefinition> states = java.util.stream.IntStream.range(0, 5)
                .mapToObj(index -> new SurvivalStateDefinition(
                        id("stage_" + index),
                        "Stage " + index,
                        index,
                        10.0D,
                        1.0D,
                        List.of(),
                        List.of(),
                        ""))
                .toList();
        return new StateTimeline(id("timeline"), id("stage_2"), states);
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath("test", path);
    }
}
