package com.orehall.block;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.orehall.entity.SprinklerDropletEntity;
import com.orehall.module.content.ContentModule;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class SprinklerBlock extends Block implements SimpleWaterloggedBlock {

    public static final BooleanProperty HANGING = BlockStateProperties.HANGING;
    public static final BooleanProperty TRIGGERED = BlockStateProperties.TRIGGERED;
    public static final IntegerProperty WAVE = IntegerProperty.create("wave", 0, 4);
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;

    public record RingOffset(int dx, int dz, double normX, double normZ) {}
    public record TrajectoryParams(double sx, double gSumGrav) {}

    private static final VoxelShape SHAPE_FLOOR = Shapes.or(
            Block.box(2.0, 0.0, 2.0, 14.0, 2.0, 14.0),
            Block.box(5.0, 2.0, 5.0, 11.0, 11.0, 11.0)
    );

    private static final VoxelShape SHAPE_HANGING = Shapes.or(
            Block.box(2.0, 14.0, 2.0, 14.0, 16.0, 14.0),
            Block.box(5.0, 5.0, 5.0, 11.0, 14.0, 11.0)
    );

    // Precomputed relative block offsets and unit vectors for the 4 concentric rings
    private static final List<List<RingOffset>> RING_OFFSETS;
    // Precomputed closed-form trajectory flight parameters for exact discrete motion
    private static final Map<Integer, TrajectoryParams> TRAJECTORY_PARAMS;

    static {
        List<List<RingOffset>> rings = new ArrayList<>(4);
        for (int r = 1; r <= 4; r++) {
            List<RingOffset> ring = new ArrayList<>();
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) == r) {
                        double dist = Math.sqrt(dx * dx + dz * dz);
                        double normX = dist > 0.0001 ? dx / dist : 0.0;
                        double normZ = dist > 0.0001 ? dz / dist : 0.0;
                        ring.add(new RingOffset(dx, dz, normX, normZ));
                    }
                }
            }
            rings.add(Collections.unmodifiableList(ring));
        }
        RING_OFFSETS = Collections.unmodifiableList(rings);

        Map<Integer, TrajectoryParams> params = new HashMap<>();
        for (int ticks : new int[]{8, 11, 14, 17}) {
            double sx = 0.0;
            double term = 1.0;
            for (int t = 0; t < ticks; t++) {
                sx += term;
                term *= 0.99;
            }

            double gSum = 0.0;
            double currentGTerm = 0.0;
            double gPower = 1.0;
            for (int t = 1; t < ticks; t++) {
                currentGTerm += gPower;
                gSum += currentGTerm;
                gPower *= 0.99;
            }
            params.put(ticks, new TrajectoryParams(sx, 0.05 * gSum));
        }
        TRAJECTORY_PARAMS = Collections.unmodifiableMap(params);
    }

    public SprinklerBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(HANGING, false)
                .setValue(TRIGGERED, false)
                .setValue(WAVE, 0)
                .setValue(WATERLOGGED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(HANGING, TRIGGERED, WAVE, WATERLOGGED);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(HANGING) ? SHAPE_HANGING : SHAPE_FLOOR;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        FluidState fluidState = context.getLevel().getFluidState(context.getClickedPos());
        Direction clickedFace = context.getClickedFace();
        boolean hanging;
        if (clickedFace == Direction.DOWN) {
            hanging = true;
        } else if (clickedFace == Direction.UP) {
            hanging = false;
        } else {
            double clickY = context.getClickLocation().y - context.getClickedPos().getY();
            hanging = clickY > 0.5;
        }

        return this.defaultBlockState()
                .setValue(HANGING, hanging)
                .setValue(TRIGGERED, false)
                .setValue(WAVE, 0)
                .setValue(WATERLOGGED, fluidState.getType() == Fluids.WATER);
    }

    @Override
    public FluidState getFluidState(BlockState state) {
        return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        if (state.getValue(WATERLOGGED)) {
            level.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        }
        return super.updateShape(state, direction, neighborState, level, pos, neighborPos);
    }

    private static boolean hasSignal(Level level, BlockPos pos) {
        return level.hasNeighborSignal(pos) || level.hasNeighborSignal(pos.above()) || level.hasNeighborSignal(pos.below());
    }

    public static boolean hasWaterSupply(Level level, BlockPos pos, BlockState state) {
        if (!ContentModule.isSprinklerWaterRequired()) {
            return true;
        }
        // 1. Sprinkler is directly waterlogged
        if (state.getValue(WATERLOGGED)) {
            return true;
        }
        // 2. Direct fluid water in any adjacent block (6 directions)
        for (Direction dir : Direction.values()) {
            BlockPos neighborPos = pos.relative(dir);
            FluidState fluid = level.getFluidState(neighborPos);
            if (fluid.is(Fluids.WATER) || fluid.is(Fluids.FLOWING_WATER)) {
                return true;
            }
        }
        // 3. Water cauldron directly underneath
        BlockState belowState = level.getBlockState(pos.below());
        if (belowState.is(Blocks.WATER_CAULDRON)) {
            return true;
        }
        return false;
    }

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean isMoving) {
        if (level.isClientSide()) {
            return;
        }

        boolean hasSignal = hasSignal(level, pos);
        boolean triggered = state.getValue(TRIGGERED);

        if (hasSignal && !triggered) {
            // Rising edge pulse!
            level.setBlock(pos, state.setValue(TRIGGERED, true), 3);
            if (state.getValue(WAVE) == 0 && ContentModule.isSprinklerEnabled()) {
                tryStartCycle(level, pos, state.setValue(TRIGGERED, true));
            }
        } else if (!hasSignal && triggered) {
            level.setBlock(pos, state.setValue(TRIGGERED, false), 3);
        }
    }

    private void tryStartCycle(Level level, BlockPos pos, BlockState state) {
        if (!hasWaterSupply(level, pos, state)) {
            // Dry sputtering: no water supply available!
            level.playSound(null, pos, SoundEvents.DISPENSER_FAIL, SoundSource.BLOCKS, 0.7F, 1.3F);
            if (level instanceof ServerLevel serverLevel) {
                double y = state.getValue(HANGING) ? pos.getY() + 0.35 : pos.getY() + 0.65;
                serverLevel.sendParticles(ParticleTypes.SMOKE, pos.getX() + 0.5, y, pos.getZ() + 0.5, 6, 0.1, 0.05, 0.1, 0.02);
            }
            if (ContentModule.isSprinklerContinuous()) {
                level.scheduleTick(pos, this, 40);
            }
            return;
        }

        level.setBlock(pos, state.setValue(TRIGGERED, true).setValue(WAVE, 1), 3);
        level.playSound(null, pos, com.orehall.registry.OrehallSoundEvents.SPRINKLER_ACTIVATE.get(), SoundSource.BLOCKS, 0.5F, 1.0F);
        level.playSound(null, pos, SoundEvents.GENERIC_SPLASH, SoundSource.BLOCKS, 0.4F, 1.4F);
        fireWave(level, pos, state, 1);
        level.scheduleTick(pos, this, 3);
    }

    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        int currentWave = state.getValue(WAVE);
        if (currentWave >= 1 && currentWave <= 3) {
            int nextWave = currentWave + 1;
            fireWave(level, pos, state, nextWave);
            level.setBlock(pos, state.setValue(WAVE, nextWave), 3);
            level.scheduleTick(pos, this, 3);
        } else if (currentWave == 4) {
            // Completed 4th wave; reset wave state to idle (0)
            boolean hasSignal = hasSignal(level, pos);
            level.setBlock(pos, state.setValue(WAVE, 0).setValue(TRIGGERED, hasSignal), 3);

            // If continuous irrigation is enabled and redstone power is maintained, schedule the next cycle!
            if (hasSignal && ContentModule.isSprinklerContinuous() && ContentModule.isSprinklerEnabled()) {
                int interval = ContentModule.getSprinklerIntervalTicks();
                level.scheduleTick(pos, this, Math.max(20, interval));
            }
        } else if (currentWave == 0) {
            // Periodic wakeup while idle
            boolean hasSignal = hasSignal(level, pos);
            if (hasSignal && ContentModule.isSprinklerContinuous() && ContentModule.isSprinklerEnabled()) {
                tryStartCycle(level, pos, state);
            } else {
                level.setBlock(pos, state.setValue(TRIGGERED, false), 3);
            }
        }
    }

    private void fireWave(Level level, BlockPos pos, BlockState state, int ringIndex) {
        if (ringIndex < 1 || ringIndex > 4) {
            return;
        }

        boolean hanging = state.getValue(HANGING);
        List<RingOffset> offsets = RING_OFFSETS.get(ringIndex - 1);
        int flightTicks = switch (ringIndex) {
            case 1 -> 8;
            case 2 -> 11;
            case 3 -> 14;
            default -> 17;
        };

        Vec3 start = new Vec3(
                pos.getX() + 0.5,
                hanging ? pos.getY() + 0.35 : pos.getY() + 0.65,
                pos.getZ() + 0.5
        );

        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();

        // Additional projectile: When placed upside-down, launch a center droplet straight down to water the block directly beneath the sprinkler
        if (hanging && ringIndex == 1) {
            BlockPos centerTargetPos = findTargetSurface(level, pos.getX(), pos.getY() - 1, pos.getZ(), true, cursor);
            Vec3 centerTarget = new Vec3(centerTargetPos.getX() + 0.5, centerTargetPos.getY() + 0.5, centerTargetPos.getZ() + 0.5);
            Vec3 centerVel = calculateTrajectory(start, centerTarget, 8);

            SprinklerDropletEntity centerDroplet = new SprinklerDropletEntity(level, start.x, start.y, start.z);
            centerDroplet.setDeltaMovement(centerVel);
            level.addFreshEntity(centerDroplet);
        }

        // When floor-mounted, ensure farmland directly beneath the sprinkler block stays hydrated
        if (!hanging && ringIndex == 1) {
            BlockPos belowPos = pos.below();
            BlockState belowState = level.getBlockState(belowPos);
            if (belowState.getBlock() instanceof FarmBlock && belowState.getValue(FarmBlock.MOISTURE) < 7) {
                level.setBlock(belowPos, belowState.setValue(FarmBlock.MOISTURE, 7), 2);
            }
        }

        // Concentric ring wave droplets
        for (RingOffset offset : offsets) {
            int targetX = pos.getX() + offset.dx();
            int targetZ = pos.getZ() + offset.dz();
            BlockPos targetPos = findTargetSurface(level, targetX, pos.getY(), targetZ, hanging, cursor);
            Vec3 target = new Vec3(targetPos.getX() + 0.5, targetPos.getY() + 0.5, targetPos.getZ() + 0.5);

            Vec3 vel = calculateTrajectory(start, target, flightTicks);

            SprinklerDropletEntity droplet = new SprinklerDropletEntity(level, start.x, start.y, start.z);
            droplet.setDeltaMovement(vel);
            level.addFreshEntity(droplet);
        }

        // Outward nozzle spray particles: shoot outwards radially from the sprinkler block itself
        if (level instanceof ServerLevel serverLevel) {
            double outwardSpeed = 0.28 + ringIndex * 0.05;
            double vy = hanging ? -0.06 : 0.12;
            int step = ringIndex >= 3 ? 2 : 1;
            for (int i = 0; i < offsets.size(); i += step) {
                RingOffset offset = offsets.get(i);
                serverLevel.sendParticles(
                        ParticleTypes.SPLASH,
                        start.x, start.y, start.z,
                        0,
                        offset.normX(), vy, offset.normZ(),
                        outwardSpeed
                );
            }
            serverLevel.sendParticles(
                    ParticleTypes.RAIN,
                    start.x, start.y + (hanging ? -0.1 : 0.1), start.z,
                    4,
                    0.1, 0.05, 0.1,
                    0.02
            );
        }

        // Pressurized nozzle hiss + dripstone splash audio
        level.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.15F, 1.8F);
        float pitchDrip = 1.0F + ringIndex * 0.15F;
        level.playSound(null, pos, SoundEvents.POINTED_DRIPSTONE_DRIP_WATER, SoundSource.BLOCKS, 0.4F, pitchDrip);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        int wave = state.getValue(WAVE);
        boolean hanging = state.getValue(HANGING);
        double centerX = pos.getX() + 0.5;
        double centerY = hanging ? pos.getY() + 0.35 : pos.getY() + 0.65;
        double centerZ = pos.getZ() + 0.5;

        if (wave > 0) {
            // Active irrigation: swirling mist and fine water splash around rotor nozzles
            for (int i = 0; i < 2; i++) {
                double angle = random.nextDouble() * Math.PI * 2.0;
                double speed = 0.04 + random.nextDouble() * 0.06;
                double vx = Math.cos(angle) * speed;
                double vz = Math.sin(angle) * speed;
                double vy = hanging ? -0.04 : 0.05;
                level.addParticle(ParticleTypes.SPLASH, centerX, centerY, centerZ, vx, vy, vz);
            }
            if (random.nextFloat() < 0.4F) {
                level.addParticle(ParticleTypes.RAIN, centerX + (random.nextDouble() - 0.5) * 0.2, centerY, centerZ + (random.nextDouble() - 0.5) * 0.2, 0.0, -0.05, 0.0);
            }
        } else if (state.getValue(WATERLOGGED)) {
            // Idle waterlogged sprinkler: occasional peaceful water drip
            if (random.nextFloat() < 0.05F) {
                double dripY = hanging ? pos.getY() + 0.1 : pos.getY() + 0.6;
                level.addParticle(ParticleTypes.DRIPPING_WATER, centerX, dripY, centerZ, 0.0, 0.0, 0.0);
            }
        }
    }

    private BlockPos findTargetSurface(Level level, int x, int baseY, int z, boolean hanging, BlockPos.MutableBlockPos cursor) {
        int startY = hanging ? baseY - 1 : baseY + 2;
        int endY = hanging ? baseY - 7 : baseY - 3;
        for (int y = startY; y >= endY; y--) {
            cursor.set(x, y, z);
            BlockState state = level.getBlockState(cursor);
            if (!state.isAir()) {
                return cursor.immutable();
            }
        }
        return new BlockPos(x, hanging ? baseY - 4 : baseY, z);
    }

    /**
     * Constant-time exact discrete trajectory calculation using precomputed flight parameters.
     */
    public static Vec3 calculateTrajectory(Vec3 start, Vec3 target, int ticks) {
        TrajectoryParams params = TRAJECTORY_PARAMS.get(ticks);
        if (params != null) {
            double dx = target.x - start.x;
            double dy = target.y - start.y;
            double dz = target.z - start.z;
            return new Vec3(dx / params.sx(), (dy + params.gSumGrav()) / params.sx(), dz / params.sx());
        }
        return calculateTrajectory(start, target, ticks, 0.05, 0.99);
    }

    /**
     * Exact discrete trajectory math matching Minecraft projectile motion (drag = 0.99, gravity = 0.05).
     */
    public static Vec3 calculateTrajectory(Vec3 start, Vec3 target, int ticks, double gravity, double drag) {
        double dx = target.x - start.x;
        double dz = target.z - start.z;
        double dy = target.y - start.y;

        double sx = 0.0;
        double term = 1.0;
        for (int t = 0; t < ticks; t++) {
            sx += term;
            term *= drag;
        }

        double gSum = 0.0;
        double currentGTerm = 0.0;
        double gPower = 1.0;
        for (int t = 1; t < ticks; t++) {
            currentGTerm += gPower;
            gSum += currentGTerm;
            gPower *= drag;
        }

        double vx = dx / sx;
        double vz = dz / sx;
        double vy = (dy + gravity * gSum) / sx;

        return new Vec3(vx, vy, vz);
    }
}
