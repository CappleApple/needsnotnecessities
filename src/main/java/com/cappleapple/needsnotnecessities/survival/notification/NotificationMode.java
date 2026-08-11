package com.cappleapple.needsnotnecessities.survival.notification;

import java.util.Locale;
import java.util.Optional;

public enum NotificationMode {
    SOUND,
    ACTION_BAR,
    TOAST,
    CHAT,
    NONE;

    public static Optional<NotificationMode> parse(String value) {
        try {
            return Optional.of(valueOf(value.trim().toUpperCase(Locale.ROOT)));
        } catch (IllegalArgumentException ignored) {
            return Optional.empty();
        }
    }
}
