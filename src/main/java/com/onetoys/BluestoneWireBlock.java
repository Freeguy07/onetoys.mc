package com.onetoys;

import java.util.Map;

import javax.annotation.Nullable;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.mojang.serialization.MapCodec;

import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.ObserverBlock;
import net.minecraft.world.level.block.RepeaterBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.RedstoneSide;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Redstone dust clone that never connects to (or takes power from) redstone dust.
 * Adapted from vanilla {@code RedStoneWireBlock}; see the marked deviations.
 */
public class BluestoneWireBlock extends Block {
    public static final MapCodec<BluestoneWireBlock> CODEC = simpleCodec(BluestoneWireBlock::new);
    public static final EnumProperty<RedstoneSide> NORTH = BlockStateProperties.NORTH_REDSTONE;
    public static final EnumProperty<RedstoneSide> EAST = BlockStateProperties.EAST_REDSTONE;
    public static final EnumProperty<RedstoneSide> SOUTH = BlockStateProperties.SOUTH_REDSTONE;
    public static final EnumProperty<RedstoneSide> WEST = BlockStateProperties.WEST_REDSTONE;
    public static final IntegerProperty POWER = BlockStateProperties.POWER;
    public static final Map<Direction, EnumProperty<RedstoneSide>> PROPERTY_BY_DIRECTION = Maps.newEnumMap(
            ImmutableMap.of(Direction.NORTH, NORTH, Direction.EAST, EAST, Direction.SOUTH, SOUTH, Direction.WEST, WEST));
    private static final VoxelShape SHAPE_DOT = Block.box(3.0, 0.0, 3.0, 13.0, 1.0, 13.0);
    private static final Map<Direction, VoxelShape> SHAPES_FLOOR = Maps.newEnumMap(ImmutableMap.of(
            Direction.NORTH, Block.box(3.0, 0.0, 0.0, 13.0, 1.0, 13.0),
            Direction.SOUTH, Block.box(3.0, 0.0, 3.0, 13.0, 1.0, 16.0),
            Direction.EAST, Block.box(3.0, 0.0, 3.0, 16.0, 1.0, 13.0),
            Direction.WEST, Block.box(0.0, 0.0, 3.0, 13.0, 1.0, 13.0)));
    private static final Map<Direction, VoxelShape> SHAPES_UP = Maps.newEnumMap(ImmutableMap.of(
            Direction.NORTH, Shapes.or(SHAPES_FLOOR.get(Direction.NORTH), Block.box(3.0, 0.0, 0.0, 13.0, 16.0, 1.0)),
            Direction.SOUTH, Shapes.or(SHAPES_FLOOR.get(Direction.SOUTH), Block.box(3.0, 0.0, 15.0, 13.0, 16.0, 16.0)),
            Direction.EAST, Shapes.or(SHAPES_FLOOR.get(Direction.EAST), Block.box(15.0, 0.0, 3.0, 16.0, 16.0, 13.0)),
            Direction.WEST, Shapes.or(SHAPES_FLOOR.get(Direction.WEST), Block.box(0.0, 0.0, 3.0, 1.0, 16.0, 13.0))));
    private static final Map<BlockState, VoxelShape> SHAPES_CACHE = Maps.newHashMap();
    // Deviation: blue-cyan colour ramp instead of vanilla's red one.
    private static final Vec3[] COLORS = Util.make(new Vec3[16], colors -> {
        for (int i = 0; i <= 15; i++) {
            float f = i / 15.0F;
            colors[i] = new Vec3(0.0, Mth.clamp(f * 0.8F, 0.0F, 1.0F), 0.3F + f * 0.7F);
        }
    });
    private final BlockState crossState;
    private final ThreadLocal<Boolean> shouldSignal = ThreadLocal.withInitial(() -> true);

    public BluestoneWireBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(NORTH, RedstoneSide.NONE)
                .setValue(EAST, RedstoneSide.NONE)
                .setValue(SOUTH, RedstoneSide.NONE)
                .setValue(WEST, RedstoneSide.NONE)
                .setValue(POWER, 0));
        this.crossState = this.defaultBlockState()
                .setValue(NORTH, RedstoneSide.SIDE)
                .setValue(EAST, RedstoneSide.SIDE)
                .setValue(SOUTH, RedstoneSide.SIDE)
                .setValue(WEST, RedstoneSide.SIDE);

        for (BlockState state : this.getStateDefinition().getPossibleStates()) {
            SHAPES_CACHE.put(state, this.calculateShape(state));
        }
    }

    @Override
    public MapCodec<BluestoneWireBlock> codec() {
        return CODEC;
    }

    private VoxelShape calculateShape(BlockState state) {
        VoxelShape shape = SHAPE_DOT;

        for (Direction direction : Direction.Plane.HORIZONTAL) {
            RedstoneSide side = state.getValue(PROPERTY_BY_DIRECTION.get(direction));
            if (side == RedstoneSide.SIDE) {
                shape = Shapes.or(shape, SHAPES_FLOOR.get(direction));
            } else if (side == RedstoneSide.UP) {
                shape = Shapes.or(shape, SHAPES_UP.get(direction));
            }
        }

        return shape;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        VoxelShape shape = SHAPES_CACHE.get(state);
        return shape != null ? shape : Shapes.empty();
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.getConnectionState(context.getLevel(), this.crossState, context.getClickedPos());
    }

    private BlockState getConnectionState(BlockGetter level, BlockState state, BlockPos pos) {
        // Deviation: no arms toward redstone dust (vanilla fill logic would draw them).
        return DustShapes.stripArmsTowards(level, pos, this.fillConnections(level, state, pos), other -> other.is(Blocks.REDSTONE_WIRE));
    }

    private BlockState fillConnections(BlockGetter level, BlockState state, BlockPos pos) {
        boolean wasDot = isDot(state);
        state = this.getMissingConnections(level, this.defaultBlockState().setValue(POWER, state.getValue(POWER)), pos);
        if (wasDot && isDot(state)) {
            return state;
        }
        boolean north = state.getValue(NORTH).isConnected();
        boolean south = state.getValue(SOUTH).isConnected();
        boolean east = state.getValue(EAST).isConnected();
        boolean west = state.getValue(WEST).isConnected();
        boolean noNorthSouth = !north && !south;
        boolean noEastWest = !east && !west;
        if (!west && noNorthSouth) {
            state = state.setValue(WEST, RedstoneSide.SIDE);
        }
        if (!east && noNorthSouth) {
            state = state.setValue(EAST, RedstoneSide.SIDE);
        }
        if (!north && noEastWest) {
            state = state.setValue(NORTH, RedstoneSide.SIDE);
        }
        if (!south && noEastWest) {
            state = state.setValue(SOUTH, RedstoneSide.SIDE);
        }
        return state;
    }

    private BlockState getMissingConnections(BlockGetter level, BlockState state, BlockPos pos) {
        boolean notBlockedAbove = !level.getBlockState(pos.above()).isRedstoneConductor(level, pos.above());

        for (Direction direction : Direction.Plane.HORIZONTAL) {
            if (!state.getValue(PROPERTY_BY_DIRECTION.get(direction)).isConnected()) {
                state = state.setValue(PROPERTY_BY_DIRECTION.get(direction), this.getConnectingSide(level, pos, direction, notBlockedAbove));
            }
        }

        return state;
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        if (direction == Direction.DOWN) {
            return !this.canSurviveOn(level, neighborPos, neighborState) ? Blocks.AIR.defaultBlockState() : state;
        } else if (direction == Direction.UP) {
            return this.getConnectionState(level, state, pos);
        } else {
            RedstoneSide side = this.getConnectingSide(level, pos, direction);
            BlockState result = side.isConnected() == state.getValue(PROPERTY_BY_DIRECTION.get(direction)).isConnected() && !isCross(state)
                    ? state.setValue(PROPERTY_BY_DIRECTION.get(direction), side)
                    : this.getConnectionState(level,
                            this.crossState.setValue(POWER, state.getValue(POWER)).setValue(PROPERTY_BY_DIRECTION.get(direction), side), pos);
            return DustShapes.stripArmsTowards(level, pos, result, other -> other.is(Blocks.REDSTONE_WIRE));
        }
    }

    private static boolean isCross(BlockState state) {
        return state.getValue(NORTH).isConnected()
                && state.getValue(SOUTH).isConnected()
                && state.getValue(EAST).isConnected()
                && state.getValue(WEST).isConnected();
    }

    private static boolean isDot(BlockState state) {
        return !state.getValue(NORTH).isConnected()
                && !state.getValue(SOUTH).isConnected()
                && !state.getValue(EAST).isConnected()
                && !state.getValue(WEST).isConnected();
    }

    @Override
    protected void updateIndirectNeighbourShapes(BlockState state, LevelAccessor level, BlockPos pos, int flags, int recursionLeft) {
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();

        for (Direction direction : Direction.Plane.HORIZONTAL) {
            RedstoneSide side = state.getValue(PROPERTY_BY_DIRECTION.get(direction));
            if (side != RedstoneSide.NONE && !level.getBlockState(mutable.setWithOffset(pos, direction)).is(this)) {
                mutable.move(Direction.DOWN);
                BlockState below = level.getBlockState(mutable);
                if (below.is(this)) {
                    BlockPos other = mutable.relative(direction.getOpposite());
                    level.neighborShapeChanged(direction.getOpposite(), level.getBlockState(other), mutable, other, flags, recursionLeft);
                }

                mutable.setWithOffset(pos, direction).move(Direction.UP);
                BlockState above = level.getBlockState(mutable);
                if (above.is(this)) {
                    BlockPos other = mutable.relative(direction.getOpposite());
                    level.neighborShapeChanged(direction.getOpposite(), level.getBlockState(other), mutable, other, flags, recursionLeft);
                }
            }
        }
    }

    private RedstoneSide getConnectingSide(BlockGetter level, BlockPos pos, Direction direction) {
        return this.getConnectingSide(level, pos, direction, !level.getBlockState(pos.above()).isRedstoneConductor(level, pos.above()));
    }

    private RedstoneSide getConnectingSide(BlockGetter level, BlockPos pos, Direction direction, boolean notBlockedAbove) {
        BlockPos neighborPos = pos.relative(direction);
        BlockState neighbor = level.getBlockState(neighborPos);
        if (notBlockedAbove) {
            boolean canPlaceOn = neighbor.getBlock() instanceof TrapDoorBlock || this.canSurviveOn(level, neighborPos, neighbor);
            if (canPlaceOn && shouldConnectTo(level.getBlockState(neighborPos.above()), null)) {
                if (neighbor.isFaceSturdy(level, neighborPos, direction.getOpposite())) {
                    return RedstoneSide.UP;
                }

                return RedstoneSide.SIDE;
            }
        }

        if (shouldConnectTo(neighbor, direction)) {
            return RedstoneSide.SIDE;
        } else if (neighbor.isRedstoneConductor(level, neighborPos)) {
            return RedstoneSide.NONE;
        } else {
            BlockPos belowPos = neighborPos.below();
            return shouldConnectTo(level.getBlockState(belowPos), null) ? RedstoneSide.SIDE : RedstoneSide.NONE;
        }
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockPos below = pos.below();
        return this.canSurviveOn(level, below, level.getBlockState(below));
    }

    private boolean canSurviveOn(BlockGetter level, BlockPos pos, BlockState state) {
        return state.isFaceSturdy(level, pos, Direction.UP) || state.is(Blocks.HOPPER);
    }

    private void updatePowerStrength(Level level, BlockPos pos, BlockState state) {
        int target = this.calculateTargetStrength(level, pos);
        if (state.getValue(POWER) != target) {
            if (level.getBlockState(pos) == state) {
                level.setBlock(pos, state.setValue(POWER, target), 2);
            }

            level.updateNeighborsAt(pos, this);
            for (Direction direction : Direction.values()) {
                level.updateNeighborsAt(pos.relative(direction), this);
            }
        }
    }

    private int calculateTargetStrength(Level level, BlockPos pos) {
        int external;
        this.shouldSignal.set(false);
        try {
            // Deviation: like getBestNeighborSignal, but redstone dust never contributes (even through solid blocks).
            external = DustSignals.bestNeighborSignal(level, pos, state -> state.is(Blocks.REDSTONE_WIRE));
        } finally {
            this.shouldSignal.set(true);
        }
        int wire = 0;
        if (external < 15) {
            for (Direction direction : Direction.Plane.HORIZONTAL) {
                BlockPos neighborPos = pos.relative(direction);
                BlockState neighbor = level.getBlockState(neighborPos);
                wire = Math.max(wire, this.getWireSignal(neighbor));
                BlockPos above = pos.above();
                if (neighbor.isRedstoneConductor(level, neighborPos) && !level.getBlockState(above).isRedstoneConductor(level, above)) {
                    wire = Math.max(wire, this.getWireSignal(level.getBlockState(neighborPos.above())));
                } else if (!neighbor.isRedstoneConductor(level, neighborPos)) {
                    wire = Math.max(wire, this.getWireSignal(level.getBlockState(neighborPos.below())));
                }
            }
        }

        return Math.max(external, wire - 1);
    }

    private int getWireSignal(BlockState state) {
        return state.is(this) ? state.getValue(POWER) : 0;
    }

    private void checkCornerChangeAt(Level level, BlockPos pos) {
        if (level.getBlockState(pos).is(this)) {
            level.updateNeighborsAt(pos, this);

            for (Direction direction : Direction.values()) {
                level.updateNeighborsAt(pos.relative(direction), this);
            }
        }
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        if (!oldState.is(state.getBlock()) && !level.isClientSide) {
            this.updatePowerStrength(level, pos, state);

            for (Direction direction : Direction.Plane.VERTICAL) {
                level.updateNeighborsAt(pos.relative(direction), this);
            }

            this.updateNeighborsOfNeighboringWires(level, pos);
        }
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!movedByPiston && !state.is(newState.getBlock())) {
            super.onRemove(state, level, pos, newState, movedByPiston);
            if (!level.isClientSide) {
                for (Direction direction : Direction.values()) {
                    level.updateNeighborsAt(pos.relative(direction), this);
                }

                this.updatePowerStrength(level, pos, state);
                this.updateNeighborsOfNeighboringWires(level, pos);
            }
        }
    }

    private void updateNeighborsOfNeighboringWires(Level level, BlockPos pos) {
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            this.checkCornerChangeAt(level, pos.relative(direction));
        }

        for (Direction direction : Direction.Plane.HORIZONTAL) {
            BlockPos neighborPos = pos.relative(direction);
            if (level.getBlockState(neighborPos).isRedstoneConductor(level, neighborPos)) {
                this.checkCornerChangeAt(level, neighborPos.above());
            } else {
                this.checkCornerChangeAt(level, neighborPos.below());
            }
        }
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean isMoving) {
        if (!level.isClientSide) {
            if (state.canSurvive(level, pos)) {
                this.updatePowerStrength(level, pos, state);
            } else {
                dropResources(state, level, pos);
                level.removeBlock(pos, false);
            }
        }
    }

    @Override
    protected int getDirectSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        if (!this.shouldSignal.get() || direction == Direction.DOWN || direction == Direction.UP) {
            return 0;
        }
        if (level.getBlockState(pos.relative(direction.getOpposite())).is(Blocks.REDSTONE_WIRE)) {
            return 0;
        }
        return state.getSignal(level, pos, direction);
    }

    @Override
    protected int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        // Deviation: never power redstone dust directly.
        if (this.shouldSignal.get() && direction != Direction.DOWN) {
            if (direction != Direction.UP && level.getBlockState(pos.relative(direction.getOpposite())).is(Blocks.REDSTONE_WIRE)) {
                return 0;
            }
            int power = state.getValue(POWER);
            if (power == 0) {
                return 0;
            }
            return direction != Direction.UP
                    && !this.getConnectionState(level, state, pos).getValue(PROPERTY_BY_DIRECTION.get(direction.getOpposite())).isConnected()
                    ? 0
                    : power;
        }
        return 0;
    }

    // Deviation: connects to bluestone itself, never to redstone dust.
    protected boolean shouldConnectTo(BlockState state) {
        return shouldConnectTo(state, null);
    }

    protected boolean shouldConnectTo(BlockState state, @Nullable Direction direction) {
        if (state.is(this)) {
            return true;
        } else if (state.is(Blocks.REDSTONE_WIRE)) {
            return false;
        } else if (state.is(Blocks.REPEATER)) {
            Direction facing = state.getValue(RepeaterBlock.FACING);
            return facing == direction || facing.getOpposite() == direction;
        } else if (state.is(Blocks.OBSERVER)) {
            return direction == state.getValue(ObserverBlock.FACING);
        }
        return state.isSignalSource() && direction != null;
    }

    /** NeoForge extension: tells redstone dust (and modded wires) that redstone NEVER connects to bluestone wire. */
    @Override
    public boolean canConnectRedstone(BlockState state, BlockGetter level, BlockPos pos, @Nullable Direction direction) {
        return false;
    }

    @Override
    protected boolean isSignalSource(BlockState state) {
        return this.shouldSignal.get();
    }

    public static int getColorForPower(int power) {
        Vec3 color = COLORS[power];
        return Mth.color((float) color.x(), (float) color.y(), (float) color.z());
    }

    private void spawnParticlesAlongLine(Level level, RandomSource random, BlockPos pos, Vec3 color, Direction from, Direction to, float min, float max) {
        float range = max - min;
        if (!(random.nextFloat() >= 0.2F * range)) {
            float offset = min + range * random.nextFloat();
            double x = 0.5 + 0.4375F * from.getStepX() + offset * to.getStepX();
            double y = 0.5 + 0.4375F * from.getStepY() + offset * to.getStepY();
            double z = 0.5 + 0.4375F * from.getStepZ() + offset * to.getStepZ();
            level.addParticle(new DustParticleOptions(color.toVector3f(), 1.0F), pos.getX() + x, pos.getY() + y, pos.getZ() + z, 0.0, 0.0, 0.0);
        }
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        int power = state.getValue(POWER);
        if (power != 0) {
            for (Direction direction : Direction.Plane.HORIZONTAL) {
                RedstoneSide side = state.getValue(PROPERTY_BY_DIRECTION.get(direction));
                switch (side) {
                    case UP:
                        this.spawnParticlesAlongLine(level, random, pos, COLORS[power], direction, Direction.UP, -0.5F, 0.5F);
                        // fall through, as in vanilla
                    case SIDE:
                        this.spawnParticlesAlongLine(level, random, pos, COLORS[power], Direction.DOWN, direction, 0.0F, 0.5F);
                        break;
                    case NONE:
                    default:
                        this.spawnParticlesAlongLine(level, random, pos, COLORS[power], Direction.DOWN, direction, 0.0F, 0.3F);
                }
            }
        }
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        switch (rotation) {
            case CLOCKWISE_180:
                return state.setValue(NORTH, state.getValue(SOUTH))
                        .setValue(EAST, state.getValue(WEST))
                        .setValue(SOUTH, state.getValue(NORTH))
                        .setValue(WEST, state.getValue(EAST));
            case COUNTERCLOCKWISE_90:
                return state.setValue(NORTH, state.getValue(EAST))
                        .setValue(EAST, state.getValue(SOUTH))
                        .setValue(SOUTH, state.getValue(WEST))
                        .setValue(WEST, state.getValue(NORTH));
            case CLOCKWISE_90:
                return state.setValue(NORTH, state.getValue(WEST))
                        .setValue(EAST, state.getValue(NORTH))
                        .setValue(SOUTH, state.getValue(EAST))
                        .setValue(WEST, state.getValue(SOUTH));
            default:
                return state;
        }
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        switch (mirror) {
            case LEFT_RIGHT:
                return state.setValue(NORTH, state.getValue(SOUTH)).setValue(SOUTH, state.getValue(NORTH));
            case FRONT_BACK:
                return state.setValue(EAST, state.getValue(WEST)).setValue(WEST, state.getValue(EAST));
            default:
                return super.mirror(state, mirror);
        }
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(NORTH, EAST, SOUTH, WEST, POWER);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!player.getAbilities().mayBuild) {
            return InteractionResult.PASS;
        }
        if (isCross(state) || isDot(state)) {
            BlockState newState = isCross(state) ? this.defaultBlockState() : this.crossState;
            newState = newState.setValue(POWER, state.getValue(POWER));
            newState = this.getConnectionState(level, newState, pos);
            if (newState != state) {
                level.setBlock(pos, newState, 3);
                this.updatesOnShapeChange(level, pos, state, newState);
                return InteractionResult.SUCCESS;
            }
        }

        return InteractionResult.PASS;
    }

    private void updatesOnShapeChange(Level level, BlockPos pos, BlockState oldState, BlockState newState) {
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            BlockPos neighborPos = pos.relative(direction);
            if (oldState.getValue(PROPERTY_BY_DIRECTION.get(direction)).isConnected() != newState.getValue(PROPERTY_BY_DIRECTION.get(direction)).isConnected()
                    && level.getBlockState(neighborPos).isRedstoneConductor(level, neighborPos)) {
                level.updateNeighborsAtExceptFromFacing(neighborPos, newState.getBlock(), direction.getOpposite());
            }
        }
    }
}
