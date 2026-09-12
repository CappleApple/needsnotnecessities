package com.cappleapple.needsnotnecessities.gametest;

import com.cappleapple.needsnotnecessities.NeedsNotNecessities;
import com.cappleapple.needsnotnecessities.api.event.FoodConsumedEvent;
import com.cappleapple.needsnotnecessities.config.ServerConfig;
import com.cappleapple.needsnotnecessities.data.ModAttachments;
import com.cappleapple.needsnotnecessities.data.PlayerSurvivalData;
import com.cappleapple.needsnotnecessities.survival.food.PlacedFoodConsumption;
import com.cappleapple.needsnotnecessities.survival.food.PlacedFoodResolver;
import com.cappleapple.needsnotnecessities.survival.hunger.HungerService;
import com.cappleapple.needsnotnecessities.survival.meal.MealIngredientResolver;
import com.cappleapple.needsnotnecessities.survival.meal.MealRecipeAnalyzer;
import com.cappleapple.needsnotnecessities.survival.meal.MealRecipeIndex;
import com.cappleapple.needsnotnecessities.survival.state.StateDefinitionManager;
import com.cappleapple.needsnotnecessities.survival.state.StateTrackService;
import com.cappleapple.needsnotnecessities.survival.state.SurvivalStateIds;
import com.mojang.authlib.GameProfile;
import io.netty.channel.embedded.EmbeddedChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.PacketSendListener;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CakeBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(NeedsNotNecessities.MOD_ID)
@PrefixGameTestTemplate(false)
public final class PlacedFoodGameTests {
    private static final String EMPTY = "bastion/mobs/empty";

    @GameTest(templateNamespace = "minecraft", template = EMPTY)
    public static void cakeBitesUseActualNutritionAndConsumeExactlyOnce(GameTestHelper helper) {
        try (Fixture fixture = new Fixture(helper)) {
            fixture.place(Blocks.CAKE.defaultBlockState());
            for (int bite = 0; bite < 7; bite++) {
                fixture.reset();
                helper.getLevel().getBlockState(fixture.pos).useWithoutItem(helper.getLevel(), fixture.player, fixture.hit);
                fixture.assertFood(new ItemStack(Items.CAKE), 2, 0.4F);
                if (bite < 6) {
                    helper.assertTrue(helper.getLevel().getBlockState(fixture.pos).getValue(CakeBlock.BITES) == bite + 1,
                            "Each successful click must remove exactly one cake bite");
                }
            }
            helper.assertTrue(helper.getLevel().getBlockState(fixture.pos).isAir(), "The final cake bite must remove the block");
        }
        helper.succeed();
    }

    @GameTest(templateNamespace = "minecraft", template = EMPTY)
    public static void candleCakeTracksCakeRecipe(GameTestHelper helper) {
        try (Fixture fixture = new Fixture(helper)) {
            fixture.place(Blocks.CANDLE_CAKE.defaultBlockState());
            helper.getLevel().getBlockState(fixture.pos).useWithoutItem(helper.getLevel(), fixture.player, fixture.hit);
            fixture.assertFood(new ItemStack(Items.CAKE), 2, 0.4F);
        }
        helper.succeed();
    }

    @GameTest(templateNamespace = "minecraft", template = EMPTY)
    public static void deniedBitesAndNonFoodInteractionsDoNotConsume(GameTestHelper helper) {
        try (Fixture fixture = new Fixture(helper)) {
            fixture.place(Blocks.CAKE.defaultBlockState());
            fixture.data().setStatePosition(SurvivalStateIds.HUNGER,
                    StateDefinitionManager.INSTANCE.require(SurvivalStateIds.HUNGER).totalHours());
            helper.getLevel().getBlockState(fixture.pos).useWithoutItem(helper.getLevel(), fixture.player, fixture.hit);
            helper.assertTrue(fixture.events.isEmpty(), "Full custom Hunger must reject the bite");
            helper.assertTrue(helper.getLevel().getBlockState(fixture.pos).getValue(CakeBlock.BITES) == 0,
                    "A rejected bite must leave the cake intact");
            fixture.reset();
            fixture.player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.CANDLE));
            helper.getLevel().getBlockState(fixture.pos).useItemOn(fixture.player.getMainHandItem(), helper.getLevel(),
                    fixture.player, InteractionHand.MAIN_HAND, fixture.hit);
            helper.assertTrue(fixture.events.isEmpty(), "Adding a candle must not count as eating");
            fixture.place(Blocks.STONE.defaultBlockState());
            helper.getLevel().getBlockState(fixture.pos).useWithoutItem(helper.getLevel(), fixture.player, fixture.hit);
            helper.assertTrue(fixture.events.isEmpty(), "Ordinary block interactions must not grant food");
        }
        helper.succeed();
    }

    @GameTest(templateNamespace = "minecraft", template = EMPTY)
    public static void heldFoodAndDuplicateBlockFinishEventsCountOnce(GameTestHelper helper) {
        try (Fixture fixture = new Fixture(helper)) {
            ItemStack bread = new ItemStack(Items.BREAD);
            FoodProperties food = bread.getFoodProperties(fixture.player);
            fixture.player.getFoodData().eat(food);
            fixture.finish(bread);
            fixture.assertFood(bread, food.nutrition(), food.saturation());

            fixture.reset();
            PlacedFoodConsumption.interact(fixture.player, Blocks.CAKE.defaultBlockState(), () -> {
                fixture.player.getFoodData().eat(food);
                fixture.finish(bread);
                return true;
            });
            fixture.assertFood(bread, food.nutrition(), food.saturation());

            fixture.reset();
            try {
                PlacedFoodConsumption.interact(fixture.player, Blocks.CAKE.defaultBlockState(), () -> {
                    throw new IllegalStateException("fixture");
                });
            } catch (IllegalStateException expected) {
                helper.assertTrue(expected.getMessage().equals("fixture"), "Unexpected interaction failure");
            }
            fixture.player.getFoodData().eat(food);
            fixture.finish(bread);
            fixture.assertFood(bread, food.nutrition(), food.saturation());
        }
        helper.succeed();
    }

    @GameTest(templateNamespace = "minecraft", template = EMPTY)
    public static void disabledHungerPreservesVanillaEatingAndOtherFoodModules(GameTestHelper helper) {
        ModConfigSpec.BooleanValue hunger = ServerConfig.SPEC.getValues().get(List.of("modules", "hunger"));
        boolean original = hunger.getAsBoolean();
        hunger.set(false);
        try (Fixture fixture = new Fixture(helper)) {
            fixture.place(Blocks.CAKE.defaultBlockState());
            fixture.player.getFoodData().setFoodLevel(10);
            helper.getLevel().getBlockState(fixture.pos).useWithoutItem(helper.getLevel(), fixture.player, fixture.hit);
            helper.assertTrue(fixture.player.getFoodData().getFoodLevel() == 12, "Disabled custom Hunger must preserve vanilla cake nutrition");
            fixture.close(1.0D, fixture.data().statePosition(SurvivalStateIds.HUNGER), "Disabled custom Hunger must stay unchanged");
            helper.assertTrue(fixture.events.size() == 1 && fixture.data().activeMeal().isPresent(),
                    "Other enabled food modules must still process the bite");
            helper.assertTrue(fixture.data().statePosition(SurvivalStateIds.THIRST) < fixture.initialThirst,
                    "Enabled Thirst must still account for the bite");
        } finally {
            hunger.set(original);
        }
        helper.succeed();
    }

    @GameTest(templateNamespace = "minecraft", template = EMPTY)
    public static void placedIngredientsRecurseThroughDeepRecipesAndCycles(GameTestHelper helper) {
        try (Fixture fixture = new Fixture(helper)) {
            RecipeManager manager = new RecipeManager(helper.getLevel().registryAccess());
            manager.replaceRecipes(List.of(
                    recipe("serving", Items.MUSHROOM_STEW, Items.CAKE),
                    recipe("cake", Items.CAKE, Items.COOKIE),
                    recipe("cookie", Items.COOKIE, Items.BREAD),
                    recipe("bread", Items.BREAD, Items.CARROT)));
            MealRecipeIndex index = MealRecipeIndex.build(manager, helper.getLevel().registryAccess());
            helper.assertTrue(new ItemStack(Items.CAKE).getFoodProperties(fixture.player) == null,
                    "The intermediate cake must lack held-food properties");
            assertAncestor(helper, fixture.player, index, Items.MUSHROOM_STEW, Items.CARROT);
            manager.replaceRecipes(List.of(
                    recipe("serving", Items.MUSHROOM_STEW, Items.CAKE),
                    recipe("cake", Items.CAKE, Items.COOKIE),
                    recipe("cookie", Items.COOKIE, Items.CAKE, Items.POTATO)));
            index = MealRecipeIndex.build(manager, helper.getLevel().registryAccess());
            assertAncestor(helper, fixture.player, index, Items.MUSHROOM_STEW, Items.POTATO);
        }
        helper.succeed();
    }

    private static RecipeHolder<?> recipe(String id, Item result, Item... ingredients) {
        NonNullList<Ingredient> inputs = NonNullList.create();
        for (Item ingredient : ingredients) {
            inputs.add(Ingredient.of(ingredient));
        }
        return new RecipeHolder<>(NeedsNotNecessities.id("test/" + id),
                new ShapelessRecipe("", CraftingBookCategory.MISC, new ItemStack(result), inputs));
    }

    @GameTest(templateNamespace = "minecraft", template = EMPTY)
    public static void farmersDelightPiesAndFeastServings(GameTestHelper helper) {
        if (!BuiltInRegistries.ITEM.containsKey(fd("apple_pie"))) {
            helper.assertTrue(!Boolean.getBoolean("nnn.test.farmersDelight"), "Requested Farmer's Delight fixture did not load");
            NeedsNotNecessities.LOGGER.info("Farmer's Delight placed-food checks omitted: optional test JAR not supplied");
            helper.succeed();
            return;
        }
        try (Fixture fixture = new Fixture(helper)) {
            for (String id : List.of("apple_pie", "sweet_berry_cheesecake", "chocolate_pie")) {
                Block block = BuiltInRegistries.BLOCK.get(fd(id));
                helper.assertTrue(block != Blocks.AIR, "Missing pie fixture " + id);
                fixture.place(block.defaultBlockState());
                ItemStack slice = PlacedFoodResolver.serving(block.defaultBlockState());
                FoodProperties food = slice.getFoodProperties(fixture.player);
                for (int bite = 0; bite < 4; bite++) {
                    fixture.reset();
                    helper.getLevel().getBlockState(fixture.pos).useWithoutItem(helper.getLevel(), fixture.player, fixture.hit);
                    fixture.assertFood(slice, food.nutrition(), food.saturation());
                }
                helper.assertTrue(helper.getLevel().getBlockState(fixture.pos).isAir(), "Final pie bite must remove " + id);
            }
            for (String id : List.of("stuffed_pumpkin_block", "roast_chicken_block", "honey_glazed_ham_block", "shepherds_pie_block")) {
                Block block = BuiltInRegistries.BLOCK.get(fd(id));
                helper.assertTrue(block != Blocks.AIR, "Missing feast fixture " + id);
                fixture.place(block.defaultBlockState());
                fixture.reset();
                ItemStack serving = PlacedFoodResolver.serving(block.defaultBlockState());
                ItemStack container = serving.getCraftingRemainingItem();
                fixture.player.setItemInHand(InteractionHand.MAIN_HAND, container);
                block.defaultBlockState().useItemOn(container, helper.getLevel(), fixture.player, InteractionHand.MAIN_HAND, fixture.hit);
                helper.assertTrue(fixture.events.isEmpty(), "Taking a serving must not count as eating: " + id);
                helper.assertTrue(fixture.player.getInventory().contains(new ItemStack(serving.getItem())),
                        "The real feast must hand out its serving: " + id);
                FoodProperties food = serving.getFoodProperties(fixture.player);
                fixture.player.getFoodData().eat(food);
                fixture.finish(serving);
                fixture.assertFood(serving, food.nutrition(), food.saturation());
            }
            MealRecipeIndex index = MealRecipeIndex.build(helper.getLevel().getRecipeManager(), helper.getLevel().registryAccess());
            assertAncestor(helper, fixture.player, index, BuiltInRegistries.ITEM.get(fd("cake_slice")), Items.WHEAT);
            assertAncestor(helper, fixture.player, index, BuiltInRegistries.ITEM.get(fd("apple_pie_slice")), Items.WHEAT);
            assertAncestor(helper, fixture.player, index, BuiltInRegistries.ITEM.get(fd("stuffed_pumpkin")), Items.POTATO);
            assertAncestor(helper, fixture.player, index, BuiltInRegistries.ITEM.get(fd("shepherds_pie")), Items.POTATO);
            // Existing roll recipes must win over the reversible medley serving conversion.
            Item roll = BuiltInRegistries.ITEM.get(fd("salmon_roll"));
            helper.assertTrue(index.ingredientsFor(roll).stream().flatMap(java.util.Arrays::stream)
                    .noneMatch(stack -> stack.is(BuiltInRegistries.ITEM.get(fd("rice_roll_medley_block")))),
                    "Native rice-roll ingredients must retain precedence over the serving fallback");
        }
        helper.succeed();
    }

    private static void assertAncestor(GameTestHelper helper, ServerPlayer player, MealRecipeIndex index, Item food, Item ancestor) {
        helper.assertTrue(food != Items.AIR, "Missing serving fixture");
        MealIngredientResolver<Item> resolver = new MealIngredientResolver<>(
                stack -> List.of(new MealIngredientResolver.Definition<>(BuiltInRegistries.ITEM.getKey(stack.getItem()), stack.getItem())),
                index::ingredientsFor, stack -> stack.getFoodProperties(player) != null || PlacedFoodResolver.isPlacedFood(stack));
        helper.assertTrue(resolver.resolveIngredient(new ItemStack[]{new ItemStack(food)}).groups().stream()
                        .anyMatch(group -> group.definition() == ancestor),
                "Serving recipe must recursively reach " + ancestor + " from " + food);
    }

    private static ResourceLocation fd(String path) {
        return ResourceLocation.fromNamespaceAndPath("farmersdelight", path);
    }

    private static final class Fixture implements AutoCloseable {
        private final GameTestHelper helper;
        private final ServerPlayer player;
        private final EmbeddedChannel channel;
        private final BlockPos pos;
        private final BlockHitResult hit;
        private final List<FoodConsumedEvent> events = new ArrayList<>();
        private final Consumer<FoodConsumedEvent> listener;
        private double initialThirst;

        private Fixture(GameTestHelper helper) {
            this.helper = helper;
            UUID id = UUID.randomUUID();
            player = new ServerPlayer(helper.getLevel().getServer(), helper.getLevel(),
                    new GameProfile(id, "food_" + id.toString().substring(0, 8)), ClientInformation.createDefault());
            Connection connection = new Connection(PacketFlow.SERVERBOUND);
            channel = new EmbeddedChannel(connection);
            new ServerGamePacketListenerImpl(player.server, connection, player,
                    CommonListenerCookie.createInitial(player.getGameProfile(), false)) {
                @Override
                public void send(Packet<?> packet, PacketSendListener listener) {
                }
            };
            pos = helper.absolutePos(new BlockPos(0, 2, 0));
            hit = new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false);
            listener = event -> {
                if (event.player() == player) {
                    events.add(event);
                }
            };
            NeoForge.EVENT_BUS.addListener(listener);
            reset();
        }

        private PlayerSurvivalData data() {
            return player.getData(ModAttachments.PLAYER_SURVIVAL);
        }

        private void reset() {
            StateTrackService.initializeMissingTracks(data());
            data().setStatePosition(SurvivalStateIds.HUNGER, 1.0D);
            initialThirst = StateDefinitionManager.INSTANCE.require(SurvivalStateIds.THIRST).neutralPosition();
            data().setStatePosition(SurvivalStateIds.THIRST, initialThirst);
            data().clearActiveMeal();
            player.getFoodData().setFoodLevel(20);
            player.getFoodData().setSaturation(0.0F);
            player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            events.clear();
        }

        private void place(BlockState state) {
            helper.getLevel().setBlockAndUpdate(pos.below(), Blocks.STONE.defaultBlockState());
            helper.getLevel().setBlockAndUpdate(pos, state);
        }

        private void finish(ItemStack stack) {
            NeoForge.EVENT_BUS.post(new LivingEntityUseItemEvent.Finish(player, stack, 0, ItemStack.EMPTY));
        }

        private void assertFood(ItemStack stack, int nutrition, float saturation) {
            double hours = HungerService.calculateFoodHours(nutrition, saturation,
                    ServerConfig.INSTANCE.hoursPerHungerPoint.getAsDouble(), ServerConfig.INSTANCE.hoursPerSaturationPoint.getAsDouble());
            helper.assertTrue(events.size() == 1, "Consumption must emit exactly one food event, got " + events.size());
            FoodConsumedEvent event = events.getFirst();
            helper.assertTrue(event.stack().is(stack.getItem()), "Food event must identify the actual bite or serving");
            close(nutrition, event.nutrition(), "Nutrition");
            close(saturation, event.saturation(), "Saturation must use points, not its modifier or clamped vanilla delta");
            close(1.0D + hours, data().statePosition(SurvivalStateIds.HUNGER), "Hunger restoration");
            close(initialThirst - hours * ServerConfig.INSTANCE.thirstHoursPerFoodHour.getAsDouble(),
                    data().statePosition(SurvivalStateIds.THIRST), "Thirst cost");
            var expected = MealRecipeAnalyzer.analyze(player, stack, hours);
            var meal = data().activeMeal().orElseThrow();
            helper.assertTrue(meal.modifiers().equals(expected.modifiers()), "Bite and held-serving meal modifiers must match");
            helper.assertTrue(meal.traits().equals(expected.traits()), "Bite and held-serving recursive traits must match");
            close(expected.durationBiologicalHours(), meal.remainingBiologicalHours(), "Meal duration");
        }

        private void close(double expected, double actual, String message) {
            helper.assertTrue(Math.abs(expected - actual) < 0.0001D, message + ": expected " + expected + ", got " + actual);
        }

        @Override
        public void close() {
            NeoForge.EVENT_BUS.unregister(listener);
            channel.finishAndReleaseAll();
        }
    }
}
