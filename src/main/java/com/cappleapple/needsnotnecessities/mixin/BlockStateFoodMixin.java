package com.cappleapple.needsnotnecessities.mixin;

import com.cappleapple.needsnotnecessities.survival.food.PlacedFoodConsumption;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(BlockBehaviour.BlockStateBase.class)
abstract class BlockStateFoodMixin {
    @WrapMethod(method = "useWithoutItem")
    private InteractionResult needsNotNecessities$eatPlacedFood(Level level, Player player, BlockHitResult hit,
            Operation<InteractionResult> original) {
        return PlacedFoodConsumption.interact(player, (BlockState) (Object) this,
                () -> original.call(level, player, hit));
    }

    @WrapMethod(method = "useItemOn")
    private ItemInteractionResult needsNotNecessities$usePlacedFood(ItemStack stack, Level level, Player player,
            InteractionHand hand, BlockHitResult hit, Operation<ItemInteractionResult> original) {
        return PlacedFoodConsumption.interact(player, (BlockState) (Object) this,
                () -> original.call(stack, level, player, hand, hit));
    }
}
