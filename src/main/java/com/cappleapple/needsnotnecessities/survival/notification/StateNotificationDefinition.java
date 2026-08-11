package com.cappleapple.needsnotnecessities.survival.notification;

import java.util.Objects;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;

public record StateNotificationDefinition(
        NotificationMode type,
        Optional<ResourceLocation> sound,
        String message,
        String title,
        float volume,
        float pitch) {

    public StateNotificationDefinition {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(sound, "sound");
        message = message == null ? "" : message;
        title = title == null ? "" : title;
        if (!Float.isFinite(volume) || volume < 0.0F || !Float.isFinite(pitch) || pitch <= 0.0F) {
            throw new IllegalArgumentException("Notification volume and pitch must be finite and valid");
        }
    }
}
