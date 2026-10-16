package com.onetoys.mixin.salt;

import com.onetoys.salt.SaltConfiguration;
import com.onetoys.salt.SaltTags;
import com.onetoys.salt.block.ISaltBlock;
import com.onetoys.salt.block.SaltClusterBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.LevelEvent;
import net.minecraft.world.level.block.PointedDripstoneBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

@Mixin(PointedDripstoneBlock.class)
public abstract class PointedDripstoneBlockMixin {
    @Invoker("canDripThrough")
    public static boolean invokeCanDripThrough(BlockGetter level, BlockPos pos, BlockState state) {
        throw new AssertionError();
    }

    @Inject(method = "maybeTransferFluid",
            at = @At(value = "INVOKE_ASSIGN",
                    target = "Lnet/minecraft/world/level/block/PointedDripstoneBlock;findFillableCauldronBelowStalactiteTip(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/material/Fluid;)Lnet/minecraft/core/BlockPos;",
                    shift = At.Shift.AFTER),
            locals = LocalCapture.CAPTURE_FAILSOFT)
    private static void onMaybeTransferFluid(BlockState pState, ServerLevel level, BlockPos pos, float pRandChance,
                                             CallbackInfo ci,
                                             Optional<?> optional,
                                             Fluid fluid, float f,
                                             FluidType.DripstoneDripInfo dripInfo,
                                             BlockPos tipPos, BlockPos cauldronPos) {
        if (fluid != Fluids.WATER && fluid != Fluids.LAVA) {
            return;
        }

        if (cauldronPos != null || !SaltConfiguration.isLoaded() || !SaltConfiguration.SALT_CLUSTER_GROWING_ENABLED.get()) {
            return;
        }

        @Nullable BlockPos growablePos = salt$findSaltGrowablesBelowStalactiteTip(level, tipPos, fluid);

        if (growablePos != null) {
            level.levelEvent(LevelEvent.DRIPSTONE_DRIP, tipPos, 0);
            int distanceToGrowable = tipPos.getY() - growablePos.getY();
            int delay = 50 + distanceToGrowable;
            BlockState growableState = level.getBlockState(growablePos);

            if (growableState.getBlock() instanceof ISaltBlock) {
                level.scheduleTick(growablePos, growableState.getBlock(), delay);
            } else {
                ISaltBlock.maybeGrowCluster(growableState, growablePos, level);
            }
        }
    }

    @Unique
    private static @Nullable BlockPos salt$findSaltGrowablesBelowStalactiteTip(Level pLevel, BlockPos pPos, Fluid pFluid) {
        Predicate<BlockState> statePredicate = (state) ->
                state.is(SaltTags.Blocks.SALT_CLUSTER_GROWABLES) || (state.getBlock() instanceof SaltClusterBlock && state.getValue(SaltClusterBlock.FACING) == Direction.UP);
        BiPredicate<BlockPos, BlockState> canDripThroughPredicate = (pos, state) ->
                invokeCanDripThrough(pLevel, pos, state);
        return salt$findBlockVertical(pLevel, pPos, Direction.DOWN.getAxisDirection(), canDripThroughPredicate, statePredicate, 11).orElse(null);
    }

    @Unique
    private static Optional<BlockPos> salt$findBlockVertical(LevelAccessor pLevel, BlockPos pPos, Direction.AxisDirection pAxis,
                                                             BiPredicate<BlockPos, BlockState> canDripThroughPredicate,
                                                             Predicate<BlockState> statePredicate, int maxIterations) {
        Direction direction = Direction.get(pAxis, Direction.Axis.Y);
        BlockPos.MutableBlockPos pos = pPos.mutable();

        for (int i = 1; i < maxIterations; ++i) {
            pos.move(direction);
            BlockState state = pLevel.getBlockState(pos);
            if (statePredicate.test(state)) {
                return Optional.of(pos.immutable());
            }

            if (pLevel.isOutsideBuildHeight(pos.getY()) || !canDripThroughPredicate.test(pos, state)) {
                return Optional.empty();
            }
        }

        return Optional.empty();
    }
}

