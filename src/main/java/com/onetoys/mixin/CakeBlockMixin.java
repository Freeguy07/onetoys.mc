package com.onetoys.mixin;

import com.onetoys.Onetoys;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.CakeBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import com.onetoys.sol_valheim.SOLValheim;
import com.onetoys.sol_valheim.ValheimFoodData;

@Mixin(CakeBlock.class)
public class CakeBlockMixin
{
    @Inject(at = @At("HEAD"), method = "eat", cancellable = true)
    private static void canEatCake(LevelAccessor level, BlockPos pos, BlockState state, Player player, CallbackInfoReturnable<InteractionResult> cir)
    {
        if (!com.onetoys.module.gameplay.FoodOverhaulModule.isFoodOverhaulEnabled()) return;
        if (player.isCreative())
        {
            return;
        }

        ValheimFoodData foodData = player.getData(SOLValheim.FOOD_DATA);
        ItemStack cakeStack = Items.CAKE.getDefaultInstance();
        if (foodData.canEat(cakeStack))
        {
            foodData.eatItem(cakeStack);
            player.removeEffect(MobEffects.HUNGER);
            player.removeEffect(Onetoys.HUNGER_2);
            if (!level.isClientSide())
            {
                player.syncData(SOLValheim.FOOD_DATA);
            }
            return;
        }

        cir.setReturnValue(InteractionResult.PASS);
        cir.cancel();
    }
}
