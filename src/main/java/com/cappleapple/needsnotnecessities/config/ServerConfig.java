package com.cappleapple.needsnotnecessities.config;

import com.cappleapple.needsnotnecessities.survival.SurvivalModule;
import java.util.EnumMap;
import java.util.Map;
import net.neoforged.neoforge.common.ModConfigSpec;

public final class ServerConfig {
    public static final ServerConfig INSTANCE;
    public static final ModConfigSpec SPEC;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        INSTANCE = new ServerConfig(builder);
        SPEC = builder.build();
    }

    private final Map<SurvivalModule, ModConfigSpec.BooleanValue> moduleEnabled = new EnumMap<>(SurvivalModule.class);

    public final ModConfigSpec.DoubleValue dayLengthMinutes;
    public final ModConfigSpec.IntValue stateUpdateIntervalTicks;

    public final ModConfigSpec.DoubleValue hoursPerHungerPoint;
    public final ModConfigSpec.DoubleValue hoursPerSaturationPoint;
    public final ModConfigSpec.DoubleValue hungerEffectDecayMultiplier;
    public final ModConfigSpec.DoubleValue hungerEatBelowStagePercentage;
    public final ModConfigSpec.EnumValue<DeathStatePolicy> hungerDeathPolicy;

    public final ModConfigSpec.DoubleValue thirstHoursPerFoodHour;
    public final ModConfigSpec.DoubleValue normalDrinkLevelAdjustment;
    public final ModConfigSpec.DoubleValue alcoholicDrinkLevelAdjustment;
    public final ModConfigSpec.EnumValue<DeathStatePolicy> thirstDeathPolicy;

    public final ModConfigSpec.DoubleValue restFullRecoveryHours;
    public final ModConfigSpec.BooleanValue allowDaytimeSleep;
    public final ModConfigSpec.BooleanValue requireTiredToSleep;
    public final ModConfigSpec.DoubleValue sleepBelowStagePercentage;
    public final ModConfigSpec.BooleanValue daytimeSleepSkipsToNight;
    public final ModConfigSpec.EnumValue<DeathStatePolicy> restDeathPolicy;

    public final ModConfigSpec.IntValue comfortRadius;
    public final ModConfigSpec.IntValue comfortScanIntervalTicks;
    public final ModConfigSpec.DoubleValue comfortRetentionMinutes;
    public final ModConfigSpec.DoubleValue comfortDiminishingFactor;
    public final ModConfigSpec.EnumValue<DeathStatePolicy> comfortDeathPolicy;

    public final ModConfigSpec.EnumValue<DeathStatePolicy> mealDeathPolicy;
    public final ModConfigSpec.DoubleValue mealBaseDurationHours;
    public final ModConfigSpec.DoubleValue mealDurationPerComplexity;
    public final ModConfigSpec.DoubleValue mealMaximumDurationHours;
    public final ModConfigSpec.DoubleValue mealScorePerComplexity;
    public final ModConfigSpec.IntValue mealMaximumBonuses;
    public final ModConfigSpec.DoubleValue mealSameGroupDiminishingFactor;
    public final ModConfigSpec.DoubleValue mealEffectStrengthMultiplier;
    public final ModConfigSpec.EnumValue<MealEqualPolicy> mealEqualPolicy;

    public final ModConfigSpec.BooleanValue qualityFoodIntegrationEnabled;
    public final ModConfigSpec.DoubleValue qualityFoodDurationInfluence;
    public final ModConfigSpec.DoubleValue qualityFoodMaximumDurationMultiplier;
    public final ModConfigSpec.DoubleValue qualityFoodStrengthPerLevel;
    public final ModConfigSpec.DoubleValue qualityFoodMaximumStrengthMultiplier;
    public final ModConfigSpec.BooleanValue farmersDelightNourishmentPausesHunger;

    public final ModConfigSpec.EnumValue<BaseHealthMode> baseHealthMode;
    public final ModConfigSpec.DoubleValue baseHealthAmount;
    public final ModConfigSpec.DoubleValue initialHealthPercentage;
    public final ModConfigSpec.BooleanValue scaleHealthEffectsWithMaxHealth;
    public final ModConfigSpec.DoubleValue healthEffectReferenceMaxHealth;

    public final ModConfigSpec.DoubleValue regenerationAmount;
    public final ModConfigSpec.IntValue regenerationIntervalTicks;
    public final ModConfigSpec.BooleanValue regenerateDuringCombat;
    public final ModConfigSpec.IntValue regenerationCombatCooldownTicks;

    public final ModConfigSpec.DoubleValue respawnHealthPercentage;
    public final ModConfigSpec.BooleanValue deathPenaltiesEnabled;
    public final ModConfigSpec.ConfigValue<String> respawnPenaltyMessage;

    public final ModConfigSpec.DoubleValue notificationCooldownMinutes;

    private ServerConfig(ModConfigSpec.Builder builder) {
        builder.push("modules");
        for (SurvivalModule module : SurvivalModule.values()) {
            moduleEnabled.put(module, builder
                    .comment("Enable the " + module.configKey() + " module independently of every other module.")
                    .translation("needs_not_necessities.configuration.module." + module.configKey())
                    .define(module.configKey(), true));
        }
        builder.pop();

        builder.push("time");
        dayLengthMinutes = builder
                .comment("Real gameplay minutes represented by one 24-hour biological day. World day/night time is ignored.")
                .translation("needs_not_necessities.configuration.day_length_minutes")
                .defineInRange("day_length_minutes", 20.0D, 0.01D, 10080.0D);
        stateUpdateIntervalTicks = builder
                .comment("Interval for state reconciliation and modifier-plan checks. Biological age still counts exact active ticks.")
                .translation("needs_not_necessities.configuration.state_update_interval_ticks")
                .defineInRange("state_update_interval_ticks", 20, 1, 1200);
        builder.pop();

        builder.push("hunger");
        hoursPerHungerPoint = positiveDouble(builder, "hours_per_hunger_point", 2.0D,
                "Biological hunger-hours granted per resolved nutrition point.");
        hoursPerSaturationPoint = positiveDouble(builder, "hours_per_saturation_point", 0.5D,
                "Biological hunger-hours granted per resolved saturation point.");
        hungerEffectDecayMultiplier = rangedDouble(builder, "hunger_effect_decay_multiplier", 2.0D, 0.0D, 100.0D,
                "Hunger timer drain multiplier while the vanilla Hunger status effect is active.");
        hungerEatBelowStagePercentage = rangedDouble(builder, "eat_below_stage_percentage", 90.0D, 0.0D, 100.0D,
                "Percentage of ordered Hunger stages, counted from worst, in which players may eat.");
        hungerDeathPolicy = deathPolicy(builder, "death_behavior", DeathStatePolicy.RESET_WORST);
        builder.pop();

        builder.push("thirst");
        thirstHoursPerFoodHour = positiveDouble(builder, "thirst_hours_per_food_hour", 0.25D,
                "Thirst pressure generated per calculated food hunger-hour.");
        normalDrinkLevelAdjustment = rangedDouble(builder, "normal_drink_level_adjustment", 1.0D, -100.0D, 100.0D,
                "Configured thirst levels advanced by an item in the drinks tag.");
        alcoholicDrinkLevelAdjustment = rangedDouble(builder, "alcoholic_drink_level_adjustment", -1.0D, -100.0D, 100.0D,
                "Configured thirst levels advanced by an alcoholic drink; negative values make the player thirstier.");
        thirstDeathPolicy = deathPolicy(builder, "death_behavior", DeathStatePolicy.RESET_WORST);
        builder.pop();

        builder.push("rest");
        restFullRecoveryHours = positiveDouble(builder, "full_recovery_biological_hours", 1.0D,
                "Biological hours spent lying in a bed to move from the worst to the best configured rest state.");
        allowDaytimeSleep = builder
                .comment("Allow players to enter and remain in beds during the day.")
                .define("allow_daytime_sleep", true);
        requireTiredToSleep = builder
                .comment("Require the player's Rest stage to be inside the configured lowest-stage percentage before sleeping.")
                .define("require_tired_to_sleep", true);
        sleepBelowStagePercentage = rangedDouble(builder, "sleep_below_stage_percentage", 50.0D, 0.0D, 100.0D,
                "Percentage of ordered Rest stages, counted from worst, in which players may start sleeping.");
        daytimeSleepSkipsToNight = builder
                .comment("When enough players sleep during the day, skip to night. False skips to the next day instead.")
                .define("daytime_sleep_skips_to_night", true);
        restDeathPolicy = deathPolicy(builder, "death_behavior", DeathStatePolicy.RESET_WORST);
        builder.pop();

        builder.push("comfort");
        comfortRadius = builder.defineInRange("scan_radius", 8, 1, 64);
        comfortScanIntervalTicks = builder.defineInRange("scan_interval_ticks", 40, 1, 12000);
        comfortRetentionMinutes = positiveDouble(builder, "retention_minutes", 5.0D,
                "Real-time minutes that a retained higher Comfort value lasts.");
        comfortDiminishingFactor = rangedDouble(builder, "diminishing_factor", 0.5D, 0.0D, 1.0D,
                "Contribution multiplier applied to each additional source of the same comfort type.");
        comfortDeathPolicy = deathPolicy(builder, "death_behavior", DeathStatePolicy.CLEAR);
        builder.pop();

        builder.push("meal");
        mealDeathPolicy = deathPolicy(builder, "death_behavior", DeathStatePolicy.CLEAR);
        mealBaseDurationHours = positiveDouble(builder, "base_duration_hours", 4.0D,
                "Base biological duration of an automatically analyzed Active Meal.");
        mealDurationPerComplexity = positiveDouble(builder, "duration_per_complexity", 2.0D,
                "Additional biological meal duration per recipe complexity point.");
        mealMaximumDurationHours = positiveDouble(builder, "maximum_duration_hours", 48.0D,
                "Hard cap on Active Meal duration before optional compatibility quality scaling.");
        mealScorePerComplexity = positiveDouble(builder, "score_per_complexity", 2.0D,
                "Hidden meal-score contribution per recipe complexity point.");
        mealMaximumBonuses = builder.defineInRange("maximum_bonuses", 5, 0, 16);
        mealSameGroupDiminishingFactor = rangedDouble(
                builder,
                "same_group_diminishing_factor",
                0.5D,
                0.0D,
                1.0D,
                "Multiplier applied successively to additional direct recipe ingredients matched by the same meal-effect definition."
                        + " A value of 0.5 gives 100%, 50%, 25%, and so on.");
        mealEffectStrengthMultiplier = rangedDouble(builder, "effect_strength_multiplier", 1.0D, 0.0D, 10.0D,
                "Global multiplier for datapack-defined Active Meal modifier amounts.");
        mealEqualPolicy = builder.defineEnum("equal_score_policy", MealEqualPolicy.REFRESH);
        builder.pop();

        builder.push("compatibility");
        qualityFoodIntegrationEnabled = builder
                .comment("Use the resulting item's Quality Food data component when that optional mod is installed.")
                .define("quality_food_enabled", true);
        qualityFoodDurationInfluence = rangedDouble(builder, "quality_food_duration_influence", 1.0D, 0.0D, 1.0D,
                "How strongly Quality Food's configured duration multiplier affects Active Meal duration.");
        qualityFoodMaximumDurationMultiplier = rangedDouble(builder, "quality_food_maximum_duration_multiplier", 3.0D, 1.0D, 100.0D,
                "Safety cap applied to the Quality Food duration multiplier.");
        qualityFoodStrengthPerLevel = rangedDouble(builder, "quality_food_strength_per_level", 0.025D, 0.0D, 1.0D,
                "Modest Active Meal modifier scaling per Quality Food quality level.");
        qualityFoodMaximumStrengthMultiplier = rangedDouble(builder, "quality_food_maximum_strength_multiplier", 1.15D, 1.0D, 10.0D,
                "Hard cap on modifier strength gained from Quality Food quality.");
        farmersDelightNourishmentPausesHunger = builder
                .comment("Pause the custom hunger-hours countdown while Farmer's Delight Nourishment is active.")
                .define("farmers_delight_nourishment_pauses_hunger", true);
        builder.pop();

        builder.push("base_health");
        baseHealthMode = builder.defineEnum("mode", BaseHealthMode.ADD);
        baseHealthAmount = rangedDouble(builder, "amount", 0.0D, -1024.0D, 1024.0D,
                "Amount added to base max health, or multiplier used in MULTIPLY mode.");
        initialHealthPercentage = rangedDouble(builder, "initial_health_percentage", 1.0D, 0.01D, 1.0D,
                "Fraction of current maximum health assigned when this mod first initializes a player.");
        builder.pop();

        builder.push("health_effects");
        scaleHealthEffectsWithMaxHealth = builder
                .comment("Scale Instant Health and Regeneration healing for players as a percentage of max health.")
                .define("scale_with_max_health", true);
        healthEffectReferenceMaxHealth = rangedDouble(builder, "reference_max_health", 20.0D, 0.01D, 1_000_000.0D,
                "Max-health value at which healing effects retain their vanilla healing amount.");
        builder.pop();

        builder.push("regeneration");
        regenerationAmount = positiveDouble(builder, "base_amount", 1.0D, "Health restored per regeneration interval.");
        regenerationIntervalTicks = builder.defineInRange("interval_ticks", 200, 1, 72000);
        regenerateDuringCombat = builder.define("during_combat", false);
        regenerationCombatCooldownTicks = builder.defineInRange("combat_cooldown_ticks", 200, 0, 72000);
        builder.pop();

        builder.push("death");
        deathPenaltiesEnabled = builder.define("enabled", true);
        respawnHealthPercentage = rangedDouble(builder, "respawn_health_percentage", 0.25D, 0.01D, 1.0D,
                "Fraction of current maximum health assigned after a death respawn.");
        respawnPenaltyMessage = builder
                .comment("Chat message sent when both Hunger and Thirst are reduced by death. Leave blank to disable.")
                .define("respawn_penalty_message", "You awaken weak, hungry, and parched.");
        builder.pop();

        builder.push("notifications");
        notificationCooldownMinutes = positiveDouble(builder, "cooldown_minutes", 5.0D,
                "Real-time cooldown before the same datapack state can notify again.");
        builder.pop();
    }

    public boolean isEnabled(SurvivalModule module) {
        return moduleEnabled.get(module).getAsBoolean();
    }

    public boolean anyBiologicalModuleEnabled() {
        return isEnabled(SurvivalModule.HUNGER)
                || isEnabled(SurvivalModule.THIRST)
                || isEnabled(SurvivalModule.REST)
                || isEnabled(SurvivalModule.ACTIVE_MEAL);
    }

    private static ModConfigSpec.EnumValue<DeathStatePolicy> deathPolicy(
            ModConfigSpec.Builder builder, String key, DeathStatePolicy defaultValue) {
        return builder.comment("State handling after death: KEEP, RESET_NEUTRAL, RESET_WORST, RESET_BEST, or CLEAR.")
                .defineEnum(key, defaultValue);
    }

    private static ModConfigSpec.DoubleValue positiveDouble(
            ModConfigSpec.Builder builder, String key, double defaultValue, String comment) {
        return rangedDouble(builder, key, defaultValue, 0.0D, Double.MAX_VALUE, comment);
    }

    private static ModConfigSpec.DoubleValue rangedDouble(
            ModConfigSpec.Builder builder, String key, double defaultValue, double min, double max, String comment) {
        return builder.comment(comment).defineInRange(key, defaultValue, min, max);
    }
}
