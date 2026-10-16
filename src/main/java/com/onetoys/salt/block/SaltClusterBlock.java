package com.onetoys.salt.block;

import com.onetoys.salt.SaltBlocks;
import com.onetoys.salt.SaltConfiguration;
import com.onetoys.salt.SaltCriteriaTriggers;
import com.onetoys.salt.SaltSounds;
import com.onetoys.salt.SaltTags;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ThrownTrident;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class SaltClusterBlock extends Block implements ISaltBlock {
    public static final MapCodec<SaltClusterBlock> CODEC = simpleCodec(props -> new SaltClusterBlock(7, 3, props));
    public static final DirectionProperty FACING = BlockStateProperties.FACING;

    protected final VoxelShape northAabb;
    protected final VoxelShape southAabb;
    protected final VoxelShape diamondEastAabb;
    protected final VoxelShape eastAabb;
    protected final VoxelShape westAabb;
    protected final VoxelShape upAabb;
    protected final VoxelShape downAabb;

    public SaltClusterBlock(int size, int offset, Properties properties) {
        super(properties);
        this.registerDefaultState(this.defaultBlockState().setValue(FACING, Direction.UP));
        this.upAabb = Block.box(offset, 0.0D, offset, (16 - offset), size, (16 - offset));
        this.downAabb = Block.box(offset, (16 - size), offset, (16 - offset), 16.0D, (16 - offset));
        this.northAabb = Block.box(offset, offset, (16 - size), (16 - offset), (16 - offset), 16.0D);
        this.southAabb = Block.box(offset, offset, 0.0D, (16 - offset), (16 - offset), size);
        this.eastAabb = Block.box(0.0D, offset, offset, size, (16 - offset), (16 - offset));
        this.diamondEastAabb = this.eastAabb;
        this.westAabb = Block.box((16 - size), offset, offset, 16.0D, (16 - offset), (16 - offset));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    public boolean onDestroyedByPlayer(BlockState state, Level level, BlockPos pos, Player player, boolean willHarvest, FluidState fluid) {
        if (player instanceof ServerPlayer serverPlayer && level instanceof ServerLevel serverLevel
                && !player.isCreative()
                && SaltConfiguration.SALT_CLUSTER_GROWING_ENABLED.get()
                && state.is(SaltBlocks.SALT_CLUSTER.get())
                && level.getBlockState(pos.below()).is(SaltTags.Blocks.SALT_CLUSTER_GROWABLES)
                && ISaltBlock.getFluidDrippingOn(serverLevel, pos) == Fluids.WATER) {
            SaltCriteriaTriggers.HARVEST_SALT_CRYSTAL.get().trigger(serverPlayer);
        }

        return super.onDestroyedByPlayer(state, level, pos, player, willHarvest, fluid);
    }

    @Override
    public void onProjectileHit(Level level, @NotNull BlockState blockState, BlockHitResult blockHitResult, @NotNull Projectile projectile) {
        BlockPos blockpos = blockHitResult.getBlockPos();
        if (!level.isClientSide && projectile.mayInteract(level, blockpos) && projectile instanceof ThrownTrident
                && projectile.getDeltaMovement().length() > 0.6D) {
            level.destroyBlock(blockpos, true);
        }
    }

    @Override
    public void onRemove(@NotNull BlockState state, Level level, @NotNull BlockPos pos, @NotNull BlockState newState, boolean isMoving) {
        if (!level.isClientSide && !newState.getFluidState().is(Fluids.EMPTY)) {
            level.playSound(null, pos, SaltSounds.SALT_DISSOLVE.get(), SoundSource.BLOCKS, 0.8f,
                    level.getRandom().nextFloat() * 0.2f + 0.9f);
        }

        super.onRemove(state, level, pos, newState, isMoving);
    }

    @Override
    public void animateTick(@NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos, @NotNull RandomSource random) {
        onSaltAnimateTick(state, level, pos, random);
    }

    @Override
    public void randomTick(@NotNull BlockState state, @NotNull ServerLevel level, @NotNull BlockPos pos, @NotNull RandomSource random) {
        onSaltRandomTick(state, level, pos, random);
    }

    @Override
    public void tick(@NotNull BlockState state, @NotNull ServerLevel level, @NotNull BlockPos pos, @NotNull RandomSource random) {
        if (ISaltBlock.canGrowCluster(pos, level)) {
            Fluid drippingFluid = ISaltBlock.getFluidDrippingOn(level, pos);

            if (drippingFluid == Fluids.WATER) {
                if (SaltConfiguration.SALT_CLUSTER_GROWING_ENABLED.get() && random.nextDouble() < SaltConfiguration.SALT_CLUSTER_GROWING_CHANCE.get())
                    ISaltBlock.growCluster(state, pos.below(), level);
            } else if (drippingFluid != Fluids.EMPTY)
                level.destroyBlock(pos, false);
        }
    }

    @Override
    public @NotNull VoxelShape getShape(BlockState state, @NotNull BlockGetter level, @NotNull BlockPos pos, @NotNull CollisionContext context) {
        Direction direction = state.getValue(FACING);
        return switch (direction) {
            case DOWN -> this.downAabb;
            case UP -> this.upAabb;
            case NORTH -> this.northAabb;
            case SOUTH -> this.southAabb;
            case WEST -> this.westAabb;
            case EAST -> this.eastAabb;
        };
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        Direction direction = state.getValue(FACING);
        BlockPos blockpos = pos.relative(direction.getOpposite());
        return level.getBlockState(blockpos).isFaceSturdy(level, blockpos, direction);
    }

    @Override
    public @NotNull BlockState updateShape(BlockState state, @NotNull Direction direction, @NotNull BlockState neighborState, @NotNull LevelAccessor pLevel, @NotNull BlockPos pCurrentPos, @NotNull BlockPos pNeighborPos) {
        return direction == state.getValue(FACING).getOpposite() && !state.canSurvive(pLevel, pCurrentPos) ?
                Blocks.AIR.defaultBlockState() :
                super.updateShape(state, direction, neighborState, pLevel, pCurrentPos, pNeighborPos);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getClickedFace());
    }

    @Override
    public @NotNull BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    public @NotNull BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> pBuilder) {
        pBuilder.add(FACING);
    }

    @Override
    public @NotNull PushReaction getPistonPushReaction(@NotNull BlockState state) {
        return PushReaction.DESTROY;
    }

    @Override
    public @NotNull BlockState getDissolvedState(BlockState originalState, ServerLevel level, BlockPos pos, Fluid fluid) {
        return Blocks.AIR.defaultBlockState();
    }
}

