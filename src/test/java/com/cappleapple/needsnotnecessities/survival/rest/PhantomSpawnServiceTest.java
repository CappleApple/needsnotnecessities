package com.cappleapple.needsnotnecessities.survival.rest;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.cappleapple.needsnotnecessities.survival.state.StateTimeline;
import com.cappleapple.needsnotnecessities.survival.state.SurvivalStateDefinition;
import java.util.List;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

class PhantomSpawnServiceTest {
    @Test
    void entireLowestConfiguredStageQualifiesUntilTheNextStageStarts() {
        SurvivalStateDefinition lowest = state("drained", -10, 3.5D);
        SurvivalStateDefinition neutral = state("okay", 20, 8.0D);
        SurvivalStateDefinition best = state("energized", 50, 2.0D);
        StateTimeline timeline = new StateTimeline(
                ResourceLocation.fromNamespaceAndPath("custom", "rest"), neutral.id(),
                List.of(best, neutral, lowest));

        assertTrue(PhantomSpawnService.isLowestRestState(timeline, 0.0D));
        assertTrue(PhantomSpawnService.isLowestRestState(timeline, 2.0D));
        assertTrue(PhantomSpawnService.isLowestRestState(timeline, Math.nextDown(3.5D)));
        assertFalse(PhantomSpawnService.isLowestRestState(timeline, 3.5D));
        assertFalse(PhantomSpawnService.isLowestRestState(timeline, timeline.neutralPosition()));
        assertFalse(PhantomSpawnService.isLowestRestState(timeline, timeline.bestPosition()));
    }

    @Test
    void singleStageTimelineIsAlwaysItsLowestStage() {
        SurvivalStateDefinition only = state("only", 12, 2.0D);
        StateTimeline timeline = new StateTimeline(
                ResourceLocation.fromNamespaceAndPath("custom", "rest"), only.id(), List.of(only));

        assertTrue(PhantomSpawnService.isLowestRestState(timeline, timeline.bestPosition()));
    }

    private static SurvivalStateDefinition state(String name, int order, double hours) {
        return new SurvivalStateDefinition(ResourceLocation.fromNamespaceAndPath("custom", name),
                name, order, hours, 1.0D, List.of(), List.of(), "");
    }
}
