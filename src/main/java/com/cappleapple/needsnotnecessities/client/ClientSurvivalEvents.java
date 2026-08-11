package com.cappleapple.needsnotnecessities.client;

import com.cappleapple.needsnotnecessities.config.ServerConfig;
import com.cappleapple.needsnotnecessities.survival.SurvivalModule;
import com.cappleapple.needsnotnecessities.client.gui.SurvivalOverlayPanel;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

public final class ClientSurvivalEvents {
    private ClientSurvivalEvents() {
    }

    public static void register(IEventBus gameBus) {
        gameBus.addListener(ClientSurvivalEvents::onRenderGuiLayer);
        gameBus.addListener(ClientSurvivalEvents::onClientLogout);
        gameBus.addListener(ClientSurvivalEvents::onItemTooltip);
        gameBus.addListener(ClientSurvivalEvents::onScreenInit);
        gameBus.addListener(ClientSurvivalEvents::onScreenRender);
        gameBus.addListener(ClientSurvivalEvents::onMousePressed);
        gameBus.addListener(ClientSurvivalEvents::onMouseDragged);
        gameBus.addListener(ClientSurvivalEvents::onMouseReleased);
        gameBus.addListener(ClientSurvivalEvents::onMouseScrolled);
    }

    private static void onRenderGuiLayer(RenderGuiLayerEvent.Pre event) {
        if (VanillaGuiLayers.FOOD_LEVEL.equals(event.getName())
                && ServerConfig.INSTANCE.isEnabled(SurvivalModule.HUNGER)) {
            event.setCanceled(true);
        }
    }

    private static void onClientLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        ClientSurvivalCache.clear();
    }

    private static void onItemTooltip(ItemTooltipEvent event) {
        ClientTooltipService.addTooltips(event);
    }

    private static void onScreenInit(ScreenEvent.Init.Post event) {
        SurvivalOverlayPanel.onScreenInit(event);
    }

    private static void onScreenRender(ScreenEvent.Render.Post event) {
        SurvivalOverlayPanel.onRender(event);
    }

    private static void onMousePressed(ScreenEvent.MouseButtonPressed.Pre event) {
        SurvivalOverlayPanel.onMousePressed(event);
    }

    private static void onMouseDragged(ScreenEvent.MouseDragged.Pre event) {
        SurvivalOverlayPanel.onMouseDragged(event);
    }

    private static void onMouseReleased(ScreenEvent.MouseButtonReleased.Pre event) {
        SurvivalOverlayPanel.onMouseReleased(event);
    }

    private static void onMouseScrolled(ScreenEvent.MouseScrolled.Pre event) {
        SurvivalOverlayPanel.onMouseScrolled(event);
    }
}
