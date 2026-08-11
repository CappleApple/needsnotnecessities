package com.cappleapple.needsnotnecessities.client;

import com.cappleapple.needsnotnecessities.network.SurvivalNotificationPayload;
import com.cappleapple.needsnotnecessities.network.SurvivalSnapshotPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public final class ClientPayloadHandler {
    private static final SystemToast.SystemToastId SURVIVAL_TOAST = new SystemToast.SystemToastId();

    private ClientPayloadHandler() {
    }

    public static void handleSnapshot(SurvivalSnapshotPayload payload, IPayloadContext context) {
        ClientSurvivalCache.update(payload.snapshot());
    }

    public static void handleNotification(SurvivalNotificationPayload payload, IPayloadContext context) {
        Minecraft minecraft = Minecraft.getInstance();
        SystemToast.addOrUpdate(
                minecraft.getToasts(),
                SURVIVAL_TOAST,
                Component.literal(payload.title()),
                Component.literal(payload.message()));
    }
}
