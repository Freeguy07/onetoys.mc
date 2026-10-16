package com.onetoys;

import java.util.function.Predicate;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/** Neighbour signal lookup for redstone-style dust that ignores a given kind of dust as a power source. */
public final class DustSignals {
    private DustSignals() {
    }

    /**
     * Same as vanilla {@code Level.getBestNeighborSignal}, except sources matching {@code ignoredSource}
     * are skipped, both when adjacent and when powering a solid neighbour.
     */
    public static int bestNeighborSignal(Level level, BlockPos pos, Predicate<BlockState> ignoredSource) {
        int best = 0;
        for (Direction direction : Direction.values()) {
            BlockPos neighborPos = pos.relative(direction);
            BlockState neighbor = level.getBlockState(neighborPos);
            if (ignoredSource.test(neighbor)) {
                continue;
            }
            int signal = neighbor.getSignal(level, neighborPos, direction);
            if (neighbor.shouldCheckWeakPower(level, neighborPos, direction)) {
                signal = Math.max(signal, directSignalTo(level, neighborPos, ignoredSource));
            }
            if (signal >= 15) {
                return 15;
            }
            best = Math.max(best, signal);
        }
        return best;
    }

    private static int directSignalTo(Level level, BlockPos pos, Predicate<BlockState> ignoredSource) {
        int best = 0;
        for (Direction direction : Direction.values()) {
            BlockPos sourcePos = pos.relative(direction);
            if (ignoredSource.test(level.getBlockState(sourcePos))) {
                continue;
            }
            best = Math.max(best, level.getDirectSignal(sourcePos, direction));
            if (best >= 15) {
                return 15;
            }
        }
        return best;
    }
}

