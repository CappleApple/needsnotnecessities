package com.cappleapple.needsnotnecessities.client;

import net.minecraft.ChatFormatting;

public final class ModifierTextColor {
    private ModifierTextColor() {
    }

    public static ChatFormatting forAmount(double amount) {
        return amount < 0.0D ? ChatFormatting.RED : ChatFormatting.GREEN;
    }
}
