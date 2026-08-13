package com.cappleapple.needsnotnecessities.client.gui;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.electronwill.nightconfig.core.io.WritingException;
import org.junit.jupiter.api.Test;

class PanelConfigPersistenceTest {
    @Test
    void writingFailureDoesNotEscapeTheMouseEvent() {
        assertDoesNotThrow(() -> PanelConfigPersistence.save(() -> {
            throw new WritingException("Config file is locked");
        }));
    }

    @Test
    void unexpectedRuntimeFailureIsNotHidden() {
        assertThrows(IllegalStateException.class, () -> PanelConfigPersistence.save(() -> {
            throw new IllegalStateException("Unexpected persistence bug");
        }));
    }
}
