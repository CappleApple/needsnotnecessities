package com.cappleapple.needsnotnecessities.survival.comfort;

import com.cappleapple.needsnotnecessities.config.ServerConfig;
import com.cappleapple.needsnotnecessities.data.PlayerSurvivalData;
import com.cappleapple.needsnotnecessities.modifier.SurvivalModifier;
import com.cappleapple.needsnotnecessities.modifier.SurvivalModifierService;
import com.cappleapple.needsnotnecessities.survival.SurvivalModule;
import com.cappleapple.needsnotnecessities.survival.state.StateTrackService;
import com.cappleapple.needsnotnecessities.api.provider.SurvivalProviderRegistry;
import com.cappleapple.needsnotnecessities.api.event.ComfortChangeEvent;
import com.cappleapple.needsnotnecessities.api.event.ComfortScanEvent;
import net.neoforged.neoforge.common.NeoForge;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockState;

public final class ComfortService {
    private static final Map<UUID, ComfortScanResult> LAST_SCANS = new HashMap<>();

    private ComfortService() {
    }

    public static void tick(ServerPlayer player, PlayerSurvivalData data) {
        if (!ServerConfig.INSTANCE.isEnabled(SurvivalModule.COMFORT)) {
            return;
        }
        if (data.comfortRetentionTicks() > 0L) {
            data.setRetainedComfort(data.retainedComfort(), data.comfortRetentionTicks() - 1L);
        }
        if (player.tickCount % ServerConfig.INSTANCE.comfortScanIntervalTicks.getAsInt() == 0) {
            scanAndUpdate(player, data);
        }
    }

    public static ComfortScanResult scanAndUpdate(ServerPlayer player, PlayerSurvivalData data) {
        ComfortScanResult result = scan(player);
        double previousComfort = data.retainedComfort();
        long fullRetentionTicks = Math.max(
                0L,
                Math.round(ServerConfig.INSTANCE.comfortRetentionMinutes.getAsDouble() * 60.0D * 20.0D));
        if (result.totalComfort() >= data.retainedComfort() || data.comfortRetentionTicks() <= 0L) {
            data.setRetainedComfort(result.totalComfort(), result.totalComfort() > 0.0D ? fullRetentionTicks : 0L);
        }
        LAST_SCANS.put(player.getUUID(), result);
        if (Double.compare(previousComfort, data.retainedComfort()) != 0) {
            if (ServerConfig.INSTANCE.isEnabled(SurvivalModule.COMPATIBILITY)) {
                NeoForge.EVENT_BUS.post(new ComfortChangeEvent(player, previousComfort, data.retainedComfort()));
            }
            SurvivalModifierService.forceRecompute(player, StateTrackService.gatherAllModifiers(player, data));
        }
        return result;
    }

    public static ComfortScanResult scan(ServerPlayer player) {
        int radius = ServerConfig.INSTANCE.comfortRadius.getAsInt();
        Map<String, List<ComfortContributor>> byType = new LinkedHashMap<>();
        BlockPos center = player.blockPosition();
        for (BlockPos cursor : BlockPos.betweenClosed(center.offset(-radius, -radius, -radius), center.offset(radius, radius, radius))) {
            BlockState state = player.level().getBlockState(cursor);
            if (state.isAir()) {
                continue;
            }
            Map<String, ComfortContributor> bestAtBlock = new HashMap<>();
            for (ComfortSourceDefinition definition : ComfortSourceManager.INSTANCE.matching(state.getBlock())) {
                ComfortContributor contributor = new ComfortContributor(
                        cursor.immutable(), definition.id(), definition.type(), definition.comfort(), 0.0D);
                bestAtBlock.merge(definition.type(), contributor,
                        (left, right) -> left.baseComfort() >= right.baseComfort() ? left : right);
            }
            bestAtBlock.forEach((type, contributor) -> byType.computeIfAbsent(type, ignored -> new ArrayList<>()).add(contributor));
        }
        if (ServerConfig.INSTANCE.isEnabled(SurvivalModule.COMPATIBILITY)) {
            SurvivalProviderRegistry.comfortProviders().forEach(provider ->
                    provider.provide(player).forEach(contribution ->
                            byType.computeIfAbsent(contribution.type(), ignored -> new ArrayList<>())
                                    .add(new ComfortContributor(
                                            center,
                                            contribution.id(),
                                            contribution.type(),
                                            contribution.comfort(),
                                            0.0D))));
            List<ComfortContributor> discovered = byType.values().stream().flatMap(List::stream).toList();
            ComfortScanEvent event = new ComfortScanEvent(player, discovered);
            NeoForge.EVENT_BUS.post(event);
            byType.clear();
            event.contributors().forEach(contributor ->
                    byType.computeIfAbsent(contributor.type(), ignored -> new ArrayList<>()).add(contributor));
        }
        return calculate(byType, ServerConfig.INSTANCE.comfortDiminishingFactor.getAsDouble());
    }

    public static ComfortScanResult calculate(Map<String, List<ComfortContributor>> grouped, double diminishingFactor) {
        if (!Double.isFinite(diminishingFactor) || diminishingFactor < 0.0D || diminishingFactor > 1.0D) {
            throw new IllegalArgumentException("Comfort diminishing factor must be between zero and one");
        }
        List<ComfortContributor> applied = new ArrayList<>();
        Map<String, Double> totals = new LinkedHashMap<>();
        grouped.forEach((type, contributors) -> {
            List<ComfortContributor> sorted = contributors.stream()
                    .sorted(Comparator.comparingDouble(ComfortContributor::baseComfort).reversed())
                    .toList();
            double typeTotal = 0.0D;
            for (int index = 0; index < sorted.size(); index++) {
                ComfortContributor source = sorted.get(index);
                double contribution = source.baseComfort() * Math.pow(diminishingFactor, index);
                typeTotal += contribution;
                applied.add(new ComfortContributor(
                        source.position(), source.definitionId(), source.type(), source.baseComfort(), contribution));
            }
            totals.put(type, typeTotal);
        });
        return new ComfortScanResult(totals.values().stream().mapToDouble(Double::doubleValue).sum(), totals, applied);
    }

    public static List<SurvivalModifier> gatherModifiers(PlayerSurvivalData data) {
        if (!ServerConfig.INSTANCE.isEnabled(SurvivalModule.COMFORT)) {
            return List.of();
        }
        return ComfortEffectManager.INSTANCE.modifiersAt(data.retainedComfort());
    }

    public static ComfortScanResult lastScan(ServerPlayer player) {
        return LAST_SCANS.getOrDefault(player.getUUID(), ComfortScanResult.EMPTY);
    }

    public static void forget(ServerPlayer player) {
        LAST_SCANS.remove(player.getUUID());
    }

    public record ComfortContributor(
            BlockPos position,
            net.minecraft.resources.ResourceLocation definitionId,
            String type,
            double baseComfort,
            double appliedComfort) {
    }

    public record ComfortScanResult(
            double totalComfort,
            Map<String, Double> totalsByType,
            List<ComfortContributor> contributors) {
        public static final ComfortScanResult EMPTY = new ComfortScanResult(0.0D, Map.of(), List.of());

        public ComfortScanResult {
            totalsByType = Map.copyOf(totalsByType);
            contributors = List.copyOf(contributors);
        }
    }
}
