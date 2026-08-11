package com.cappleapple.needsnotnecessities.network;

import com.cappleapple.needsnotnecessities.NeedsNotNecessities;
import com.cappleapple.needsnotnecessities.config.ServerConfig;
import com.cappleapple.needsnotnecessities.data.ActiveMealData;
import com.cappleapple.needsnotnecessities.data.PlayerSurvivalData;
import com.cappleapple.needsnotnecessities.modifier.SurvivalModifier;
import com.cappleapple.needsnotnecessities.modifier.ModifierOperation;
import com.cappleapple.needsnotnecessities.survival.SurvivalModule;
import com.cappleapple.needsnotnecessities.survival.comfort.ComfortService;
import com.cappleapple.needsnotnecessities.survival.comfort.ComfortSourceDefinition;
import com.cappleapple.needsnotnecessities.survival.comfort.ComfortSourceManager;
import com.cappleapple.needsnotnecessities.survival.state.StateDefinitionManager;
import com.cappleapple.needsnotnecessities.survival.state.StateTimeline;
import com.cappleapple.needsnotnecessities.survival.state.SurvivalStateDefinition;
import com.cappleapple.needsnotnecessities.survival.state.SurvivalStateIds;
import com.cappleapple.needsnotnecessities.survival.food.FoodTooltipGroupDefinition;
import com.cappleapple.needsnotnecessities.survival.food.FoodTooltipGroupManager;
import com.cappleapple.needsnotnecessities.survival.meal.MealEffectDefinition;
import com.cappleapple.needsnotnecessities.survival.meal.MealEffectManager;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

public final class SurvivalSnapshotService {
    private SurvivalSnapshotService() {
    }

    public static void sync(ServerPlayer player, PlayerSurvivalData data) {
        PacketDistributor.sendToPlayer(player, new SurvivalSnapshotPayload(createSnapshot(data)));
    }

    static CompoundTag createSnapshot(PlayerSurvivalData data) {
        ServerConfig config = ServerConfig.INSTANCE;
        CompoundTag root = new CompoundTag();
        CompoundTag enabled = new CompoundTag();
        for (SurvivalModule module : SurvivalModule.values()) {
            enabled.putBoolean(module.configKey(), config.isEnabled(module));
        }
        root.put("enabled", enabled);
        root.putDouble("day_length_minutes", config.dayLengthMinutes.getAsDouble());
        root.putDouble("hours_per_hunger_point", config.hoursPerHungerPoint.getAsDouble());
        root.putDouble("hours_per_saturation_point", config.hoursPerSaturationPoint.getAsDouble());
        root.putDouble("thirst_hours_per_food_hour", config.thirstHoursPerFoodHour.getAsDouble());
        root.putDouble("meal_score_per_complexity", config.mealScorePerComplexity.getAsDouble());
        root.putDouble("meal_base_duration_hours", config.mealBaseDurationHours.getAsDouble());
        root.putDouble("meal_duration_per_complexity", config.mealDurationPerComplexity.getAsDouble());
        root.putDouble("meal_maximum_duration_hours", config.mealMaximumDurationHours.getAsDouble());
        root.putInt("meal_maximum_bonuses", config.mealMaximumBonuses.getAsInt());
        root.putDouble("meal_same_group_diminishing_factor", config.mealSameGroupDiminishingFactor.getAsDouble());
        root.putDouble("meal_effect_strength_multiplier", config.mealEffectStrengthMultiplier.getAsDouble());
        root.putBoolean("quality_food_enabled", config.isEnabled(SurvivalModule.COMPATIBILITY)
                && config.qualityFoodIntegrationEnabled.getAsBoolean());
        root.putDouble("quality_food_duration_influence", config.qualityFoodDurationInfluence.getAsDouble());
        root.putDouble("quality_food_maximum_duration_multiplier", config.qualityFoodMaximumDurationMultiplier.getAsDouble());
        root.putDouble("quality_food_strength_per_level", config.qualityFoodStrengthPerLevel.getAsDouble());
        root.putDouble("quality_food_maximum_strength_multiplier", config.qualityFoodMaximumStrengthMultiplier.getAsDouble());
        root.put("food_tooltip_groups", writeFoodTooltipGroups());
        root.put("meal_effect_definitions", writeMealEffectDefinitions());

        ListTag states = new ListTag();
        for (var entry : List.of(
                new StateModule(SurvivalStateIds.HUNGER, SurvivalModule.HUNGER, "Hunger"),
                new StateModule(SurvivalStateIds.THIRST, SurvivalModule.THIRST, "Thirst"),
                new StateModule(SurvivalStateIds.REST, SurvivalModule.REST, "Rest"))) {
            if (!config.isEnabled(entry.module())) {
                continue;
            }
            StateTimeline timeline = StateDefinitionManager.INSTANCE.find(entry.id()).orElse(null);
            if (timeline == null) {
                continue;
            }
            double position = timeline.clamp(data.statePosition(entry.id()));
            SurvivalStateDefinition state = timeline.stateAt(position);
            StateTimeline.StateRange range = timeline.rangeOf(state.id());
            CompoundTag stateTag = new CompoundTag();
            stateTag.putString("system", entry.id().toString());
            stateTag.putString("label", entry.label());
            stateTag.putString("state_id", state.id().toString());
            stateTag.putString("state_name", state.displayName());
            stateTag.putString("description", state.description());
            stateTag.putDouble("position_hours", position);
            stateTag.putDouble("range_start", range.startHours());
            stateTag.putDouble("range_end", range.endHours());
            stateTag.putDouble("total_hours", timeline.totalHours());
            stateTag.putDouble("hours_to_worse_state", Math.max(0.0D, position - range.startHours()));
            stateTag.put("modifiers", writeModifiers(stateEffectModifiers(
                    state,
                    config.isEnabled(SurvivalModule.PASSIVE_REGENERATION))));
            states.add(stateTag);
        }
        root.put("states", states);

        if (config.isEnabled(SurvivalModule.COMFORT)) {
            CompoundTag comfort = new CompoundTag();
            comfort.putDouble("value", data.retainedComfort());
            comfort.putLong("retention_ticks", data.comfortRetentionTicks());
            comfort.putInt("radius", config.comfortRadius.getAsInt());
            comfort.putDouble("diminishing_factor", config.comfortDiminishingFactor.getAsDouble());
            comfort.put("modifiers", writeModifiers(ComfortService.gatherModifiers(data)));
            root.put("comfort", comfort);
            root.put("comfort_sources", writeComfortSources());
        }
        if (config.isEnabled(SurvivalModule.ACTIVE_MEAL)) {
            data.activeMeal().map(ActiveMealData::toTag).ifPresent(tag -> root.put("active_meal", tag));
        }
        return root;
    }

    private static ListTag writeModifiers(List<SurvivalModifier> modifiers) {
        ListTag list = new ListTag();
        for (SurvivalModifier modifier : modifiers) {
            CompoundTag tag = new CompoundTag();
            tag.putString("target", modifier.target().toString());
            tag.putDouble("amount", modifier.amount());
            tag.putString("operation", modifier.operation().name());
            list.add(tag);
        }
        return list;
    }

    static List<SurvivalModifier> stateEffectModifiers(
            SurvivalStateDefinition state,
            boolean passiveRegenerationEnabled) {
        List<SurvivalModifier> result = new ArrayList<>(state.modifiers());
        if (passiveRegenerationEnabled && state.passiveRegenerationMultiplier() != 1.0D) {
            result.add(new SurvivalModifier(
                    net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(
                            state.id().getNamespace(), state.id().getPath() + "/passive_regeneration"),
                    NeedsNotNecessities.id("passive_regeneration"),
                    state.passiveRegenerationMultiplier() - 1.0D,
                    ModifierOperation.MULTIPLY_TOTAL));
        }
        return List.copyOf(result);
    }

    private static ListTag writeComfortSources() {
        ListTag list = new ListTag();
        for (ComfortSourceDefinition definition : ComfortSourceManager.INSTANCE.definitions()) {
            CompoundTag tag = new CompoundTag();
            tag.putString("id", definition.id().toString());
            tag.putString("type", definition.type());
            tag.putString("name", definition.displayName());
            tag.putDouble("comfort", definition.comfort());
            definition.block().ifPresent(block -> {
                tag.putString("selector_kind", "block");
                tag.putString("selector", BuiltInRegistries.BLOCK.getKey(block).toString());
            });
            definition.tag().ifPresent(blockTag -> {
                tag.putString("selector_kind", "tag");
                tag.putString("selector", blockTag.location().toString());
            });
            definition.autoMatch().ifPresent(filter -> {
                tag.putString("auto_namespace_regex", filter.namespaceRegex());
                tag.putString("auto_path_regex", filter.pathRegex());
                filter.excludePathRegex().ifPresent(regex -> tag.putString("auto_exclude_path_regex", regex));
            });
            list.add(tag);
        }
        return list;
    }

    private static ListTag writeFoodTooltipGroups() {
        ListTag list = new ListTag();
        for (FoodTooltipGroupDefinition definition : FoodTooltipGroupManager.INSTANCE.definitions()) {
            CompoundTag tag = new CompoundTag();
            tag.putString("id", definition.id().toString());
            tag.putString("name", definition.displayName());
            tag.putDouble("minimum_hours", definition.minimumHours());
            tag.putDouble("maximum_hours", definition.maximumHours());
            tag.putInt("color", definition.color());
            tag.putString("description", definition.description());
            list.add(tag);
        }
        return list;
    }

    private static ListTag writeMealEffectDefinitions() {
        ListTag list = new ListTag();
        for (MealEffectDefinition definition : MealEffectManager.INSTANCE.definitions()) {
            CompoundTag tag = new CompoundTag();
            tag.putString("id", definition.id().toString());
            definition.item().ifPresent(item ->
                    tag.putString("item", BuiltInRegistries.ITEM.getKey(item).toString()));
            ListTag tags = new ListTag();
            definition.tags().forEach(itemTag -> {
                CompoundTag entry = new CompoundTag();
                entry.putString("id", itemTag.location().toString());
                tags.add(entry);
            });
            tag.put("tags", tags);
            CompoundTag traits = new CompoundTag();
            definition.traits().forEach(traits::putDouble);
            tag.put("traits", traits);
            ListTag bonuses = new ListTag();
            definition.bonuses().forEach(bonus -> {
                CompoundTag entry = new CompoundTag();
                entry.putString("id", bonus.id().toString());
                entry.putString("trait", bonus.trait());
                entry.putString("target", bonus.target().toString());
                entry.putDouble("amount", bonus.amount());
                entry.putString("operation", bonus.operation().name());
                bonuses.add(entry);
            });
            tag.put("bonuses", bonuses);
            tag.putDouble("score_bonus", definition.scoreBonus());
            tag.putDouble("duration_bonus_hours", definition.durationBonusHours());
            list.add(tag);
        }
        return list;
    }

    private record StateModule(
            net.minecraft.resources.ResourceLocation id,
            SurvivalModule module,
            String label) {
    }
}
