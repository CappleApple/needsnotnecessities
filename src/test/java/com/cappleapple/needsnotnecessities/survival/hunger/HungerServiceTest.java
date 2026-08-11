package com.cappleapple.needsnotnecessities.survival.hunger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import net.minecraft.resources.ResourceLocation;
import com.cappleapple.needsnotnecessities.survival.state.StateTimeline;
import com.cappleapple.needsnotnecessities.survival.state.SurvivalStateDefinition;
import org.junit.jupiter.api.Test;

class HungerServiceTest {
    @Test
    void combinesResolvedNutritionAndSaturationAsFloatingPointHours() {
        assertEquals(15.6D, HungerService.calculateFoodHours(6, 7.2F, 2.0D, 0.5D), 1.0E-6D);
    }

    @Test
    void biologicalDecayStopsAtWorstState() {
        ResourceLocation timelineId = ResourceLocation.fromNamespaceAndPath("test", "hunger");
        ResourceLocation stateId = ResourceLocation.fromNamespaceAndPath("test", "hunger/neutral");
        StateTimeline timeline = new StateTimeline(
                timelineId,
                stateId,
                List.of(new SurvivalStateDefinition(stateId, "Neutral", 0, 8.0D, 1.0D, List.of(), List.of(), "")));
        assertEquals(3.5D, HungerService.decayPosition(timeline, 5.0D, 1.5D));
        assertEquals(0.0D, HungerService.decayPosition(timeline, 1.0D, 20.0D));
    }

    @Test
    void rejectsInvalidFoodConversions() {
        assertThrows(IllegalArgumentException.class,
                () -> HungerService.calculateFoodHours(-1, 0.0F, 2.0D, 0.5D));
    }

    @Test
    void hungerStatusEffectDoublesConfiguredTimerDrain() {
        assertEquals(1.0D, HungerService.decayMultiplier(false, 2.0D));
        assertEquals(2.0D, HungerService.decayMultiplier(true, 2.0D));
        assertThrows(IllegalArgumentException.class, () -> HungerService.decayMultiplier(true, -1.0D));
    }
}
