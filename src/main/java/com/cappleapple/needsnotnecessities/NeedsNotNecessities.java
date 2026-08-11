package com.cappleapple.needsnotnecessities;

import com.cappleapple.needsnotnecessities.config.ConfigManager;
import com.cappleapple.needsnotnecessities.data.ModAttachments;
import com.cappleapple.needsnotnecessities.survival.SurvivalEvents;
import com.cappleapple.needsnotnecessities.network.SurvivalNetwork;
import com.cappleapple.needsnotnecessities.registry.ModSounds;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;

@Mod(NeedsNotNecessities.MOD_ID)
public final class NeedsNotNecessities {
    public static final String MOD_ID = "needs_not_necessities";
    public static final Logger LOGGER = LogUtils.getLogger();

    public NeedsNotNecessities(IEventBus modBus, ModContainer container) {
        ModAttachments.register(modBus);
        ModSounds.register(modBus);
        ConfigManager.register(container, modBus);
        SurvivalNetwork.register(modBus);
        SurvivalEvents.register(NeoForge.EVENT_BUS);
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }
}
