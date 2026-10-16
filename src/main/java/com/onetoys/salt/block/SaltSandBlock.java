package com.onetoys.salt.block;

import com.mojang.serialization.MapCodec;
import com.onetoys.salt.SaltConfiguration;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ColorRGBA;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ColoredFallingBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import org.jetbrains.annotations.NotNull;

public class SaltSandBlock extends ColoredFallingBlock implements ISaltBlock {
    public static final MapCodec<SaltSandBlock> CODEC = simpleCodec(props -> new SaltSandBlock(new ColorRGBA(0xe7d5cf), props));

    private final BlockState dissolvedState;

    public SaltSandBlock(BlockState dissolvedState, ColorRGBA dustColor, Properties properties) {
        super(dustColor, properties);
        this.dissolvedState = dissolvedState;
    }

    public SaltSandBlock(ColorRGBA dustColor, Properties properties) {
        super(dustColor, properties);
        this.dissolvedState = Blocks.AIR.defaultBlockState();
    }

    public SaltSandBlock(int dustColorRgb, Properties properties) {
        this(new ColorRGBA(dustColorRgb), properties);
    }

    @SuppressWarnings("unchecked")
    @Override
    public MapCodec<ColoredFallingBlock> codec() {
        return (MapCodec<ColoredFallingBlock>) (MapCodec<?>) CODEC;
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
        super.tick(state, level, pos, random);

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

