package com.cappleapple.needsnotnecessities.survival.state;

import com.cappleapple.needsnotnecessities.modifier.SurvivalModifier;
import java.util.List;
import java.util.Objects;
import com.cappleapple.needsnotnecessities.survival.notification.StateNotificationDefinition;
import net.minecraft.resources.ResourceLocation;

public record SurvivalStateDefinition(
        ResourceLocation id,
        String displayName,
        int order,
        double durationHours,
        double passiveRegenerationMultiplier,
        List<SurvivalModifier> modifiers,
        List<StateNotificationDefinition> notifications,
        String description) {

    public SurvivalStateDefinition {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(displayName, "displayName");
        Objects.requireNonNull(modifiers, "modifiers");
        Objects.requireNonNull(notifications, "notifications");
        if (displayName.isBlank()) {
            throw new IllegalArgumentException("State display name cannot be blank: " + id);
        }
        if (!Double.isFinite(durationHours) || durationHours <= 0.0D) {
            throw new IllegalArgumentException("State duration must be finite and greater than zero: " + id);
        }
        if (!Double.isFinite(passiveRegenerationMultiplier) || passiveRegenerationMultiplier < 0.0D) {
            throw new IllegalArgumentException("Passive regeneration multiplier must be finite and non-negative: " + id);
        }
        modifiers = List.copyOf(modifiers);
        notifications = List.copyOf(notifications);
        description = description == null ? "" : description;
    }
}
