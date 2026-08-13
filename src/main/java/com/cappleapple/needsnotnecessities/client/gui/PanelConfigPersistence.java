package com.cappleapple.needsnotnecessities.client.gui;

import com.cappleapple.needsnotnecessities.NeedsNotNecessities;
import com.electronwill.nightconfig.core.io.WritingException;

final class PanelConfigPersistence {
    private PanelConfigPersistence() {
    }

    static void save(Runnable saveOperation) {
        try {
            saveOperation.run();
        } catch (WritingException exception) {
            NeedsNotNecessities.LOGGER.error(
                    "Could not persist the inventory panel state. The panel remains usable, but its new position may not survive a restart.",
                    exception);
        }
    }
}
