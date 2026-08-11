package com.cappleapple.needsnotnecessities.survival.notification;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class NotificationModeTest {
    @Test
    void modeParsingIsCaseAndWhitespaceInsensitive() {
        assertEquals(NotificationMode.ACTION_BAR, NotificationMode.parse(" action_bar ").orElseThrow());
        assertEquals(NotificationMode.SOUND, NotificationMode.parse("sound").orElseThrow());
        assertTrue(NotificationMode.parse("not_a_mode").isEmpty());
    }
}
