package com.onetoys.salt;

import com.onetoys.salt.block.ISaltBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.Vec3;

public class Dissolving {

    public static boolean maybeDissolveInRain(BlockState dissolvedState, ServerLevel level, BlockPos pos) {
        if (SaltConfiguration.isLoaded()
                && SaltConfiguration.DISSOLVING_ENABLED.get()
                && SaltConfiguration.DISSOLVING_IN_RAIN.get()
                && level.isRainingAt(pos.above())
                && level.random.nextDouble() < SaltConfiguration.DISSOLVING_IN_RAIN_CHANCE.get()) {
            dissolve(dissolvedState, level, pos, Fluids.EMPTY, false);
            return true;
        }
        return false;
    }

    public static boolean maybeDissolveFromWatering(BlockState state, ServerLevel level, BlockPos pos) {
        if (SaltConfiguration.isLoaded()
                && SaltConfiguration.DISSOLVING_ENABLED.get()
                && SaltConfiguration.DISSOLVING_IN_RAIN.get()
                && level.random.nextDouble() < SaltConfiguration.DISSOLVING_IN_RAIN_CHANCE.get()) {
            dissolve(state, level, pos, Fluids.EMPTY, false);
            return true;
        }
        return false;
    }

    public static boolean maybeDissolve(BlockState dissolvedState, BlockPos dissolvedPos, BlockState adjacentState, BlockPos adjacentPos, ServerLevel level) {
        if (SaltConfiguration.isLoaded()
                && SaltConfiguration.DISSOLVING_ENABLED.get()
                && adjacentState.is(SaltTags.Blocks.SALT_DISSOLVABLES) && level.random.nextDouble() < SaltConfiguration.DISSOLVING_CHANCE.get()) {
            FluidState fluidState = adjacentState.getFluidState();
            if (dissolvedPos.getY() > adjacentPos.getY())
                fluidState = Fluids.EMPTY.defaultFluidState();
            dissolve(dissolvedState, level, dissolvedPos, fluidState.getType(), fluidState.isSource());
            return true;
        }
        return false;
    }

    public static void dissolve(BlockState state, ServerLevel level, BlockPos pos, Fluid fluid, boolean isSource) {
        BlockState dissolvedState = state.getBlock() instanceof ISaltBlock saltBlock ?
                saltBlock.getDissolvedState(state, level, pos, fluid) :
                Blocks.AIR.defaultBlockState();

        if (shouldPlaceFluidSource(level, pos, fluid, isSource)) {
            if (dissolvedState.isAir())
                dissolvedState = fluid.defaultFluidState().createLegacyBlock();
            else if (dissolvedState.getBlock() instanceof SimpleWaterloggedBlock waterloggedBlock
                    && waterloggedBlock.canPlaceLiquid(null, level, pos, dissolvedState, fluid))
                dissolvedState = dissolvedState.setValue(BlockStateProperties.WATERLOGGED, true);
        }

        level.setBlockAndUpdate(pos, dissolvedState);

        onBlockDissolved(level, pos);
    }

    private static boolean shouldPlaceFluidSource(ServerLevel level, BlockPos pos, Fluid fluid, boolean isSource) {
        return SaltConfiguration.isLoaded() && SaltConfiguration.DISSOLVING_FLUID_SOURCE_CONVERSION.get() && fluid == Fluids.WATER && isSource;
    }

    private static void onBlockDissolved(ServerLevel level, BlockPos pos) {
        Vec3 center = Vec3.atCenterOf(pos);
        RandomSource random = level.getRandom();

        level.playSound(null, center.x, center.y, center.z, SaltSounds.SALT_DISSOLVE.get(), SoundSource.BLOCKS, 1f, random.nextFloat() * 0.2f + 0.9f);

        for (int i = 0; i < 6; i++) {
            level.sendParticles(ParticleTypes.SPIT, center.x + random.nextGaussian() * 0.35f,
                    center.y + random.nextGaussian() * 0.35f, center.z + random.nextGaussian() * 0.35f,
                    1, 0f, 0f, 0f, 0f);
        }
    }
}
