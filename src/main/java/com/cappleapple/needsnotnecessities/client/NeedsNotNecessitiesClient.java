package com.cappleapple.needsnotnecessities.client;

import com.cappleapple.needsnotnecessities.NeedsNotNecessities;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.NeoForge;

@Mod(value = NeedsNotNecessities.MOD_ID, dist = Dist.CLIENT)
public final class NeedsNotNecessitiesClient {
    public NeedsNotNecessitiesClient(ModContainer container) {
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
        ClientSurvivalEvents.register(NeoForge.EVENT_BUS);
    }
}
