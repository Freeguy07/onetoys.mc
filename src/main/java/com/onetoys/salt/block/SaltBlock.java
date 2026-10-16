package com.onetoys.salt.block;

import com.mojang.serialization.MapCodec;
import com.onetoys.salt.SaltConfiguration;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import org.jetbrains.annotations.NotNull;

public class SaltBlock extends Block implements ISaltBlock {
    public static final MapCodec<SaltBlock> CODEC = simpleCodec(SaltBlock::new);

    private final BlockState dissolvedState;

    public SaltBlock(BlockState dissolvedState, Properties properties) {
        super(properties);
        this.dissolvedState = dissolvedState;
    }

    public SaltBlock(Properties properties) {
        super(properties);
        this.dissolvedState = Blocks.AIR.defaultBlockState();
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    public @NotNull BlockState getDissolvedState(BlockState originalState, ServerLevel level, BlockPos pos, Fluid fluid) {
        return dissolvedState;
    }

    @Override
    public void animateTick(@NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos, @NotNull RandomSource random) {
        onSaltAnimateTick(state, level, pos, random);
    }

    @Override
    public void randomTick(@NotNull BlockState state, @NotNull ServerLevel level, @NotNull BlockPos pos, @NotNull RandomSource random) {
        if (!onSaltRandomTick(state, level, pos, random))
            return;
    }

    @Override
    public void tick(@NotNull BlockState state, @NotNull ServerLevel level, @NotNull BlockPos pos, @NotNull RandomSource random) {
        BlockPos clusterPos = pos.above();
        if (ISaltBlock.canGrowCluster(clusterPos, level)) {
            Fluid drippingFluid = ISaltBlock.getFluidDrippingOn(level, clusterPos);

            if (drippingFluid == Fluids.WATER) {
                if (SaltConfiguration.SALT_CLUSTER_GROWING_ENABLED.get() && random.nextDouble() < SaltConfiguration.SALT_CLUSTER_GROWING_CHANCE.get())
                    ISaltBlock.growCluster(state, pos, level);
            } else if (drippingFluid != Fluids.EMPTY)
                level.destroyBlock(clusterPos, false);
        }
    }
}

