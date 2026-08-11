package com.cappleapple.needsnotnecessities.network;

import com.cappleapple.needsnotnecessities.NeedsNotNecessities;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record SurvivalNotificationPayload(String title, String message) implements CustomPacketPayload {
    public static final Type<SurvivalNotificationPayload> TYPE =
            new Type<>(NeedsNotNecessities.id("survival_notification"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SurvivalNotificationPayload> STREAM_CODEC =
            ByteBufCodecs.COMPOUND_TAG
                    .<RegistryFriendlyByteBuf>cast()
                    .map(SurvivalNotificationPayload::decode, SurvivalNotificationPayload::encode);

    private static SurvivalNotificationPayload decode(CompoundTag tag) {
        return new SurvivalNotificationPayload(tag.getString("title"), tag.getString("message"));
    }

    private static CompoundTag encode(SurvivalNotificationPayload payload) {
        CompoundTag tag = new CompoundTag();
        tag.putString("title", payload.title());
        tag.putString("message", payload.message());
        return tag;
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
