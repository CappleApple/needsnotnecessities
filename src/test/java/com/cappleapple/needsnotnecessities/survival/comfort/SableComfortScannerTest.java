package com.cappleapple.needsnotnecessities.survival.comfort;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.ryanhcode.sable.companion.math.BoundingBox3d;
import dev.ryanhcode.sable.companion.math.BoundingBox3dc;
import dev.ryanhcode.sable.companion.math.Pose3d;
import dev.ryanhcode.sable.companion.math.Pose3dc;
import dev.ryanhcode.sable.companion.SubLevelAccess;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import org.joml.Quaterniond;
import org.joml.Vector3d;
import org.junit.jupiter.api.Test;

class SableComfortScannerTest {
    @Test
    void translatedSubLevelBlockIsIncludedInLocalCandidateBounds() {
        BlockPos localBlock = new BlockPos(32_000, 80, -48_000);
        Pose3d pose = new Pose3d(
                new Vector3d(12.0D, 65.0D, -8.0D),
                new Quaterniond(),
                new Vector3d(localBlock.getX(), localBlock.getY(), localBlock.getZ()),
                new Vector3d(1.0D));
        AABB scanBounds = new AABB(10.0D, 63.0D, -10.0D, 15.0D, 68.0D, -5.0D);

        var bounds = SableComfortScanner.candidateBounds(scanBounds, pose);

        assertTrue(bounds.contains(localBlock));
        assertTrue(scanBounds.contains(pose.transformPosition(localBlock.getCenter())));
    }

    @Test
    void rotatedSubLevelBlockIsIncludedInLocalCandidateBounds() {
        BlockPos localBlock = new BlockPos(16_000, 40, 16_000);
        Pose3d pose = new Pose3d(
                new Vector3d(100.0D, 70.0D, 100.0D),
                new Quaterniond().rotateY(Math.PI / 2.0D),
                new Vector3d(localBlock.getX(), localBlock.getY(), localBlock.getZ()),
                new Vector3d(1.0D));
        AABB scanBounds = new AABB(98.0D, 68.0D, 98.0D, 103.0D, 73.0D, 103.0D);

        var bounds = SableComfortScanner.candidateBounds(scanBounds, pose);

        assertTrue(bounds.contains(localBlock));
        assertTrue(scanBounds.contains(pose.transformPosition(localBlock.getCenter())));
    }

    @Test
    void staleSubLevelBoundsDoNotExcludeTrackedLocalBlocks() {
        BlockPos localBlock = new BlockPos(24_000, 70, 24_000);
        Pose3d pose = new Pose3d(
                new Vector3d(20.0D, 75.0D, 20.0D),
                new Quaterniond(),
                new Vector3d(localBlock.getX(), localBlock.getY(), localBlock.getZ()),
                new Vector3d(1.0D));
        AABB scanBounds = new AABB(18.0D, 73.0D, 18.0D, 23.0D, 78.0D, 23.0D);

        var bounds = SableComfortScanner.candidateBounds(
                scanBounds,
                new BoundingBox3d(0.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D),
                pose);

        assertTrue(bounds.contains(localBlock));
    }

    @Test
    void trackedSubLevelIsScannedEvenWhenIntersectionQueryDoesNotReturnIt() {
        TestSubLevel tracked = new TestSubLevel(UUID.randomUUID());
        TestSubLevel intersecting = new TestSubLevel(UUID.randomUUID());

        List<SubLevelAccess> result = SableComfortScanner.subLevelsToScan(
                tracked,
                List.of(intersecting));

        assertEquals(2, result.size());
        assertSame(tracked, result.get(0));
        assertSame(intersecting, result.get(1));
    }

    @Test
    void trackedSubLevelIsNotScannedTwiceWhenAlsoIntersecting() {
        UUID id = UUID.randomUUID();
        TestSubLevel tracked = new TestSubLevel(id);
        TestSubLevel duplicate = new TestSubLevel(id);

        List<SubLevelAccess> result = SableComfortScanner.subLevelsToScan(
                tracked,
                List.of(duplicate));

        assertEquals(1, result.size());
        assertSame(tracked, result.getFirst());
    }

    private record TestSubLevel(UUID getUniqueId) implements SubLevelAccess {
        @Override
        public Pose3dc logicalPose() {
            return new Pose3d();
        }

        @Override
        public Pose3dc lastPose() {
            return logicalPose();
        }

        @Override
        public BoundingBox3dc boundingBox() {
            return new BoundingBox3d();
        }

        @Override
        public String getName() {
            return "test";
        }
    }
}
