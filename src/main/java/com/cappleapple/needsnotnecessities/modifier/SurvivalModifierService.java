package com.cappleapple.needsnotnecessities.modifier;

import com.cappleapple.needsnotnecessities.NeedsNotNecessities;
import com.cappleapple.needsnotnecessities.data.ModAttachments;
import com.cappleapple.needsnotnecessities.data.PlayerSurvivalData;
import com.cappleapple.needsnotnecessities.config.ServerConfig;
import com.cappleapple.needsnotnecessities.survival.SurvivalModule;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

public final class SurvivalModifierService {
    public static final ResourceLocation PASSIVE_REGENERATION = NeedsNotNecessities.id("passive_regeneration");

    private static final Map<UUID, Integer> LAST_PLAN_HASH = new HashMap<>();

    private SurvivalModifierService() {
    }

    public static void recomputeIfChanged(ServerPlayer player, List<SurvivalModifier> gathered) {
        apply(player, gathered, false);
    }

    public static void forceRecompute(ServerPlayer player, List<SurvivalModifier> gathered) {
        apply(player, gathered, true);
    }

    public static void forget(ServerPlayer player) {
        LAST_PLAN_HASH.remove(player.getUUID());
    }

    public static double scalarValue(PlayerSurvivalData data, ResourceLocation target) {
        return data.computedScalarModifiers().getOrDefault(target, 1.0D);
    }

    private static void apply(ServerPlayer player, List<SurvivalModifier> gathered, boolean force) {
        List<SurvivalModifier> plan = normalize(gathered);
        int planHash = plan.hashCode();
        if (!force && LAST_PLAN_HASH.getOrDefault(player.getUUID(), Integer.MIN_VALUE) == planHash) {
            return;
        }

        PlayerSurvivalData data = player.getData(ModAttachments.PLAYER_SURVIVAL);
        removePreviouslyApplied(player, data.appliedAttributeModifiers());

        Map<ResourceLocation, ResourceLocation> applied = new LinkedHashMap<>();
        Map<ResourceLocation, ScalarAccumulator> scalars = new HashMap<>();
        for (SurvivalModifier modifier : plan) {
            if (PASSIVE_REGENERATION.equals(modifier.target())) {
                if (!ServerConfig.INSTANCE.isEnabled(SurvivalModule.PASSIVE_REGENERATION)) {
                    continue;
                }
                scalars.computeIfAbsent(modifier.target(), ignored -> new ScalarAccumulator()).accept(modifier);
                continue;
            }

            Holder.Reference<Attribute> attribute = BuiltInRegistries.ATTRIBUTE.getHolder(modifier.target()).orElse(null);
            if (attribute == null) {
                NeedsNotNecessities.LOGGER.warn(
                        "Ignoring survival modifier {} because attribute target {} is not registered",
                        modifier.id(),
                        modifier.target());
                continue;
            }
            AttributeInstance instance = player.getAttribute(attribute);
            if (instance == null) {
                NeedsNotNecessities.LOGGER.debug(
                        "Player {} has no instance of survival attribute target {}",
                        player.getGameProfile().getName(),
                        modifier.target());
                continue;
            }
            instance.removeModifier(modifier.id());
            instance.addTransientModifier(new AttributeModifier(
                    modifier.id(), modifier.amount(), modifier.operation().toMinecraft()));
            applied.put(modifier.id(), modifier.target());
        }

        Map<ResourceLocation, Double> scalarValues = new HashMap<>();
        scalars.forEach((target, accumulator) -> scalarValues.put(target, accumulator.value()));
        data.replaceAppliedAttributeModifiers(applied);
        data.replaceComputedScalarModifiers(scalarValues);
        player.setHealth(Math.min(player.getHealth(), player.getMaxHealth()));
        LAST_PLAN_HASH.put(player.getUUID(), planHash);
    }

    private static List<SurvivalModifier> normalize(List<SurvivalModifier> gathered) {
        Map<ResourceLocation, SurvivalModifier> unique = new HashMap<>();
        for (SurvivalModifier modifier : gathered) {
            SurvivalModifier previous = unique.putIfAbsent(modifier.id(), modifier);
            if (previous != null && !previous.equals(modifier)) {
                NeedsNotNecessities.LOGGER.warn(
                        "Ignoring conflicting duplicate survival modifier ID {} targeting {}",
                        modifier.id(),
                        modifier.target());
            }
        }
        List<SurvivalModifier> sorted = new ArrayList<>(unique.values());
        sorted.sort(Comparator.comparing(modifier -> modifier.id().toString()));
        return List.copyOf(sorted);
    }

    private static void removePreviouslyApplied(
            ServerPlayer player,
            Map<ResourceLocation, ResourceLocation> appliedModifiers) {
        appliedModifiers.forEach((modifierId, attributeId) -> {
            BuiltInRegistries.ATTRIBUTE.getHolder(attributeId).ifPresent(attribute -> {
                AttributeInstance instance = player.getAttribute(attribute);
                if (instance != null) {
                    instance.removeModifier(modifierId);
                }
            });
        });
    }

    private static final class ScalarAccumulator {
        private double additions;
        private double multiplyBase;
        private double multiplyTotal = 1.0D;

        private void accept(SurvivalModifier modifier) {
            switch (modifier.operation()) {
                case ADD -> additions += modifier.amount();
                case MULTIPLY_BASE -> multiplyBase += modifier.amount();
                case MULTIPLY_TOTAL -> multiplyTotal *= 1.0D + modifier.amount();
            }
        }

        private double value() {
            return Math.max(0.0D, (1.0D + additions + multiplyBase) * multiplyTotal);
        }
    }
}
