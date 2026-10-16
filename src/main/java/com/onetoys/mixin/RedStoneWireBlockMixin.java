package com.onetoys.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.onetoys.BluestoneWireBlock;
import com.onetoys.DustShapes;
import com.onetoys.DustSignals;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.RedStoneWireBlock;
import net.minecraft.world.level.block.state.BlockState;

/** Keeps redstone dust from connecting to, or taking power from, bluestone dust. */
@Mixin(RedStoneWireBlock.class)
public class RedStoneWireBlockMixin {
    @Inject(method = "shouldConnectTo(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/Direction;)Z",
            at = @At("HEAD"), cancellable = true)
    private static void Onetoys$ignoreBluestone(BlockState state, Direction direction, CallbackInfoReturnable<Boolean> cir) {
        if (state.getBlock() instanceof BluestoneWireBlock) {
            cir.setReturnValue(false);
        }
    }

    @WrapOperation(method = "calculateTargetStrength",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;getBestNeighborSignal(Lnet/minecraft/core/BlockPos;)I"), require = 0)
    private int Onetoys$ignoreBluestonePower(Level level, BlockPos pos, Operation<Integer> original) {
        return DustSignals.bestNeighborSignal(level, pos, state -> state.getBlock() instanceof BluestoneWireBlock);
    }

    @ModifyReturnValue(method = "getConnectionState", at = @At("RETURN"), require = 0)
    private BlockState Onetoys$noArmsTowardBluestone(BlockState result, BlockGetter level, BlockState state, BlockPos pos) {
        return DustShapes.stripArmsTowards(level, pos, result, other -> other.getBlock() instanceof BluestoneWireBlock);
    }

    @ModifyReturnValue(method = "getStateForPlacement", at = @At("RETURN"), require = 0)
    private BlockState Onetoys$noArmsTowardBluestonePlacement(BlockState result, net.minecraft.world.item.context.BlockPlaceContext context) {
        return DustShapes.stripArmsTowards(context.getLevel(), context.getClickedPos(), result, other -> other.getBlock() instanceof BluestoneWireBlock);
    }

    @ModifyReturnValue(method = "updateShape", at = @At("RETURN"), require = 0)
    private BlockState Onetoys$noArmsTowardBluestoneUpdateShape(BlockState result, BlockState state, Direction facing, BlockState facingState, LevelAccessor level, BlockPos currentPos, BlockPos facingPos) {
        return DustShapes.stripArmsTowards(level, currentPos, result, other -> other.getBlock() instanceof BluestoneWireBlock);
    }

    @Inject(method = "getSignal", at = @At("HEAD"), cancellable = true, require = 0)
    private void Onetoys$neverSignalBluestone(BlockState blockState, BlockGetter blockAccess, BlockPos pos, Direction side, CallbackInfoReturnable<Integer> cir) {
        if (side != Direction.DOWN && side != Direction.UP) {
            BlockPos targetPos = pos.relative(side.getOpposite());
            if (blockAccess.getBlockState(targetPos).getBlock() instanceof BluestoneWireBlock) {
                cir.setReturnValue(0);
            }
        }
    }

    @Inject(method = "getDirectSignal", at = @At("HEAD"), cancellable = true, require = 0)
    private void Onetoys$neverDirectSignalBluestone(BlockState blockState, BlockGetter blockAccess, BlockPos pos, Direction side, CallbackInfoReturnable<Integer> cir) {
        if (side != Direction.DOWN && side != Direction.UP) {
            BlockPos targetPos = pos.relative(side.getOpposite());
            if (blockAccess.getBlockState(targetPos).getBlock() instanceof BluestoneWireBlock) {
                cir.setReturnValue(0);
            }
        }
    }
}

