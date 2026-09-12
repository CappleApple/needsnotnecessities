package com.cappleapple.needsnotnecessities.survival.food;

import com.cappleapple.needsnotnecessities.NeedsNotNecessities;
import java.lang.reflect.Method;
import java.util.List;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CakeBlock;
import net.minecraft.world.level.block.CandleCakeBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

/** Reads the serving declared by a food block, including Farmer's Delight subclasses. */
public final class PlacedFoodResolver {
    private static final TagKey<Item> PLACED_FOODS = TagKey.create(Registries.ITEM, NeedsNotNecessities.id("placed_foods"));
    private static final FoodProperties CAKE_BITE = new FoodProperties.Builder().nutrition(2).saturationModifier(0.1F).build();
    private static final ClassValue<ServingMethods> METHODS = new ClassValue<>() {
        @Override
        protected ServingMethods computeValue(Class<?> type) {
            try {
                for (Class<?> parent = type; parent != null; parent = parent.getSuperclass()) {
                    if (parent.getName().equals("vectorwing.farmersdelight.common.block.PieBlock")) {
                        return new ServingMethods(type.getMethod("getPieSliceItem"), null);
                    }
                    if (parent.getName().equals("vectorwing.farmersdelight.common.block.FeastBlock")) {
                        return new ServingMethods(type.getMethod("getServingItem", BlockState.class),
                                type.getMethod("getServingsProperty"));
                    }
                }
            } catch (ReflectiveOperationException exception) {
                NeedsNotNecessities.LOGGER.warn("Cannot resolve placed food servings for {}", type.getName(), exception);
            }
            return new ServingMethods(null, null);
        }
    };

    private PlacedFoodResolver() {
    }

    public static boolean isPlacedFood(ItemStack stack) {
        return stack.is(PLACED_FOODS) || stack.getItem() instanceof BlockItem item && isPlacedFood(item.getBlock());
    }

    private static boolean isPlacedFood(Block block) {
        return block instanceof CakeBlock || block instanceof CandleCakeBlock || METHODS.get(block.getClass()).serving() != null;
    }

    public static ItemStack serving(BlockState state) {
        Block block = state.getBlock();
        if (block instanceof CandleCakeBlock) {
            return new ItemStack(Items.CAKE);
        }
        ServingMethods methods = METHODS.get(block.getClass());
        if (methods.serving() != null) {
            try {
                if (methods.property() == null) {
                    return ((ItemStack) methods.serving().invoke(block)).copy();
                }
                IntegerProperty property = (IntegerProperty) methods.property().invoke(block);
                if (state.getValue(property) <= 0) {
                    return ItemStack.EMPTY;
                }
                return ((ItemStack) methods.serving().invoke(block, state)).copy();
            } catch (ReflectiveOperationException | ClassCastException exception) {
                NeedsNotNecessities.LOGGER.warn("Cannot read placed food serving for {}", block, exception);
            }
        }
        return new ItemStack(block);
    }

    public static List<ItemStack> servings(Block block) {
        if (!isPlacedFood(block)) {
            return List.of();
        }
        return block.getStateDefinition().getPossibleStates().stream()
                .map(PlacedFoodResolver::serving).filter(stack -> !stack.isEmpty()).toList();
    }

    public static ItemStack analysisStack(ItemStack stack) {
        if (stack.getItem() instanceof BlockItem item && METHODS.get(item.getBlock().getClass()).serving() != null) {
            return serving(item.getBlock().defaultBlockState());
        }
        return stack;
    }

    /** Nutrition displayed for one bite/serving, never for an entire multi-serving block. */
    public static FoodProperties foodProperties(ItemStack stack, LivingEntity eater) {
        FoodProperties food = stack.getFoodProperties(eater);
        if (food != null || !(stack.getItem() instanceof BlockItem item)) {
            return food;
        }
        if (item.getBlock() instanceof CakeBlock || item.getBlock() instanceof CandleCakeBlock) {
            return CAKE_BITE;
        }
        ItemStack serving = serving(item.getBlock().defaultBlockState());
        return serving.isEmpty() ? null : serving.getFoodProperties(eater);
    }

    private record ServingMethods(Method serving, Method property) {
    }
}
