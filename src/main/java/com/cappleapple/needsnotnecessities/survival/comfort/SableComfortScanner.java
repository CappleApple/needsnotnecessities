package com.cappleapple.needsnotnecessities.survival.comfort;

import dev.ryanhcode.sable.companion.SableCompanion;
import dev.ryanhcode.sable.companion.SubLevelAccess;
import dev.ryanhcode.sable.companion.math.BoundingBox3d;
import dev.ryanhcode.sable.companion.math.BoundingBox3dc;
import dev.ryanhcode.sable.companion.math.Pose3dc;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiConsumer;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

final class SableComfortScanner {
    private SableComfortScanner() {
    }

    static void forEachNearbyBlock(
            ServerPlayer player,
            int radius,
            BiConsumer<BlockState, BlockPos> consumer) {
        ServerLevel level = player.serverLevel();
        forEachNearbyBlock(
                level,
                player.blockPosition(),
                radius,
                SableCompanion.INSTANCE.getTrackingOrVehicleSubLevel(player),
                consumer);
    }

    static void forEachNearbyBlock(
            ServerLevel level,
            BlockPos center,
            int radius,
            SubLevelAccess trackedSubLevel,
            BiConsumer<BlockState, BlockPos> consumer) {
        BlockPos minimum = center.offset(-radius, -radius, -radius);
        BlockPos maximum = center.offset(radius, radius, radius);

        visitVanillaBlocks(level, minimum, maximum, consumer);

        AABB scanBounds = new AABB(
                minimum.getX(),
                minimum.getY(),
                minimum.getZ(),
                maximum.getX() + 1.0D,
                maximum.getY() + 1.0D,
                maximum.getZ() + 1.0D);
        Iterable<? extends SubLevelAccess> intersectingSubLevels = SableCompanion.INSTANCE.getAllIntersecting(
                level,
                new BoundingBox3d(scanBounds));
        for (SubLevelAccess subLevel : subLevelsToScan(trackedSubLevel, intersectingSubLevels)) {
            visitSubLevelBlocks(level, scanBounds, subLevel, consumer);
        }
    }

    private static void visitVanillaBlocks(
            ServerLevel level,
            BlockPos minimum,
            BlockPos maximum,
            BiConsumer<BlockState, BlockPos> consumer) {
        for (BlockPos cursor : BlockPos.betweenClosed(minimum, maximum)) {
            BlockState state = level.getBlockState(cursor);
            if (!state.isAir()) {
                consumer.accept(state, cursor.immutable());
            }
        }
    }

    private static void visitSubLevelBlocks(
            ServerLevel level,
            AABB scanBounds,
            SubLevelAccess subLevel,
            BiConsumer<BlockState, BlockPos> consumer) {
        Pose3dc pose = subLevel.logicalPose();
        LocalScanBounds bounds = candidateBounds(scanBounds, subLevel.boundingBox(), pose);
        Map<Long, LevelChunk> loadedChunks = new HashMap<>();
        Set<Long> missingChunks = new HashSet<>();
        for (BlockPos cursor : BlockPos.betweenClosed(bounds.minimum(), bounds.maximum())) {
            int chunkX = cursor.getX() >> 4;
            int chunkZ = cursor.getZ() >> 4;
            long chunkKey = ChunkPos.asLong(chunkX, chunkZ);
            if (missingChunks.contains(chunkKey)) {
                continue;
            }
            LevelChunk chunk = loadedChunks.get(chunkKey);
            if (chunk == null) {
                // Sable hooks getChunkNow for plot chunks in both its 1.2.x and 2.x lines.
                chunk = level.getChunkSource().getChunkNow(chunkX, chunkZ);
                if (chunk == null) {
                    missingChunks.add(chunkKey);
                    continue;
                }
                loadedChunks.put(chunkKey, chunk);
            }
            BlockState state = chunk.getBlockState(cursor);
            if (state.isAir()) {
                continue;
            }
            Vec3 globalCenter = pose.transformPosition(cursor.getCenter());
            if (scanBounds.contains(globalCenter)) {
                consumer.accept(state, BlockPos.containing(globalCenter));
            }
        }
    }

    static LocalScanBounds candidateBounds(
            AABB scanBounds,
            BoundingBox3dc subLevelBounds,
            Pose3dc pose) {
        double minimumX = Math.max(scanBounds.minX, subLevelBounds.minX());
        double minimumY = Math.max(scanBounds.minY, subLevelBounds.minY());
        double minimumZ = Math.max(scanBounds.minZ, subLevelBounds.minZ());
        double maximumX = Math.min(scanBounds.maxX, subLevelBounds.maxX());
        double maximumY = Math.min(scanBounds.maxY, subLevelBounds.maxY());
        double maximumZ = Math.min(scanBounds.maxZ, subLevelBounds.maxZ());
        if (minimumX < maximumX && minimumY < maximumY && minimumZ < maximumZ) {
            return candidateBounds(
                    new AABB(minimumX, minimumY, minimumZ, maximumX, maximumY, maximumZ),
                    pose);
        }

        // A tracked sub-level can remain authoritative while its broadphase bounds are stale.
        return candidateBounds(scanBounds, pose);
    }

    static LocalScanBounds candidateBounds(AABB scanBounds, Pose3dc pose) {
        BoundingBox3d localBounds = new BoundingBox3d(
                scanBounds).transformInverse(pose);
        return new LocalScanBounds(
                new BlockPos(
                        Mth.floor(localBounds.minX()),
                        Mth.floor(localBounds.minY()),
                        Mth.floor(localBounds.minZ())),
                new BlockPos(
                        Mth.floor(localBounds.maxX()),
                        Mth.floor(localBounds.maxY()),
                        Mth.floor(localBounds.maxZ())));
    }

    static List<SubLevelAccess> subLevelsToScan(
            SubLevelAccess trackedSubLevel,
            Iterable<? extends SubLevelAccess> intersectingSubLevels) {
        Map<UUID, SubLevelAccess> uniqueSubLevels = new LinkedHashMap<>();
        if (trackedSubLevel != null) {
            uniqueSubLevels.put(trackedSubLevel.getUniqueId(), trackedSubLevel);
        }
        for (SubLevelAccess subLevel : intersectingSubLevels) {
            uniqueSubLevels.putIfAbsent(subLevel.getUniqueId(), subLevel);
        }
        return List.copyOf(uniqueSubLevels.values());
    }

    record LocalScanBounds(BlockPos minimum, BlockPos maximum) {
        boolean contains(BlockPos position) {
            return position.getX() >= minimum.getX() && position.getX() <= maximum.getX()
                    && position.getY() >= minimum.getY() && position.getY() <= maximum.getY()
                    && position.getZ() >= minimum.getZ() && position.getZ() <= maximum.getZ();
        }
    }
}
