package com.cappleapple.needsnotnecessities.survival.meal;

import static org.junit.jupiter.api.Assertions.assertEquals;

import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

class MealGroupDiminishingTrackerTest {
    private static final ResourceLocation VEGETABLES = id("vegetables");
    private static final ResourceLocation MEAT = id("meat");

    @Test
    void repeatedGroupUsesGeometricDiminishingReturns() {
        MealGroupDiminishingTracker tracker = new MealGroupDiminishingTracker();

        assertEquals(1.0D, tracker.nextMultiplier(VEGETABLES, 0.5D), 1.0E-9D);
        assertEquals(0.5D, tracker.nextMultiplier(VEGETABLES, 0.5D), 1.0E-9D);
        assertEquals(0.25D, tracker.nextMultiplier(VEGETABLES, 0.5D), 1.0E-9D);
        assertEquals(0.125D, tracker.nextMultiplier(VEGETABLES, 0.5D), 1.0E-9D);
    }

    @Test
    void distinctGroupsBeginAtFullStrengthIndependently() {
        MealGroupDiminishingTracker tracker = new MealGroupDiminishingTracker();

        assertEquals(1.0D, tracker.nextMultiplier(VEGETABLES, 0.5D), 1.0E-9D);
        assertEquals(1.0D, tracker.nextMultiplier(MEAT, 0.5D), 1.0E-9D);
        assertEquals(0.5D, tracker.nextMultiplier(VEGETABLES, 0.5D), 1.0E-9D);
        assertEquals(0.5D, tracker.nextMultiplier(MEAT, 0.5D), 1.0E-9D);
    }

    @Test
    void configuredFactorControlsSuccessiveContributions() {
        MealGroupDiminishingTracker tracker = new MealGroupDiminishingTracker();

        assertEquals(1.0D, tracker.nextMultiplier(VEGETABLES, 0.75D), 1.0E-9D);
        assertEquals(0.75D, tracker.nextMultiplier(VEGETABLES, 0.75D), 1.0E-9D);
        assertEquals(0.5625D, tracker.nextMultiplier(VEGETABLES, 0.75D), 1.0E-9D);
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath("needs_not_necessities", path);
    }
}
