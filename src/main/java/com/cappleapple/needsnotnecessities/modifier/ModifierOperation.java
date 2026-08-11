package com.cappleapple.needsnotnecessities.modifier;

import java.util.Locale;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

public enum ModifierOperation {
    ADD,
    MULTIPLY_BASE,
    MULTIPLY_TOTAL;

    public AttributeModifier.Operation toMinecraft() {
        return switch (this) {
            case ADD -> AttributeModifier.Operation.ADD_VALUE;
            case MULTIPLY_BASE -> AttributeModifier.Operation.ADD_MULTIPLIED_BASE;
            case MULTIPLY_TOTAL -> AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL;
        };
    }

    public static ModifierOperation parse(String value) {
        return switch (value.toUpperCase(Locale.ROOT)) {
            case "ADD", "ADDITIVE", "ADD_VALUE" -> ADD;
            case "MULTIPLY_BASE", "ADD_MULTIPLIED_BASE" -> MULTIPLY_BASE;
            case "MULTIPLY_TOTAL", "ADD_MULTIPLIED_TOTAL" -> MULTIPLY_TOTAL;
            default -> throw new IllegalArgumentException("Unknown modifier operation: " + value);
        };
    }
}
