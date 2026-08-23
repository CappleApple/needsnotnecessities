package com.cappleapple.needsnotnecessities.data;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.common.util.INBTSerializable;

public final class PlayerSurvivalData implements INBTSerializable<CompoundTag> {
    public static final int CURRENT_SCHEMA_VERSION = 5;

    private boolean initialized;
    private long biologicalAgeTicks;
    private final Map<ResourceLocation, Double> statePositions = new HashMap<>();
    private double retainedComfort;
    private long comfortRetentionTicks;
    private ActiveMealData activeMeal;
    private final Map<ResourceLocation, Long> notificationCooldownTicks = new HashMap<>();
    private final Map<ResourceLocation, ResourceLocation> appliedAttributeModifiers = new HashMap<>();
    private final Map<ResourceLocation, Double> computedScalarModifiers = new HashMap<>();
    private long lastCombatGameTick = -1L;
    private boolean pendingDeathHealthReset;
    private boolean pendingRespawnPenaltyMessage;
    private boolean hasBaseHealthAdjustment;
    private double originalBaseHealth;
    private double appliedBaseHealth;

    public boolean initialized() {
        return initialized;
    }

    public void markInitialized() {
        initialized = true;
    }

    public long biologicalAgeTicks() {
        return biologicalAgeTicks;
    }

    public void advanceBiologicalTicks(long ticks) {
        if (ticks < 0L) {
            throw new IllegalArgumentException("Biological tick delta cannot be negative");
        }
        biologicalAgeTicks = Long.MAX_VALUE - biologicalAgeTicks < ticks
                ? Long.MAX_VALUE
                : biologicalAgeTicks + ticks;
    }

    public double statePosition(ResourceLocation systemId) {
        return statePositions.getOrDefault(systemId, 0.0D);
    }

    public boolean hasStatePosition(ResourceLocation systemId) {
        return statePositions.containsKey(systemId);
    }

    public void setStatePosition(ResourceLocation systemId, double positionHours) {
        if (!Double.isFinite(positionHours)) {
            throw new IllegalArgumentException("State position must be finite");
        }
        statePositions.put(systemId, positionHours);
    }

    public Map<ResourceLocation, Double> statePositions() {
        return Map.copyOf(statePositions);
    }

    public double retainedComfort() {
        return retainedComfort;
    }

    public long comfortRetentionTicks() {
        return comfortRetentionTicks;
    }

    public void setRetainedComfort(double comfort, long retentionTicks) {
        if (!Double.isFinite(comfort) || comfort < 0.0D || retentionTicks < 0L) {
            throw new IllegalArgumentException("Comfort and retention time must be finite and non-negative");
        }
        retainedComfort = comfort;
        comfortRetentionTicks = retentionTicks;
    }

    public void clearComfort() {
        retainedComfort = 0.0D;
        comfortRetentionTicks = 0L;
    }

    public Optional<ActiveMealData> activeMeal() {
        return Optional.ofNullable(activeMeal);
    }

    public void setActiveMeal(ActiveMealData meal) {
        activeMeal = meal;
    }

    public void clearActiveMeal() {
        activeMeal = null;
    }

    public long notificationCooldown(ResourceLocation notificationId) {
        return notificationCooldownTicks.getOrDefault(notificationId, 0L);
    }

    public void setNotificationCooldown(ResourceLocation notificationId, long untilGameTick) {
        notificationCooldownTicks.put(notificationId, Math.max(0L, untilGameTick));
    }

    public Map<ResourceLocation, ResourceLocation> appliedAttributeModifiers() {
        return Map.copyOf(appliedAttributeModifiers);
    }

    public void replaceAppliedAttributeModifiers(Map<ResourceLocation, ResourceLocation> replacements) {
        appliedAttributeModifiers.clear();
        appliedAttributeModifiers.putAll(replacements);
    }

    public Map<ResourceLocation, Double> computedScalarModifiers() {
        return Map.copyOf(computedScalarModifiers);
    }

    public void replaceComputedScalarModifiers(Map<ResourceLocation, Double> replacements) {
        computedScalarModifiers.clear();
        computedScalarModifiers.putAll(replacements);
    }

    public long lastCombatGameTick() {
        return lastCombatGameTick;
    }

    public void markCombat(long gameTick) {
        lastCombatGameTick = Math.max(0L, gameTick);
    }

    public boolean pendingDeathHealthReset() {
        return pendingDeathHealthReset;
    }

    public void setPendingDeathHealthReset(boolean pendingDeathHealthReset) {
        this.pendingDeathHealthReset = pendingDeathHealthReset;
    }

    public boolean pendingRespawnPenaltyMessage() {
        return pendingRespawnPenaltyMessage;
    }

    public void setPendingRespawnPenaltyMessage(boolean pendingRespawnPenaltyMessage) {
        this.pendingRespawnPenaltyMessage = pendingRespawnPenaltyMessage;
    }

    public boolean hasBaseHealthAdjustment() {
        return hasBaseHealthAdjustment;
    }

    public double originalBaseHealth() {
        return originalBaseHealth;
    }

    public double appliedBaseHealth() {
        return appliedBaseHealth;
    }

    public void setBaseHealthAdjustment(double originalBaseHealth, double appliedBaseHealth) {
        if (!Double.isFinite(originalBaseHealth) || !Double.isFinite(appliedBaseHealth)) {
            throw new IllegalArgumentException("Base-health values must be finite");
        }
        hasBaseHealthAdjustment = true;
        this.originalBaseHealth = originalBaseHealth;
        this.appliedBaseHealth = appliedBaseHealth;
    }

    public void clearBaseHealthAdjustment() {
        hasBaseHealthAdjustment = false;
        originalBaseHealth = 0.0D;
        appliedBaseHealth = 0.0D;
    }

    public PlayerSurvivalData copy() {
        PlayerSurvivalData copy = new PlayerSurvivalData();
        copy.initialized = initialized;
        copy.biologicalAgeTicks = biologicalAgeTicks;
        copy.statePositions.putAll(statePositions);
        copy.retainedComfort = retainedComfort;
        copy.comfortRetentionTicks = comfortRetentionTicks;
        copy.activeMeal = activeMeal;
        copy.notificationCooldownTicks.putAll(notificationCooldownTicks);
        copy.appliedAttributeModifiers.putAll(appliedAttributeModifiers);
        copy.computedScalarModifiers.putAll(computedScalarModifiers);
        copy.lastCombatGameTick = lastCombatGameTick;
        copy.pendingDeathHealthReset = pendingDeathHealthReset;
        copy.pendingRespawnPenaltyMessage = pendingRespawnPenaltyMessage;
        copy.hasBaseHealthAdjustment = hasBaseHealthAdjustment;
        copy.originalBaseHealth = originalBaseHealth;
        copy.appliedBaseHealth = appliedBaseHealth;
        return copy;
    }

    @Override
    public CompoundTag serializeNBT(HolderLookup.Provider provider) {
        CompoundTag root = new CompoundTag();
        root.putInt("schema_version", CURRENT_SCHEMA_VERSION);
        root.putBoolean("initialized", initialized);
        root.putLong("biological_age_ticks", biologicalAgeTicks);
        root.put("state_positions", writeDoubleMap(statePositions));
        root.putDouble("retained_comfort", retainedComfort);
        root.putLong("comfort_retention_ticks", comfortRetentionTicks);
        if (activeMeal != null) {
            root.put("active_meal", activeMeal.toTag());
        }
        root.put("notification_cooldowns", writeLongMap(notificationCooldownTicks));
        root.put("applied_attribute_modifiers", writeResourceMap(appliedAttributeModifiers));
        root.put("computed_scalar_modifiers", writeDoubleMap(computedScalarModifiers));
        root.putLong("last_combat_game_tick", lastCombatGameTick);
        root.putBoolean("pending_death_health_reset", pendingDeathHealthReset);
        root.putBoolean("pending_respawn_penalty_message", pendingRespawnPenaltyMessage);
        if (hasBaseHealthAdjustment) {
            root.putDouble("original_base_health", originalBaseHealth);
            root.putDouble("applied_base_health", appliedBaseHealth);
        }
        return root;
    }

    @Override
    public void deserializeNBT(HolderLookup.Provider provider, CompoundTag root) {
        initialized = root.getBoolean("initialized");
        biologicalAgeTicks = Math.max(0L, root.getLong("biological_age_ticks"));
        statePositions.clear();
        readDoubleMap(root.getList("state_positions", Tag.TAG_COMPOUND), statePositions);
        retainedComfort = Math.max(0.0D, root.getDouble("retained_comfort"));
        comfortRetentionTicks = Math.max(0L, root.getLong("comfort_retention_ticks"));
        activeMeal = root.contains("active_meal", Tag.TAG_COMPOUND)
                ? ActiveMealData.fromTag(root.getCompound("active_meal")).orElse(null)
                : null;
        notificationCooldownTicks.clear();
        readLongMap(root.getList("notification_cooldowns", Tag.TAG_COMPOUND), notificationCooldownTicks);
        appliedAttributeModifiers.clear();
        readResourceMap(root.getList("applied_attribute_modifiers", Tag.TAG_COMPOUND), appliedAttributeModifiers);
        computedScalarModifiers.clear();
        readDoubleMap(root.getList("computed_scalar_modifiers", Tag.TAG_COMPOUND), computedScalarModifiers);
        lastCombatGameTick = root.contains("last_combat_game_tick", Tag.TAG_ANY_NUMERIC)
                ? root.getLong("last_combat_game_tick")
                : -1L;
        pendingDeathHealthReset = root.getBoolean("pending_death_health_reset");
        pendingRespawnPenaltyMessage = root.getBoolean("pending_respawn_penalty_message");
        if (root.contains("original_base_health", Tag.TAG_ANY_NUMERIC)
                && root.contains("applied_base_health", Tag.TAG_ANY_NUMERIC)) {
            double restoredOriginalBaseHealth = root.getDouble("original_base_health");
            double restoredAppliedBaseHealth = root.getDouble("applied_base_health");
            if (Double.isFinite(restoredOriginalBaseHealth) && Double.isFinite(restoredAppliedBaseHealth)) {
                setBaseHealthAdjustment(restoredOriginalBaseHealth, restoredAppliedBaseHealth);
            } else {
                clearBaseHealthAdjustment();
            }
        } else {
            clearBaseHealthAdjustment();
        }
    }

    private static ListTag writeDoubleMap(Map<ResourceLocation, Double> values) {
        ListTag list = new ListTag();
        values.forEach((id, value) -> {
            CompoundTag entry = new CompoundTag();
            entry.putString("id", id.toString());
            entry.putDouble("value", value);
            list.add(entry);
        });
        return list;
    }

    private static ListTag writeLongMap(Map<ResourceLocation, Long> values) {
        ListTag list = new ListTag();
        values.forEach((id, value) -> {
            CompoundTag entry = new CompoundTag();
            entry.putString("id", id.toString());
            entry.putLong("value", value);
            list.add(entry);
        });
        return list;
    }

    private static ListTag writeResourceMap(Map<ResourceLocation, ResourceLocation> values) {
        ListTag list = new ListTag();
        values.forEach((id, value) -> {
            CompoundTag entry = new CompoundTag();
            entry.putString("id", id.toString());
            entry.putString("value", value.toString());
            list.add(entry);
        });
        return list;
    }

    private static void readDoubleMap(ListTag list, Map<ResourceLocation, Double> destination) {
        for (Tag element : list) {
            CompoundTag entry = (CompoundTag) element;
            ResourceLocation id = ResourceLocation.tryParse(entry.getString("id"));
            double value = entry.getDouble("value");
            if (id != null && Double.isFinite(value)) {
                destination.put(id, value);
            }
        }
    }

    private static void readLongMap(ListTag list, Map<ResourceLocation, Long> destination) {
        for (Tag element : list) {
            CompoundTag entry = (CompoundTag) element;
            ResourceLocation id = ResourceLocation.tryParse(entry.getString("id"));
            if (id != null) {
                destination.put(id, Math.max(0L, entry.getLong("value")));
            }
        }
    }

    private static void readResourceMap(ListTag list, Map<ResourceLocation, ResourceLocation> destination) {
        for (Tag element : list) {
            CompoundTag entry = (CompoundTag) element;
            ResourceLocation id = ResourceLocation.tryParse(entry.getString("id"));
            ResourceLocation value = ResourceLocation.tryParse(entry.getString("value"));
            if (id != null && value != null) {
                destination.put(id, value);
            }
        }
    }
}
