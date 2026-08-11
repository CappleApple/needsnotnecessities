package com.cappleapple.needsnotnecessities.client;

import com.cappleapple.needsnotnecessities.config.ServerConfig;
import com.cappleapple.needsnotnecessities.modifier.SurvivalModifier;
import com.cappleapple.needsnotnecessities.survival.SurvivalModule;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

public final class ClientTooltipService {
    private static final Map<String, Pattern> COMFORT_PATTERN_CACHE = new ConcurrentHashMap<>();

    private ClientTooltipService() {
    }

    public static void addTooltips(ItemTooltipEvent event) {
        addComfortTooltip(event);
        ItemStack stack = event.getItemStack();
        FoodProperties food = stack.getFoodProperties(event.getEntity());
        if (food == null) {
            return;
        }
        double hungerPointHours = ClientSurvivalCache.number(
                "hours_per_hunger_point", ServerConfig.INSTANCE.hoursPerHungerPoint.getAsDouble());
        double saturationPointHours = ClientSurvivalCache.number(
                "hours_per_saturation_point", ServerConfig.INSTANCE.hoursPerSaturationPoint.getAsDouble());
        double foodHours = food.nutrition() * hungerPointHours + food.saturation() * saturationPointHours;
        boolean advanced = event.getFlags().isAdvanced();
        if (ClientSurvivalCache.enabled(SurvivalModule.HUNGER)) {
            CompoundTag group = foodGroup(foodHours);
            String groupName = group == null ? "Food" : group.getString("name");
            String flavorText = group == null || group.getString("description").isBlank()
                    ? groupName
                    : group.getString("description");
            int color = group == null ? 0xE8E8E8 : group.getInt("color");
            event.getToolTip().add(Component.literal(flavorText)
                    .withStyle(style -> style.withColor(color)));
            if (advanced) {
                event.getToolTip().add(debug(String.format(
                        Locale.ROOT, "  Hunger restoration: %.3f hours", foodHours)));
            }
        }
        if (advanced && ClientSurvivalCache.enabled(SurvivalModule.THIRST)) {
            double thirstHours = foodHours * ClientSurvivalCache.number(
                    "thirst_hours_per_food_hour", ServerConfig.INSTANCE.thirstHoursPerFoodHour.getAsDouble());
            event.getToolTip().add(debug(String.format(
                    Locale.ROOT, "  Thirst cost: %.3f hours", thirstHours)));
        }
        if (Screen.hasShiftDown() && ClientSurvivalCache.enabled(SurvivalModule.ACTIVE_MEAL)) {
            ClientMealPreviewService.Preview preview = ClientMealPreviewService.analyze(stack, foodHours);
            event.getToolTip().add(Component.literal(String.format(
                    Locale.ROOT, "Active Meal effects (%.1f hours):", preview.durationHours()))
                    .withStyle(ChatFormatting.GOLD));
            for (SurvivalModifier modifier : preview.modifiers()) {
                event.getToolTip().add(Component.literal("  " + formatModifier(modifier))
                        .withStyle(ModifierTextColor.forAmount(modifier.amount())));
            }
            if (advanced) {
                double baseScore = foodHours + preview.recipeComplexity() * ClientSurvivalCache.number(
                        "meal_score_per_complexity", ServerConfig.INSTANCE.mealScorePerComplexity.getAsDouble());
                event.getToolTip().add(debug("  Detected recipe complexity: " + preview.recipeComplexity()));
                event.getToolTip().add(debug(String.format(
                        Locale.ROOT, "  Base meal score before datapack trait votes: %.3f", baseScore)));
            }
        }
        if (advanced) {
            event.getToolTip().add(Component.literal("Needs, Not Necessities debug:")
                    .withStyle(ChatFormatting.DARK_GRAY));
            event.getToolTip().add(debug(String.format(Locale.ROOT, "  Hunger points: %d", food.nutrition())));
            event.getToolTip().add(debug(String.format(Locale.ROOT, "  Saturation points: %.3f", food.saturation())));
        }
    }

    private static void addComfortTooltip(ItemTooltipEvent event) {
        if (!ClientSurvivalCache.enabled(SurvivalModule.COMFORT)
                || !(event.getItemStack().getItem() instanceof BlockItem blockItem)) {
            return;
        }
        BlockState state = blockItem.getBlock().defaultBlockState();
        ResourceLocation blockId = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(blockItem.getBlock());
        List<ComfortTooltipMatch> matches = new ArrayList<>();
        for (CompoundTag source : ClientSurvivalCache.list("comfort_sources")) {
            ResourceLocation selector = ResourceLocation.tryParse(source.getString("selector"));
            boolean selectorMatch = selector != null && matches(state, source.getString("selector_kind"), selector);
            boolean automaticMatch = matchesAutomatically(blockId, source);
            if (selectorMatch || automaticMatch) {
                matches.add(new ComfortTooltipMatch(source, selectorMatch, automaticMatch));
            }
        }
        boolean hasExplicitMatch = matches.stream().anyMatch(ComfortTooltipMatch::explicit);
        List<ComfortTooltipMatch> selectedMatches = hasExplicitMatch
                ? matches.stream().filter(ComfortTooltipMatch::explicit).toList()
                : matches.isEmpty() ? List.of() : List.of(matches.getFirst());
        for (ComfortTooltipMatch match : selectedMatches) {
            CompoundTag source = match.source();
            String type = source.getString("name");
            if (type.isBlank()) {
                type = titleCase(source.getString("type").replace('_', ' '));
            }
            event.getToolTip().add(Component.literal(String.format(
                    Locale.ROOT,
                    "Comfort - %s: %.0f",
                    type,
                    source.getDouble("comfort"))).withStyle(ChatFormatting.GREEN));
            if (event.getFlags().isAdvanced()) {
                CompoundTag comfort = ClientSurvivalCache.snapshot().getCompound("comfort");
                event.getToolTip().add(debug("  Definition: " + source.getString("id")));
                if (match.automatic() && !match.explicit()) {
                    event.getToolTip().add(debug("  Auto-classified block ID: " + blockId));
                }
                event.getToolTip().add(debug("  Scan radius: " + comfort.getInt("radius")));
                event.getToolTip().add(debug(String.format(
                        Locale.ROOT, "  Same-type diminishing factor: %.3f", comfort.getDouble("diminishing_factor"))));
            }
        }
    }

    private static boolean matches(BlockState state, String selectorKind, ResourceLocation selector) {
        if (selectorKind.equals("block")) {
            return state.is(net.minecraft.core.registries.BuiltInRegistries.BLOCK.getOptional(selector).orElse(null));
        }
        if (selectorKind.equals("tag")) {
            return state.is(TagKey.create(Registries.BLOCK, selector));
        }
        return false;
    }

    private static boolean matchesAutomatically(ResourceLocation blockId, CompoundTag source) {
        if (!source.contains("auto_namespace_regex", Tag.TAG_STRING)
                || !source.contains("auto_path_regex", Tag.TAG_STRING)) {
            return false;
        }
        try {
            if (!pattern(source.getString("auto_namespace_regex"))
                    .matcher(blockId.getNamespace()).matches()
                    || !pattern(source.getString("auto_path_regex"))
                    .matcher(blockId.getPath()).find()) {
                return false;
            }
            return !source.contains("auto_exclude_path_regex", Tag.TAG_STRING)
                    || !pattern(source.getString("auto_exclude_path_regex"))
                    .matcher(blockId.getPath()).find();
        } catch (PatternSyntaxException exception) {
            return false;
        }
    }

    private static Pattern pattern(String regex) {
        return COMFORT_PATTERN_CACHE.computeIfAbsent(regex, Pattern::compile);
    }

    private record ComfortTooltipMatch(CompoundTag source, boolean explicit, boolean automatic) {
    }

    private static Component debug(String value) {
        return Component.literal(value).withStyle(ChatFormatting.DARK_GRAY);
    }

    private static CompoundTag foodGroup(double foodHours) {
        for (CompoundTag group : ClientSurvivalCache.list("food_tooltip_groups")) {
            if (foodHours >= group.getDouble("minimum_hours")
                    && foodHours <= group.getDouble("maximum_hours")) {
                return group;
            }
        }
        return null;
    }

    private static String formatModifier(SurvivalModifier modifier) {
        String target = modifier.target().toString();
        String path = modifier.target().getPath()
                .replace("generic.", "")
                .replace("player.", "")
                .replace('_', ' ');
        path = titleCase(path);
        if (modifier.operation().name().startsWith("MULTIPLY")
                || target.endsWith("passive_regeneration")) {
            return String.format(Locale.ROOT, "%+.1f%% %s", modifier.amount() * 100.0D, path);
        }
        return String.format(Locale.ROOT, "%+.2f %s", modifier.amount(), path);
    }

    private static String titleCase(String value) {
        StringBuilder result = new StringBuilder();
        for (String word : value.split(" ")) {
            if (!word.isBlank()) {
                if (!result.isEmpty()) {
                    result.append(' ');
                }
                result.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
            }
        }
        return result.toString();
    }
}
