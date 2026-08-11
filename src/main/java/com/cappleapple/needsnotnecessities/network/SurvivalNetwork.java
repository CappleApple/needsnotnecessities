package com.cappleapple.needsnotnecessities.network;

import com.cappleapple.needsnotnecessities.client.ClientPayloadHandler;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

public final class SurvivalNetwork {
    private static final String PROTOCOL_VERSION = "1";

    private SurvivalNetwork() {
    }

    public static void register(IEventBus modBus) {
        modBus.addListener(SurvivalNetwork::registerPayloads);
    }

    private static void registerPayloads(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar(PROTOCOL_VERSION);
        registrar.playToClient(
                SurvivalSnapshotPayload.TYPE,
                SurvivalSnapshotPayload.STREAM_CODEC,
                ClientPayloadHandler::handleSnapshot);
        registrar.playToClient(
                SurvivalNotificationPayload.TYPE,
                SurvivalNotificationPayload.STREAM_CODEC,
                ClientPayloadHandler::handleNotification);
    }
}
