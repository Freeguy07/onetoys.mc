package com.onetoys.salt.block;

import com.onetoys.salt.Dissolving;
import com.onetoys.salt.Melting;
import com.onetoys.salt.SaltBlocks;
import com.onetoys.salt.SaltConfiguration;
import com.onetoys.salt.SaltTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.PointedDripstoneBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import org.jetbrains.annotations.NotNull;


public interface ISaltBlock {
    @NotNull BlockState getDissolvedState(BlockState state, ServerLevel level, BlockPos pos, Fluid fluid);

    default boolean onSaltRandomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!SaltConfiguration.isLoaded()) {
            return true;
        }
        for (Direction direction : net.minecraft.Util.shuffledCopy(Direction.values(), random)) {
            BlockPos adjacentPos = pos.relative(direction);
            BlockState adjacentState = level.getBlockState(adjacentPos);

            if (Melting.tryMeltFromBlock(adjacentPos, level))
                break; // Melting only one block per random tick

            if (direction != Direction.DOWN && Dissolving.maybeDissolve(state, pos, adjacentState, adjacentPos, level))
                return false;
        }

        if (Dissolving.maybeDissolveInRain(state, level, pos))
            return false;

        return true;
    }

    default void onSaltAnimateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (SaltConfiguration.isLoaded() && random.nextInt(3) == 0 && level.isRainingAt(pos.above())) {
            Direction direction = Direction.getRandom(random);
            if (direction != Direction.UP) {
                BlockPos blockpos = pos.relative(direction);
                BlockState blockstate = level.getBlockState(blockpos);
                if (!state.canOcclude() || !blockstate.isFaceSturdy(level, blockpos, direction.getOpposite())) {
                    double d0 = direction.getStepX() == 0 ? random.nextDouble() : 0.5D + (double) direction.getStepX() * 0.65D;
                    double d1 = direction.getStepY() == 0 ? random.nextDouble() : 0.5D + (double) direction.getStepY() * 0.65D;
                    double d2 = direction.getStepZ() == 0 ? random.nextDouble() : 0.5D + (double) direction.getStepZ() * 0.65D;
                    level.addParticle(ParticleTypes.SPIT, (double) pos.getX() + d0, (double) pos.getY() + d1, (double) pos.getZ() + d2, 0.0D, 0.0D, 0.0D);
                }
            }
        }
    }

    static void maybeGrowCluster(BlockState baseState, BlockPos basePos, ServerLevel level) {
        if (SaltConfiguration.isLoaded()
                && baseState.is(SaltTags.Blocks.SALT_CLUSTER_GROWABLES)
                && SaltConfiguration.SALT_CLUSTER_GROWING_ENABLED.get()
                && level.random.nextFloat() < SaltConfiguration.SALT_CLUSTER_GROWING_CHANCE.get()) {
            BlockPos clusterPos = basePos.above();
            if (canGrowCluster(clusterPos, level)) {
                Fluid drippingFluid = getFluidDrippingOn(level, clusterPos);

                if (drippingFluid == Fluids.WATER)
                    growCluster(baseState, basePos, level);
                else if (drippingFluid != Fluids.EMPTY)
                    level.destroyBlock(clusterPos, false);
            }
        }
    }

    static Fluid getFluidDrippingOn(ServerLevel level, BlockPos pos) {
        BlockPos blockpos = PointedDripstoneBlock.findStalactiteTipAboveCauldron(level, pos);
        return blockpos == null ? Fluids.EMPTY : PointedDripstoneBlock.getCauldronFillFluidType(level, blockpos);
    }

    static boolean canGrowCluster(BlockPos clusterPos, ServerLevel level) {
        if (!level.getBlockState(clusterPos.below()).is(SaltTags.Blocks.SALT_CLUSTER_GROWABLES))
            return false;

        BlockState blockStateAbove = level.getBlockState(clusterPos);
        return blockStateAbove.isAir() || (blockStateAbove.getBlock() instanceof SaltClusterBlock && !blockStateAbove.is(SaltBlocks.SALT_CLUSTER.get()));
    }

    static void growCluster(BlockState state, BlockPos basePos, ServerLevel level) {
        BlockPos clusterPos = basePos.above();
        BlockState prevState = level.getBlockState(clusterPos);
        BlockState clusterState;
        if (prevState.isAir())
            clusterState = SaltBlocks.SMALL_SALT_BUD.get().defaultBlockState();
        else if (prevState.is(SaltBlocks.SMALL_SALT_BUD.get()))
            clusterState = SaltBlocks.MEDIUM_SALT_BUD.get().defaultBlockState();
        else if (prevState.is(SaltBlocks.MEDIUM_SALT_BUD.get()))
            clusterState = SaltBlocks.LARGE_SALT_BUD.get().defaultBlockState();
        else
            clusterState = SaltBlocks.SALT_CLUSTER.get().defaultBlockState();

        level.setBlockAndUpdate(clusterPos, clusterState);
    }
}
