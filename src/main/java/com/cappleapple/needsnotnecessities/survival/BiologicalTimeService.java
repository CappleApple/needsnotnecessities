package com.cappleapple.needsnotnecessities.survival;

import com.cappleapple.needsnotnecessities.config.ServerConfig;

public final class BiologicalTimeService {
    public static final double TICKS_PER_SECOND = 20.0D;
    public static final double HOURS_PER_DAY = 24.0D;
    public static final BiologicalTimeService INSTANCE = new BiologicalTimeService();

    private BiologicalTimeService() {
    }

    public double ticksToBiologicalHours(double ticks) {
        return ticksToBiologicalHours(ticks, configuredDayLengthMinutes());
    }

    public double biologicalHoursToTicks(double hours) {
        return biologicalHoursToTicks(hours, configuredDayLengthMinutes());
    }

    public double biologicalDaysToTicks(double days) {
        return biologicalDaysToTicks(days, configuredDayLengthMinutes());
    }

    public long wholeTicksForBiologicalHours(double hours) {
        return Math.max(0L, Math.round(biologicalHoursToTicks(hours)));
    }

    public static double ticksToBiologicalHours(double ticks, double dayLengthMinutes) {
        validateNonNegative(ticks, "ticks");
        validateDayLength(dayLengthMinutes);
        return ticks / ticksPerBiologicalDay(dayLengthMinutes) * HOURS_PER_DAY;
    }

    public static double biologicalHoursToTicks(double hours, double dayLengthMinutes) {
        validateNonNegative(hours, "biological hours");
        validateDayLength(dayLengthMinutes);
        return hours / HOURS_PER_DAY * ticksPerBiologicalDay(dayLengthMinutes);
    }

    public static double biologicalDaysToTicks(double days, double dayLengthMinutes) {
        validateNonNegative(days, "biological days");
        validateDayLength(dayLengthMinutes);
        return days * ticksPerBiologicalDay(dayLengthMinutes);
    }

    public static double ticksPerBiologicalDay(double dayLengthMinutes) {
        validateDayLength(dayLengthMinutes);
        return dayLengthMinutes * 60.0D * TICKS_PER_SECOND;
    }

    private static double configuredDayLengthMinutes() {
        return ServerConfig.INSTANCE.dayLengthMinutes.getAsDouble();
    }

    private static void validateDayLength(double dayLengthMinutes) {
        if (!Double.isFinite(dayLengthMinutes) || dayLengthMinutes <= 0.0D) {
            throw new IllegalArgumentException("Biological day length must be finite and greater than zero");
        }
    }

    private static void validateNonNegative(double value, String label) {
        if (!Double.isFinite(value) || value < 0.0D) {
            throw new IllegalArgumentException(label + " must be finite and non-negative");
        }
    }
}
