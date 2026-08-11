package com.cappleapple.needsnotnecessities.survival.thirst;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class ThirstServiceTest {
    @Test
    void thirstPressureOnlyComesFromConsumedFoodHoursAndConfiguredRatio() {
        assertEquals(5.0D, ThirstService.calculateFoodPressure(20.0D, 0.25D));
        assertEquals(0.0D, ThirstService.calculateFoodPressure(20.0D, 0.0D));
        assertThrows(IllegalArgumentException.class, () -> ThirstService.calculateFoodPressure(-1.0D, 0.25D));
        assertThrows(IllegalArgumentException.class, () -> ThirstService.calculateFoodPressure(1.0D, -0.25D));
    }
}
