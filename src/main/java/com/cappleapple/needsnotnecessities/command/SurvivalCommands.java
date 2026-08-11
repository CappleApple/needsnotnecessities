package com.cappleapple.needsnotnecessities.command;

import com.cappleapple.needsnotnecessities.NeedsNotNecessities;
import com.cappleapple.needsnotnecessities.api.NeedsNotNecessitiesApi;
import com.cappleapple.needsnotnecessities.data.ModAttachments;
import com.cappleapple.needsnotnecessities.data.PlayerSurvivalData;
import com.cappleapple.needsnotnecessities.survival.comfort.ComfortService;
import com.cappleapple.needsnotnecessities.survival.hunger.HungerService;
import com.cappleapple.needsnotnecessities.survival.meal.MealAnalysis;
import com.cappleapple.needsnotnecessities.survival.meal.MealRecipeAnalyzer;
import com.cappleapple.needsnotnecessities.survival.state.SurvivalStateIds;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import java.util.Locale;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

public final class SurvivalCommands {
    private SurvivalCommands() {
    }

    public static void register(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        var root = dispatcher.register(root());
        dispatcher.register(Commands.literal("nnn").redirect(root));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> root() {
        return Commands.literal(NeedsNotNecessities.MOD_ID)
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("status")
                        .executes(context -> status(context.getSource(), context.getSource().getPlayerOrException()))
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(context -> status(context.getSource(), EntityArgument.getPlayer(context, "player")))))
                .then(stateCommands("hunger"))
                .then(stateCommands("thirst"))
                .then(stateCommands("rest"))
                .then(Commands.literal("comfort")
                        .then(Commands.literal("scan")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(context -> comfortScan(
                                                context.getSource(), EntityArgument.getPlayer(context, "player"))))))
                .then(Commands.literal("meal")
                        .then(Commands.literal("inspect")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(context -> mealInspect(
                                                context.getSource(), EntityArgument.getPlayer(context, "player")))))
                        .then(Commands.literal("analyze")
                                .executes(context -> mealAnalyze(
                                        context.getSource(), context.getSource().getPlayerOrException()))))
                .then(Commands.literal("reset")
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(context -> reset(
                                        context.getSource(), EntityArgument.getPlayer(context, "player")))))
                .then(Commands.literal("reload")
                        .executes(context -> reload(context.getSource())));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> stateCommands(String system) {
        return Commands.literal(system)
                .then(Commands.literal("set")
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("state_or_hours", StringArgumentType.word())
                                        .executes(context -> setState(
                                                context.getSource(),
                                                system,
                                                EntityArgument.getPlayer(context, "player"),
                                                StringArgumentType.getString(context, "state_or_hours"))))))
                .then(Commands.literal("add")
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("hours", DoubleArgumentType.doubleArg(-1000000.0D, 1000000.0D))
                                        .executes(context -> addState(
                                                context.getSource(),
                                                system,
                                                EntityArgument.getPlayer(context, "player"),
                                                DoubleArgumentType.getDouble(context, "hours"))))));
    }

    private static int status(CommandSourceStack source, ServerPlayer player) {
        PlayerSurvivalData data = player.getData(ModAttachments.PLAYER_SURVIVAL);
        source.sendSuccess(() -> Component.literal("Survival status for " + player.getGameProfile().getName()), false);
        source.sendSuccess(() -> Component.literal(String.format(
                Locale.ROOT, "Hunger: %s (%.3f h)", NeedsNotNecessitiesApi.getHungerState(player).displayName(), NeedsNotNecessitiesApi.getHungerHours(player))), false);
        source.sendSuccess(() -> Component.literal(String.format(
                Locale.ROOT, "Thirst: %s (%.3f h)", NeedsNotNecessitiesApi.getThirstState(player).displayName(), NeedsNotNecessitiesApi.getThirstHours(player))), false);
        source.sendSuccess(() -> Component.literal(String.format(
                Locale.ROOT, "Rest: %s (%.3f h)", NeedsNotNecessitiesApi.getRestState(player).displayName(), NeedsNotNecessitiesApi.getRestHours(player))), false);
        source.sendSuccess(() -> Component.literal(String.format(
                Locale.ROOT, "Comfort: %.3f (%d ticks retained)", data.retainedComfort(), data.comfortRetentionTicks())), false);
        source.sendSuccess(() -> Component.literal(data.activeMeal()
                .map(meal -> String.format(Locale.ROOT, "Active Meal: %s, score %.3f, %.3f h remaining", meal.displayName(), meal.score(), meal.remainingBiologicalHours()))
                .orElse("Active Meal: none")), false);
        return 1;
    }

    private static int setState(CommandSourceStack source, String system, ServerPlayer player, String value) {
        boolean success;
        try {
            double hours = Double.parseDouble(value);
            success = NeedsNotNecessitiesApi.setStateHours(player, systemId(system), hours);
        } catch (NumberFormatException ignored) {
            success = switch (system) {
                case "hunger" -> NeedsNotNecessitiesApi.setHungerState(player, value);
                case "thirst" -> NeedsNotNecessitiesApi.setThirstState(player, value);
                case "rest" -> NeedsNotNecessitiesApi.setRestState(player, value);
                default -> false;
            };
        }
        if (!success) {
            source.sendFailure(Component.literal("Unknown/cancelled state or disabled module: " + value));
            return 0;
        }
        source.sendSuccess(() -> Component.literal("Updated " + system + " for " + player.getGameProfile().getName()), true);
        return 1;
    }

    private static int addState(CommandSourceStack source, String system, ServerPlayer player, double amount) {
        boolean success = switch (system) {
            case "hunger" -> NeedsNotNecessitiesApi.addHungerHours(player, amount);
            case "thirst" -> NeedsNotNecessitiesApi.adjustThirst(player, amount);
            case "rest" -> NeedsNotNecessitiesApi.advanceRest(player, amount);
            default -> false;
        };
        if (!success) {
            source.sendFailure(Component.literal("Adjustment was cancelled or the module is disabled"));
            return 0;
        }
        source.sendSuccess(() -> Component.literal(String.format(Locale.ROOT, "Adjusted %s by %.3f biological hours", system, amount)), true);
        return 1;
    }

    private static int comfortScan(CommandSourceStack source, ServerPlayer player) {
        ComfortService.ComfortScanResult result = ComfortService.scan(player);
        source.sendSuccess(() -> Component.literal(String.format(Locale.ROOT, "Comfort total: %.3f", result.totalComfort())), false);
        result.totalsByType().forEach((type, total) -> source.sendSuccess(
                () -> Component.literal(String.format(Locale.ROOT, "  %s: %.3f", type, total)), false));
        result.contributors().stream().limit(32).forEach(contributor -> source.sendSuccess(
                () -> Component.literal(String.format(
                        Locale.ROOT,
                        "    %s at %s: %.3f -> %.3f",
                        contributor.definitionId(), contributor.position().toShortString(), contributor.baseComfort(), contributor.appliedComfort())),
                false));
        return (int) Math.round(result.totalComfort());
    }

    private static int mealInspect(CommandSourceStack source, ServerPlayer player) {
        var meal = NeedsNotNecessitiesApi.getActiveMeal(player);
        if (meal.isEmpty()) {
            source.sendFailure(Component.literal("Player has no Active Meal"));
            return 0;
        }
        var value = meal.get();
        source.sendSuccess(() -> Component.literal(String.format(
                Locale.ROOT,
                "%s: score %.3f, complexity %d, %.3f biological hours, quality %.3f",
                value.displayName(), value.score(), value.recipeComplexity(), value.remainingBiologicalHours(), value.qualityValue())), false);
        source.sendSuccess(() -> Component.literal("Traits: " + value.traits()), false);
        value.modifiers().forEach(modifier -> source.sendSuccess(() -> Component.literal("  " + modifier), false));
        return 1;
    }

    private static int mealAnalyze(CommandSourceStack source, ServerPlayer player) {
        ItemStack stack = player.getMainHandItem();
        FoodProperties food = stack.getFoodProperties(player);
        if (stack.isEmpty() || food == null) {
            source.sendFailure(Component.literal("Hold a food item in the main hand"));
            return 0;
        }
        double foodHours = HungerService.calculateFoodHours(food);
        MealAnalysis analysis = MealRecipeAnalyzer.analyze(player, stack, foodHours);
        source.sendSuccess(() -> Component.literal(String.format(
                Locale.ROOT,
                "%s: food %.3f h, score %.3f, complexity %d, duration %.3f h, quality %.3f",
                stack.getHoverName().getString(), foodHours, analysis.score(), analysis.recipeComplexity(), analysis.durationBiologicalHours(), analysis.qualityValue())), false);
        source.sendSuccess(() -> Component.literal("Traits: " + analysis.traits()), false);
        analysis.modifiers().forEach(modifier -> source.sendSuccess(() -> Component.literal("  " + modifier), false));
        return 1;
    }

    private static int reset(CommandSourceStack source, ServerPlayer player) {
        NeedsNotNecessitiesApi.resetToNeutral(player);
        source.sendSuccess(() -> Component.literal("Reset survival data for " + player.getGameProfile().getName()), true);
        return 1;
    }

    private static int reload(CommandSourceStack source) {
        var server = source.getServer();
        server.reloadResources(server.getPackRepository().getSelectedIds())
                .thenRun(() -> source.sendSuccess(() -> Component.literal("Reloaded survival datapack definitions"), true));
        return 1;
    }

    private static net.minecraft.resources.ResourceLocation systemId(String system) {
        return switch (system) {
            case "hunger" -> SurvivalStateIds.HUNGER;
            case "thirst" -> SurvivalStateIds.THIRST;
            case "rest" -> SurvivalStateIds.REST;
            default -> throw new IllegalArgumentException("Unknown state system " + system);
        };
    }
}
