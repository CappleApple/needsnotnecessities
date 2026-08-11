package com.cappleapple.needsnotnecessities.data;

import com.cappleapple.needsnotnecessities.NeedsNotNecessities;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

public final class ModTags {
    private ModTags() {
    }

    public static final class Items {
        public static final TagKey<Item> DRINKS = TagKey.create(Registries.ITEM, NeedsNotNecessities.id("drinks"));
        public static final TagKey<Item> ALCOHOLIC_DRINKS = TagKey.create(
                Registries.ITEM, NeedsNotNecessities.id("alcoholic_drinks"));

        private Items() {
        }
    }
}
