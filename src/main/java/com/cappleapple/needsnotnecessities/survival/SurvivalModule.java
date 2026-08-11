package com.cappleapple.needsnotnecessities.survival;

import java.util.Locale;

public enum SurvivalModule {
    HUNGER,
    THIRST,
    REST,
    COMFORT,
    ACTIVE_MEAL,
    PASSIVE_REGENERATION,
    BASE_HEALTH,
    NOTIFICATIONS,
    COMPATIBILITY;

    public String configKey() {
        return name().toLowerCase(Locale.ROOT);
    }
}
