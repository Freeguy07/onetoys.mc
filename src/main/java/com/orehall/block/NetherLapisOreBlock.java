package com.orehall.block;

import com.orehall.registry.OrehallParticles;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DropExperienceBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

public class NetherLapisOreBlock extends DropExperienceBlock {
    public NetherLapisOreBlock(IntProvider xpRange, BlockBehaviour.Properties properties) {
        super(xpRange, properties);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        super.animateTick(state, level, pos, random);

        // Only simulate melting away if the block is in a hot dimension
        if (!level.dimensionType().ultraWarm()) {
            return;
        }

        if (random.nextInt(5) == 0) {
            Direction direction = Direction.getRandom(random);
            if (direction != Direction.UP) {
                BlockPos blockpos = pos.relative(direction);
                BlockState blockstate = level.getBlockState(blockpos);
                if (!state.canOcclude() || !blockstate.isFaceSturdy(level, blockpos, direction.getOpposite())) {
                    double d0 = direction.getStepX() == 0 ? random.nextDouble() : 0.5 + (double) direction.getStepX() * 0.6;
                    double d1 = direction.getStepY() == 0 ? random.nextDouble() : 0.5 + (double) direction.getStepY() * 0.6;
                    double d2 = direction.getStepZ() == 0 ? random.nextDouble() : 0.5 + (double) direction.getStepZ() * 0.6;
                    level.addParticle(
                            OrehallParticles.DRIPPING_NETHER_LAPIS.get(),
                            (double) pos.getX() + d0,
                            (double) pos.getY() + d1,
                            (double) pos.getZ() + d2,
                            0.0,
                            0.0,
                            0.0
                    );
                }
            }
        }
    }
}
