package com.onetoys.block;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Queue;
import java.util.Set;

import javax.annotation.Nullable;

import com.onetoys.block.entity.QuartzGlassBlockEntity;
import com.onetoys.registry.OnetoysBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.TransparentBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class QuartzGlassBlock extends TransparentBlock implements EntityBlock {
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
    private static final ThreadLocal<Boolean> UPDATING_NETWORK = ThreadLocal.withInitial(() -> false);
    private static final int MAX_NETWORK_SIZE = 512;

    public QuartzGlassBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(POWERED, Boolean.FALSE));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(POWERED);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new QuartzGlassBlockEntity(pos, state);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return state.getValue(POWERED) ? RenderShape.MODEL : RenderShape.INVISIBLE;
    }

    @Override
    public boolean skipRendering(BlockState state, BlockState adjacentState, Direction direction) {
        return adjacentState.is(this) ? true : super.skipRendering(state, adjacentState, direction);
    }

    @Override
    public float getShadeBrightness(BlockState state, BlockGetter level, BlockPos pos) {
        return 1.0F;
    }

    @Override
    public boolean propagatesSkylightDown(BlockState state, BlockGetter reader, BlockPos pos) {
        return true;
    }

    @Override
    public int getLightBlock(BlockState state, BlockGetter level, BlockPos pos) {
        return 0;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.block();
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.block();
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (level.getBlockEntity(pos) instanceof QuartzGlassBlockEntity be) {
            be.setPlacedGameTime(level.getGameTime());
        }
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        if (!state.is(oldState.getBlock())) {
            updateNetwork(level, pos);
        }
    }

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock, BlockPos neighborPos, boolean isMoving) {
        if (!level.isClientSide) {
            updateNetwork(level, pos);
        }
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            super.onRemove(state, level, pos, newState, isMoving);
            if (!level.isClientSide) {
                for (Direction dir : Direction.values()) {
                    BlockPos neighborPos = pos.relative(dir);
                    if (level.getBlockState(neighborPos).is(this)) {
                        updateNetwork(level, neighborPos);
                    }
                }
            }
        } else {
            super.onRemove(state, level, pos, newState, isMoving);
        }
    }

    private static void updateNetwork(Level level, BlockPos startPos) {
        if (level.isClientSide || UPDATING_NETWORK.get()) {
            return;
        }

        try {
            UPDATING_NETWORK.set(true);
            Set<BlockPos> visited = new HashSet<>();
            Queue<BlockPos> queue = new ArrayDeque<>();

            visited.add(startPos);
            queue.add(startPos);

            boolean hasSignal = false;

            while (!queue.isEmpty() && visited.size() <= MAX_NETWORK_SIZE) {
                BlockPos current = queue.poll();
                if (level.hasNeighborSignal(current)) {
                    hasSignal = true;
                }

                for (Direction dir : Direction.values()) {
                    BlockPos neighborPos = current.relative(dir);
                    if (!visited.contains(neighborPos) && level.getBlockState(neighborPos).is(OnetoysBlocks.QUARTZ_GLASS.get())) {
                        visited.add(neighborPos);
                        queue.add(neighborPos);
                    }
                }
            }

            for (BlockPos p : visited) {
                BlockState current = level.getBlockState(p);
                if (current.is(OnetoysBlocks.QUARTZ_GLASS.get()) && current.getValue(POWERED) != hasSignal) {
                    level.setBlock(p, current.setValue(POWERED, hasSignal), 3);
                }
            }
        } finally {
            UPDATING_NETWORK.set(false);
        }
    }
}
