package com.cappleapple.needsnotnecessities.survival.state;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

class StateTimelineTest {
    private static final ResourceLocation TIMELINE_ID = ResourceLocation.fromNamespaceAndPath("test", "need");
    private static final ResourceLocation LOW = ResourceLocation.fromNamespaceAndPath("test", "need/low");
    private static final ResourceLocation NEUTRAL = ResourceLocation.fromNamespaceAndPath("test", "need/neutral");
    private static final ResourceLocation HIGH = ResourceLocation.fromNamespaceAndPath("test", "need/high");

    @Test
    void resolvesOrderedRangesWithoutHardcodedTierCount() {
        StateTimeline timeline = timeline();

        assertEquals(14.0D, timeline.totalHours());
        assertEquals(LOW, timeline.stateAt(0.0D).id());
        assertEquals(LOW, timeline.stateAt(3.999D).id());
        assertEquals(NEUTRAL, timeline.stateAt(4.0D).id());
        assertEquals(HIGH, timeline.stateAt(12.0D).id());
        assertEquals(HIGH, timeline.stateAt(999.0D).id());
        assertEquals(8.0D, timeline.neutralPosition());
    }

    @Test
    void progressClampsAndFillsAcrossStateBoundaries() {
        StateTimeline timeline = timeline();

        assertEquals(13.0D, timeline.add(5.0D, 8.0D));
        assertEquals(14.0D, timeline.add(13.0D, 20.0D));
        assertEquals(14.0D, timeline.add(13.0D, Double.MAX_VALUE));
        assertEquals(0.0D, timeline.add(1.0D, -20.0D));
    }

    @Test
    void levelMovementPreservesFractionAcrossDifferentDurations() {
        StateTimeline timeline = timeline();
        assertEquals(8.0D, timeline.moveLevels(2.0D, 1.0D));
        assertEquals(13.0D, timeline.moveLevels(8.0D, 1.0D));
        assertEquals(0.0D, timeline.moveLevels(2.0D, -2.0D));
        assertEquals(14.0D, timeline.moveLevels(13.0D, 10.0D));
    }

    @Test
    void rejectsDuplicateOrderingAndMissingNeutralState() {
        SurvivalStateDefinition duplicateOrder = state(HIGH, 1, 2.0D);
        assertThrows(IllegalArgumentException.class,
                () -> new StateTimeline(TIMELINE_ID, NEUTRAL, List.of(state(LOW, 1, 4.0D), duplicateOrder)));
        assertThrows(IllegalArgumentException.class,
                () -> new StateTimeline(TIMELINE_ID, NEUTRAL, List.of(state(LOW, 0, 4.0D))));
    }

    private static StateTimeline timeline() {
        return new StateTimeline(
                TIMELINE_ID,
                NEUTRAL,
                List.of(
                        state(HIGH, 2, 2.0D),
                        state(LOW, 0, 4.0D),
                        state(NEUTRAL, 1, 8.0D)));
    }

    private static SurvivalStateDefinition state(ResourceLocation id, int order, double duration) {
        return new SurvivalStateDefinition(id, id.getPath(), order, duration, 1.0D, List.of(), List.of(), "");
    }
}
