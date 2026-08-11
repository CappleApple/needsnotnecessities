package com.cappleapple.needsnotnecessities.client;

import static org.junit.jupiter.api.Assertions.assertEquals;

import net.minecraft.ChatFormatting;
import org.junit.jupiter.api.Test;

class ModifierTextColorTest {
    @Test
    void negativeAmountsAreRedAndNonNegativeAmountsRemainGreen() {
        assertEquals(ChatFormatting.RED, ModifierTextColor.forAmount(-0.01D));
        assertEquals(ChatFormatting.GREEN, ModifierTextColor.forAmount(0.0D));
        assertEquals(ChatFormatting.GREEN, ModifierTextColor.forAmount(1.0D));
    }
}
