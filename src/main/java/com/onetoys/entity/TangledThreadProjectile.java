package com.onetoys.entity;

import com.onetoys.registry.OnetoysEntityTypes;
import com.onetoys.registry.OnetoysItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

public class TangledThreadProjectile extends ThrowableItemProjectile {

    public TangledThreadProjectile(EntityType<? extends TangledThreadProjectile> entityType, Level level) {
        super(entityType, level);
    }

    public TangledThreadProjectile(Level level, LivingEntity shooter) {
        super(OnetoysEntityTypes.TANGLED_THREAD.get(), shooter, level);
    }

    public TangledThreadProjectile(Level level, double x, double y, double z) {
        super(OnetoysEntityTypes.TANGLED_THREAD.get(), x, y, z, level);
    }

    @Override
    protected Item getDefaultItem() {
        return OnetoysItems.MATTED_WOOL.get();
    }

    private ParticleOptions getParticle() {
        ItemStack stack = this.getItem();
        return new ItemParticleOption(ParticleTypes.ITEM, stack.isEmpty() ? new ItemStack(this.getDefaultItem()) : stack);
    }

    @Override
    public void handleEntityEvent(byte id) {
        if (id == 3) {
            ParticleOptions particleOptions = this.getParticle();
            for (int i = 0; i < 8; i++) {
                this.level().addParticle(particleOptions, this.getX(), this.getY(), this.getZ(), 0.0D, 0.0D, 0.0D);
            }
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        Entity target = result.getEntity();
        target.hurt(this.damageSources().thrown(this, this.getOwner()), 0.1F);

        if (!this.level().isClientSide) {
            tryPlaceCobweb(target.blockPosition());
        }
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        super.onHitBlock(result);
        if (!this.level().isClientSide) {
            BlockPos hitPos = result.getBlockPos();
            BlockState hitState = this.level().getBlockState(hitPos);
            if (canPlaceCobwebAt(hitPos, hitState)) {
                this.level().setBlock(hitPos, Blocks.COBWEB.defaultBlockState(), 3);
            } else {
                Direction face = result.getDirection();
                BlockPos adjacentPos = hitPos.relative(face);
                tryPlaceCobweb(adjacentPos);
            }
        }
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (!this.level().isClientSide) {
            this.level().broadcastEntityEvent(this, (byte) 3);
            this.discard();
        }
    }

    private boolean canPlaceCobwebAt(BlockPos pos, BlockState state) {
        return (state.isAir() || state.canBeReplaced()) && state.getFluidState().isEmpty() && this.level().isInWorldBounds(pos);
    }

    private void tryPlaceCobweb(BlockPos centerPos) {
        BlockState state = this.level().getBlockState(centerPos);
        if (canPlaceCobwebAt(centerPos, state)) {
            this.level().setBlock(centerPos, Blocks.COBWEB.defaultBlockState(), 3);
            return;
        }
        BlockPos abovePos = centerPos.above();
        BlockState aboveState = this.level().getBlockState(abovePos);
        if (canPlaceCobwebAt(abovePos, aboveState)) {
            this.level().setBlock(abovePos, Blocks.COBWEB.defaultBlockState(), 3);
        }
    }
}
