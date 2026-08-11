package com.cappleapple.needsnotnecessities.network;

import com.cappleapple.needsnotnecessities.NeedsNotNecessities;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.RegistryFriendlyByteBuf;

public record SurvivalSnapshotPayload(CompoundTag snapshot) implements CustomPacketPayload {
    public static final Type<SurvivalSnapshotPayload> TYPE =
            new Type<>(NeedsNotNecessities.id("survival_snapshot"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SurvivalSnapshotPayload> STREAM_CODEC =
            ByteBufCodecs.COMPOUND_TAG
                    .<RegistryFriendlyByteBuf>cast()
                    .map(SurvivalSnapshotPayload::new, SurvivalSnapshotPayload::snapshot);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
