package com.cappleapple.needsnotnecessities.survival.comfort;

import java.util.Objects;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public record ComfortSourceDefinition(
        ResourceLocation id,
        String type,
        String displayName,
        double comfort,
        Optional<Block> block,
        Optional<TagKey<Block>> tag,
        Optional<ComfortBlockNameFilter> autoMatch) {

    public ComfortSourceDefinition {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(displayName, "displayName");
        Objects.requireNonNull(block, "block");
        Objects.requireNonNull(tag, "tag");
        Objects.requireNonNull(autoMatch, "autoMatch");
        int selectorCount = (block.isPresent() ? 1 : 0)
                + (tag.isPresent() ? 1 : 0)
                + (autoMatch.isPresent() ? 1 : 0);
        if (type.isBlank() || displayName.isBlank() || !Double.isFinite(comfort) || comfort <= 0.0D
                || selectorCount != 1) {
            throw new IllegalArgumentException(
                    "Comfort source requires a type, display name, positive comfort, and exactly one selector: " + id);
        }
    }

    public boolean matches(BlockState state, ResourceLocation blockId) {
        return block.map(value -> state.is(value)).orElse(false)
                || tag.map(state::is).orElse(false)
                || autoMatch.map(filter -> filter.matches(blockId)).orElse(false);
    }
}
