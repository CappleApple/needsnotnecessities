package com.cappleapple.needsnotnecessities.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ConfigDefaultsTest {
    @Test
    void requestedConsumptionMealAndSleepDefaultsAreStable() {
        assertEquals(90.0D, ServerConfig.INSTANCE.hungerEatBelowStagePercentage.getDefault());
        assertEquals(90.0D, ServerConfig.INSTANCE.thirstDrinkBelowStagePercentage.getDefault());
        assertEquals(5, ServerConfig.INSTANCE.mealMaximumBonuses.getDefault());
        assertEquals(0.5D, ServerConfig.INSTANCE.mealSameGroupDiminishingFactor.getDefault());
        assertTrue(ServerConfig.INSTANCE.allowDaytimeSleep.getDefault());
        assertTrue(ServerConfig.INSTANCE.requireTiredToSleep.getDefault());
        assertEquals(50.0D, ServerConfig.INSTANCE.sleepBelowStagePercentage.getDefault());
        assertTrue(ServerConfig.INSTANCE.daytimeSleepSkipsToNight.getDefault());
        assertEquals("minecraft:item/carrot", ClientConfig.INSTANCE.panelIconSprite.getDefault());
    }
}
