package com.onetoys.block;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class DumbbellBlock extends FallingBlock {
    public static final MapCodec<DumbbellBlock> CODEC = simpleCodec(DumbbellBlock::new);
    public static final IntegerProperty DUMBBELLS = IntegerProperty.create("dumbbells", 1, 4);
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    private static final VoxelShape ONE_Z = Block.box(4.0, 0.0, 0.0, 12.0, 8.0, 16.0);
    private static final VoxelShape ONE_X = Block.box(0.0, 0.0, 4.0, 16.0, 8.0, 12.0);
    private static final VoxelShape TWO = Block.box(0.0, 0.0, 0.0, 16.0, 8.0, 16.0);
    private static final VoxelShape THREE_OR_FOUR = Block.box(0.0, 0.0, 0.0, 16.0, 16.0, 16.0);

    @Override
    public MapCodec<DumbbellBlock> codec() {
        return CODEC;
    }

    public DumbbellBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(DUMBBELLS, 1)
                .setValue(FACING, Direction.NORTH));
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState clickedState = context.getLevel().getBlockState(context.getClickedPos());
        if (clickedState.is(this)) {
            return clickedState.setValue(DUMBBELLS, Math.min(4, clickedState.getValue(DUMBBELLS) + 1));
        }
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection());
    }

    @Override
    protected boolean canBeReplaced(BlockState state, BlockPlaceContext useContext) {
        return !useContext.isSecondaryUseActive() && useContext.getItemInHand().is(this.asItem()) && state.getValue(DUMBBELLS) < 4
                ? true
                : super.canBeReplaced(state, useContext);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        int count = state.getValue(DUMBBELLS);
        if (count == 1) {
            Direction direction = state.getValue(FACING);
            return direction.getAxis() == Direction.Axis.X ? ONE_X : ONE_Z;
        } else if (count == 2) {
            return TWO;
        } else {
            return THREE_OR_FOUR;
        }
    }

    @Override
    protected VoxelShape getOcclusionShape(BlockState state, BlockGetter level, BlockPos pos) {
        return Shapes.empty();
    }

    @Override
    protected VoxelShape getVisualShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    @Override
    protected void falling(FallingBlockEntity fallingEntity) {
        BlockState state = fallingEntity.getBlockState();
        int count = state.hasProperty(DUMBBELLS) ? state.getValue(DUMBBELLS) : 1;
        fallingEntity.setHurtsEntities(count * 2.0F, count * 10);
    }

    @Override
    public void onLand(Level level, BlockPos pos, BlockState state, BlockState replaceableState, FallingBlockEntity fallingBlock) {
        if (!fallingBlock.isSilent()) {
            level.levelEvent(1031, pos, 0);
        }
    }

    @Override
    public void onBrokenAfterFall(Level level, BlockPos pos, FallingBlockEntity fallingBlock) {
        if (!fallingBlock.isSilent()) {
            level.levelEvent(1029, pos, 0);
        }
    }

    @Override
    public DamageSource getFallDamageSource(Entity entity) {
        return entity.damageSources().fallingBlock(entity);
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rot) {
        return state.setValue(FACING, rot.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.setValue(FACING, mirror.mirror(state.getValue(FACING)));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(DUMBBELLS, FACING);
    }

    @Override
    public int getDustColor(BlockState state, BlockGetter reader, BlockPos pos) {
        return state.getMapColor(reader, pos).col;
    }
}
