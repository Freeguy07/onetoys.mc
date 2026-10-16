package com.onetoys;

import java.util.function.Predicate;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.RedstoneSide;

/** Shape helpers shared by redstone and bluestone dust. */
public final class DustShapes {
    private DustShapes() {
    }

    /**
     * Clears every horizontal arm whose neighbouring column (same level, one up or one down)
     * holds dust of the other kind, so unlinked dust never looks connected.
     */
    public static BlockState stripArmsTowards(BlockGetter level, BlockPos pos, BlockState state, Predicate<BlockState> otherDust) {
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            var property = BluestoneWireBlock.PROPERTY_BY_DIRECTION.get(direction);
            if (state.getValue(property) == RedstoneSide.NONE) {
                continue;
            }
            BlockPos side = pos.relative(direction);
            if (otherDust.test(level.getBlockState(side))
                    || otherDust.test(level.getBlockState(side.above()))
                    || otherDust.test(level.getBlockState(side.below()))) {
                state = state.setValue(property, RedstoneSide.NONE);
            }
        }
        return state;
    }
}

