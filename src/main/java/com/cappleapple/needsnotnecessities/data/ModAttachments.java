package com.cappleapple.needsnotnecessities.data;

import com.cappleapple.needsnotnecessities.NeedsNotNecessities;
import java.util.function.Supplier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public final class ModAttachments {
    private static final DeferredRegister<AttachmentType<?>> TYPES =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, NeedsNotNecessities.MOD_ID);

    public static final Supplier<AttachmentType<PlayerSurvivalData>> PLAYER_SURVIVAL = TYPES.register(
            "player_survival",
            () -> AttachmentType.serializable(PlayerSurvivalData::new).build());

    private ModAttachments() {
    }

    public static void register(IEventBus modBus) {
        TYPES.register(modBus);
    }
}
