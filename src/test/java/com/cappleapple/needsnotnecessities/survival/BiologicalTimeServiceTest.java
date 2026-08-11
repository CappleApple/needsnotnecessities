package com.cappleapple.needsnotnecessities.survival;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class BiologicalTimeServiceTest {
    @Test
    void twentyMinuteDayMatchesTwentyFourThousandTicks() {
        assertEquals(24.0D, BiologicalTimeService.ticksToBiologicalHours(24_000.0D, 20.0D));
        assertEquals(1_000.0D, BiologicalTimeService.biologicalHoursToTicks(1.0D, 20.0D));
        assertEquals(24_000.0D, BiologicalTimeService.biologicalDaysToTicks(1.0D, 20.0D));
    }

    @Test
    void floatingPointDayLengthsRoundTrip() {
        double ticks = BiologicalTimeService.biologicalHoursToTicks(7.25D, 30.5D);
        assertEquals(7.25D, BiologicalTimeService.ticksToBiologicalHours(ticks, 30.5D), 1.0E-10D);
    }

    @Test
    void invalidInputsAreRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> BiologicalTimeService.ticksToBiologicalHours(-1.0D, 20.0D));
        assertThrows(IllegalArgumentException.class,
                () -> BiologicalTimeService.biologicalHoursToTicks(1.0D, 0.0D));
        assertThrows(IllegalArgumentException.class,
                () -> BiologicalTimeService.biologicalDaysToTicks(Double.NaN, 20.0D));
    }
}
