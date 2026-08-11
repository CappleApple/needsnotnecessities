package com.cappleapple.needsnotnecessities.config;

import com.cappleapple.needsnotnecessities.NeedsNotNecessities;
import java.util.concurrent.atomic.AtomicLong;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;

public final class ConfigManager {
    private static final AtomicLong GENERATION = new AtomicLong();

    private ConfigManager() {
    }

    public static void register(ModContainer container, IEventBus modBus) {
        container.registerConfig(ModConfig.Type.SERVER, ServerConfig.SPEC, "needs_not_necessities-server.toml");
        container.registerConfig(ModConfig.Type.CLIENT, ClientConfig.SPEC, "needs_not_necessities-client.toml");
        modBus.addListener(ConfigManager::onConfigLoaded);
        modBus.addListener(ConfigManager::onConfigReloaded);
    }

    public static long generation() {
        return GENERATION.get();
    }

    private static void onConfigLoaded(ModConfigEvent.Loading event) {
        markChanged(event.getConfig());
    }

    private static void onConfigReloaded(ModConfigEvent.Reloading event) {
        markChanged(event.getConfig());
    }

    private static void markChanged(ModConfig config) {
        if (NeedsNotNecessities.MOD_ID.equals(config.getModId())) {
            GENERATION.incrementAndGet();
        }
    }
}
