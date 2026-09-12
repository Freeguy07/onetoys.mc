package com.orehall.mixin;

import com.orehall.module.gameplay.sleep.RespawnAnchorTweaks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.dispenser.BlockSource;
import net.minecraft.core.dispenser.DefaultDispenseItemBehavior;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.RespawnAnchorBlock;
import net.minecraft.world.level.block.entity.DispenserBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(DispenserBlock.class)
public abstract class DispenserBlockMixin {

    @Unique
    private static final DefaultDispenseItemBehavior OREHALL$DEFAULT_DISPENSE = new DefaultDispenseItemBehavior();

    @Inject(method = "dispenseFrom", at = @At("HEAD"), cancellable = true)
    private void orehall$dynamicRespawnAnchorFuelDispense(ServerLevel level, BlockState state, BlockPos pos, CallbackInfo ci) {
        if (!RespawnAnchorTweaks.isTweakEnabled()) {
            return;
        }

        Direction facing = state.getValue(DispenserBlock.FACING);
        BlockPos targetPos = pos.relative(facing);
        BlockState targetState = level.getBlockState(targetPos);

        if (!targetState.is(Blocks.RESPAWN_ANCHOR)) {
            return;
        }

        if (!(level.getBlockEntity(pos) instanceof DispenserBlockEntity dispenserBlockEntity)) {
            return;
        }

        int slot = dispenserBlockEntity.getRandomSlot(level.random);
        if (slot < 0) {
            level.levelEvent(1001, pos, 0);
            level.gameEvent(GameEvent.BLOCK_ACTIVATE, pos, GameEvent.Context.of(dispenserBlockEntity.getBlockState()));
            ci.cancel();
            return;
        }

        ItemStack stack = dispenserBlockEntity.getItem(slot);
        if (stack.isEmpty()) {
            return;
        }

        // 1. If stack is valid configured fuel (e.g. echo shards, tags, or any configured item)
        if (RespawnAnchorTweaks.isValidFuel(stack)) {
            int currentCharge = targetState.getValue(RespawnAnchorBlock.CHARGE);
            if (currentCharge < 4) {
                RespawnAnchorBlock.charge(null, level, targetPos, targetState);
                stack.shrink(1);
                dispenserBlockEntity.setItem(slot, stack);
                level.levelEvent(1000, pos, 0); // SOUND_DISPENSER_DISPENSE
                level.gameEvent(GameEvent.BLOCK_ACTIVATE, pos, GameEvent.Context.of(dispenserBlockEntity.getBlockState()));
            } else {
                level.levelEvent(1001, pos, 0); // SOUND_DISPENSER_FAIL
                level.gameEvent(GameEvent.BLOCK_ACTIVATE, pos, GameEvent.Context.of(dispenserBlockEntity.getBlockState()));
            }
            ci.cancel();
            return;
        }

        // 2. If stack is Glowstone, but Glowstone is NOT valid fuel, prevent vanilla's hardcoded Glowstone behavior
        // from charging the anchor; instead drop the item via default dispenser behavior.
        if (stack.is(Items.GLOWSTONE)) {
            BlockSource blocksource = new BlockSource(level, pos, state, dispenserBlockEntity);
            dispenserBlockEntity.setItem(slot, OREHALL$DEFAULT_DISPENSE.dispense(blocksource, stack));
            ci.cancel();
        }
    }
}
