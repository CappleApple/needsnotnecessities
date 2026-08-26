package com.cappleapple.needsnotnecessities.network;

import com.cappleapple.needsnotnecessities.NeedsNotNecessities;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record SilentHealthAdjustmentPayload(float reduction, float targetHealth) implements CustomPacketPayload {
    public static final Type<SilentHealthAdjustmentPayload> TYPE =
            new Type<>(NeedsNotNecessities.id("silent_health_adjustment"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SilentHealthAdjustmentPayload> STREAM_CODEC =
            ByteBufCodecs.COMPOUND_TAG
                    .<RegistryFriendlyByteBuf>cast()
                    .map(SilentHealthAdjustmentPayload::decode, SilentHealthAdjustmentPayload::encode);

    private static SilentHealthAdjustmentPayload decode(CompoundTag tag) {
        return new SilentHealthAdjustmentPayload(tag.getFloat("reduction"), tag.getFloat("target_health"));
    }

    private static CompoundTag encode(SilentHealthAdjustmentPayload payload) {
        CompoundTag tag = new CompoundTag();
        tag.putFloat("reduction", payload.reduction());
        tag.putFloat("target_health", payload.targetHealth());
        return tag;
    }

    public static float adjustedClientHealth(float currentHealth, float reduction, float targetHealth) {
        if (!Float.isFinite(currentHealth)
                || !Float.isFinite(reduction)
                || !Float.isFinite(targetHealth)
                || reduction <= 0.0F
                || currentHealth <= targetHealth) {
            return currentHealth;
        }
        return Math.max(targetHealth, currentHealth - reduction);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
