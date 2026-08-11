package com.cappleapple.needsnotnecessities.survival.rest;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class SleepRulesServiceTest {
    @Test
    void daytimeSleepCanTargetNightOrTheNextDay() {
        assertEquals(13_000L, SleepRulesService.targetWakeTime(6_000L, true, true));
        assertEquals(24_000L, SleepRulesService.targetWakeTime(6_000L, true, false));
    }

    @Test
    void nighttimeSleepAlwaysTargetsTheNextDay() {
        assertEquals(24_000L, SleepRulesService.targetWakeTime(18_000L, false, true));
        assertEquals(48_000L, SleepRulesService.targetWakeTime(42_000L, false, false));
    }
}
