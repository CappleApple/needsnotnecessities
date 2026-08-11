package com.cappleapple.needsnotnecessities.registry;

import com.cappleapple.needsnotnecessities.NeedsNotNecessities;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModSounds {
    private static final DeferredRegister<SoundEvent> SOUNDS =
            DeferredRegister.create(Registries.SOUND_EVENT, NeedsNotNecessities.MOD_ID);

    public static final DeferredHolder<SoundEvent, SoundEvent> HUNGER_LOW = register("notification.hunger_low");
    public static final DeferredHolder<SoundEvent, SoundEvent> THIRST_LOW = register("notification.thirst_low");
    public static final DeferredHolder<SoundEvent, SoundEvent> REST_LOW = register("notification.rest_low");

    private ModSounds() {
    }

    public static void register(IEventBus modBus) {
        SOUNDS.register(modBus);
    }

    private static DeferredHolder<SoundEvent, SoundEvent> register(String path) {
        return SOUNDS.register(path, () -> SoundEvent.createVariableRangeEvent(NeedsNotNecessities.id(path)));
    }
}
