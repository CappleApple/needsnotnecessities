package com.cappleapple.needsnotnecessities.compat;

import com.cappleapple.needsnotnecessities.NeedsNotNecessities;
import com.cappleapple.needsnotnecessities.config.ServerConfig;
import com.cappleapple.needsnotnecessities.modifier.SurvivalModifier;
import com.cappleapple.needsnotnecessities.survival.SurvivalModule;
import com.cappleapple.needsnotnecessities.survival.meal.MealAnalysis;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/**
 * Soft adapter for Quality Food 1.21.1. It intentionally uses the registered
 * data component and reflection so no Quality Food class appears in an
 * unconditional class signature or linkage path.
 */
public final class QualityFoodCompat {
    private static final ResourceLocation QUALITY_COMPONENT =
            ResourceLocation.fromNamespaceAndPath("quality_food", "quality");
    private static boolean warned;

    private QualityFoodCompat() {
    }

    public static MealAnalysis apply(ItemStack stack, MealAnalysis analysis) {
        ServerConfig config = ServerConfig.INSTANCE;
        if (!config.isEnabled(SurvivalModule.COMPATIBILITY)
                || !config.qualityFoodIntegrationEnabled.getAsBoolean()) {
            return analysis;
        }
        DataComponentType<?> componentType = BuiltInRegistries.DATA_COMPONENT_TYPE
                .getOptional(QUALITY_COMPONENT)
                .orElse(null);
        if (componentType == null) {
            return analysis;
        }
        try {
            Object quality = getComponent(stack, componentType);
            if (quality == null) {
                return analysis;
            }
            int level = ((Number) invoke(quality, "level")).intValue();
            if (level <= 0) {
                return analysis;
            }
            Object holder = invoke(quality, "getType");
            Object qualityType = invoke(holder, "value");
            double durationMultiplier = ((Number) invoke(qualityType, "durationMultiplier")).doubleValue();
            return applyScaling(analysis, level, durationMultiplier, config);
        } catch (ReflectiveOperationException | LinkageError | ClassCastException exception) {
            if (!warned) {
                warned = true;
                NeedsNotNecessities.LOGGER.warn(
                        "Quality Food is present but its quality component API could not be read; compatibility is disabled for this session",
                        exception);
            }
            return analysis;
        }
    }

    static MealAnalysis applyScaling(
            MealAnalysis analysis,
            int level,
            double qualityDurationMultiplier,
            ServerConfig config) {
        double requestedDurationMultiplier = 1.0D
                + (qualityDurationMultiplier - 1.0D) * config.qualityFoodDurationInfluence.getAsDouble();
        double durationMultiplier = Math.clamp(
                requestedDurationMultiplier,
                0.0D,
                config.qualityFoodMaximumDurationMultiplier.getAsDouble());
        double strengthMultiplier = Math.min(
                config.qualityFoodMaximumStrengthMultiplier.getAsDouble(),
                1.0D + level * config.qualityFoodStrengthPerLevel.getAsDouble());
        List<SurvivalModifier> modifiers = analysis.modifiers().stream()
                .map(modifier -> new SurvivalModifier(
                        modifier.id(),
                        modifier.target(),
                        modifier.amount() * strengthMultiplier,
                        modifier.operation()))
                .toList();
        return new MealAnalysis(
                analysis.score() + level,
                analysis.durationBiologicalHours() * durationMultiplier,
                analysis.recipeComplexity(),
                analysis.traits(),
                modifiers,
                level);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static Object getComponent(ItemStack stack, DataComponentType<?> componentType) {
        return stack.get((DataComponentType) componentType);
    }

    private static Object invoke(Object owner, String methodName)
            throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        Method method = owner.getClass().getMethod(methodName);
        return method.invoke(owner);
    }
}
