package com.cappleapple.needsnotnecessities.client;

import com.cappleapple.needsnotnecessities.survival.SurvivalModule;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;

public final class ClientSurvivalCache {
    private static CompoundTag snapshot;

    private ClientSurvivalCache() {
    }

    public static void update(CompoundTag replacement) {
        snapshot = replacement.copy();
    }

    public static void clear() {
        snapshot = null;
    }

    public static boolean available() {
        return snapshot != null;
    }

    public static CompoundTag snapshot() {
        return snapshot == null ? new CompoundTag() : snapshot.copy();
    }

    public static boolean enabled(SurvivalModule module) {
        return snapshot != null
                && snapshot.getCompound("enabled").getBoolean(module.configKey());
    }

    public static double number(String key, double fallback) {
        return snapshot != null && snapshot.contains(key, Tag.TAG_ANY_NUMERIC)
                ? snapshot.getDouble(key)
                : fallback;
    }

    public static List<CompoundTag> list(String key) {
        List<CompoundTag> result = new ArrayList<>();
        if (snapshot == null) {
            return result;
        }
        for (Tag element : snapshot.getList(key, Tag.TAG_COMPOUND)) {
            result.add(((CompoundTag) element).copy());
        }
        return List.copyOf(result);
    }
}
