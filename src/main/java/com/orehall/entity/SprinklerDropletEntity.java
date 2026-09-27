package com.orehall.entity;

import com.orehall.module.content.WateringHelper;
import com.orehall.module.gameplay.wetness.WetnessTweaks;
import com.orehall.registry.OrehallEntityTypes;

import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Ballistic water droplet projectile launched in waves by the Sprinkler block.
 * Emits flowing water spray particles in flight and applies comprehensive watering
 * effects (farmland, crops, concrete, fire, entities) on impact before discarding.
 */
public class SprinklerDropletEntity extends ThrowableProjectile {

    public SprinklerDropletEntity(EntityType<? extends SprinklerDropletEntity> entityType, Level level) {
        super(entityType, level);
    }

    public SprinklerDropletEntity(Level level, double x, double y, double z) {
        super(OrehallEntityTypes.SPRINKLER_DROPLET.get(), level);
        setPos(x, y, z);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
    }

    @Override
    protected double getDefaultGravity() {
        return 0.05;
    }

    @Override
    protected void onHitBlock(BlockHitResult hitResult) {
        super.onHitBlock(hitResult);
        if (!level().isClientSide()) {
            WateringHelper.applyWatering(
                    level(),
                    hitResult.getBlockPos(),
                    hitResult.getDirection(),
                    hitResult.getLocation(),
                    null
            );
            discard();
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult hitResult) {
        super.onHitEntity(hitResult);
        if (!level().isClientSide()) {
            Entity target = hitResult.getEntity();
            if (target.isOnFire()) {
                target.clearFire();
            }
            if (target instanceof LivingEntity living) {
                if (living.isSensitiveToWater()) {
                    living.hurt(living.damageSources().drown(), 1.0F);
                }
                WetnessTweaks.setWet(living, WetnessTweaks.getWetDurationTicks());
            }
            WateringHelper.applyWatering(
                    level(),
                    target.blockPosition(),
                    Direction.UP,
                    target.position(),
                    null
            );
            discard();
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide() && (tickCount > 60 || getY() < level().getMinBuildHeight())) {
            discard();
            return;
        }
        if (level().isClientSide()) {
            Vec3 pos = position();
            Vec3 prev = new Vec3(xo, yo, zo);
            Vec3 motion = getDeltaMovement();

            // Sub-tick 3-step interpolation fills gaps between ticks for a continuous, silky water arc
            int steps = 3;
            for (int i = 0; i < steps; i++) {
                double t = (double) i / steps;
                double px = prev.x + (pos.x - prev.x) * t;
                double py = prev.y + (pos.y - prev.y) * t;
                double pz = prev.z + (pos.z - prev.z) * t;

                double sprayFactor = 0.28;
                double vx = motion.x * sprayFactor;
                double vy = motion.y * sprayFactor + 0.015;
                double vz = motion.z * sprayFactor;

                // Vibrant water droplet stream
                level().addParticle(ParticleTypes.FALLING_WATER, px, py, pz, vx, vy, vz);

                // Secondary fine mist/spray with slight dispersion
                double jitter = 0.035;
                double jx = vx + (random.nextDouble() - 0.5) * jitter;
                double jy = vy + (random.nextDouble() - 0.5) * jitter;
                double jz = vz + (random.nextDouble() - 0.5) * jitter;
                level().addParticle(ParticleTypes.SPLASH, px, py, pz, jx, jy, jz);
            }

            // Trailing rain droplet streak
            if ((tickCount & 1) == 0) {
                level().addParticle(ParticleTypes.RAIN, pos.x, pos.y, pos.z, 0.0, -0.05, 0.0);
            }
        }
    }
}
