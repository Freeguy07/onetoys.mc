package com.onetoys.client;

import com.onetoys.module.gameplay.wetness.WetnessTweaks;

import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

/**
 * Ultra-fast client-only helper for tracking the active entity being rendered,
 * applying wetness darkening to models and layers with zero per-cube allocations,
 * and managing client dripping particles with distance culling.
 */
@OnlyIn(Dist.CLIENT)
public class WetnessClientHelper {
    private static final int MAX_DEPTH = 16;
    private static final boolean[] WET_STACK = new boolean[MAX_DEPTH];
    private static int depth = 0;

    public static void pushEntity(LivingEntity entity) {
        if (depth < MAX_DEPTH) {
            boolean isWet = WetnessTweaks.isTweakEnabled() && entity != null && !(entity instanceof Wolf) && WetnessTweaks.isWet(entity);
            WET_STACK[depth] = isWet;
        }
        depth++;
    }

    public static void popEntity() {
        if (depth > 0) {
            depth--;
        }
    }

    public static int darkenIfWet(int originalColor) {
        if (depth > 0 && depth <= MAX_DEPTH && WET_STACK[depth - 1]) {
            int a = originalColor & 0xFF000000;
            int r = (((originalColor >>> 16) & 0xFF) * 3) >> 2;
            int g = (((originalColor >>> 8) & 0xFF) * 3) >> 2;
            int b = ((originalColor & 0xFF) * 3) >> 2;
            return a | (r << 16) | (g << 8) | b;
        }
        return originalColor;
    }

    public static void onClientEntityTick(EntityTickEvent.Pre event) {
        // Fast-path 1: Ignore server-side entities immediately on integrated server
        if (!event.getEntity().level().isClientSide()) {
            return;
        }

        // Fast-path 2: Master toggle check
        if (!WetnessTweaks.isTweakEnabled()) {
            return;
        }

        // Fast-path 3: Only living entities
        if (!(event.getEntity() instanceof LivingEntity entity)) {
            return;
        }

        // Fast-path 4: Wolves shake naturally, submerged entities don't drip, and dry entities don't drip
        if (entity instanceof Wolf || entity.isInWaterOrBubble() || !WetnessTweaks.isWet(entity)) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();

        // Fast-path 5: Respect user video settings (MINIMAL particles disables decorative dripping)
        net.minecraft.client.ParticleStatus particleStatus = mc.options.particles().get();
        if (particleStatus == net.minecraft.client.ParticleStatus.MINIMAL) {
            return;
        }
        if (particleStatus == net.minecraft.client.ParticleStatus.DECREASED && (entity.tickCount & 1) != 0) {
            return;
        }

        // Fast-path 6: Distance culling (skip entities further than 32 blocks from camera)
        LivingEntity camera = mc.cameraEntity instanceof LivingEntity livingCamera ? livingCamera : mc.player;
        if (camera != null && entity.distanceToSqr(camera) > 1024.0) {
            return;
        }

        Vec3 movement = entity.getDeltaMovement();
        double horizontalSpeedSqr = movement.x * movement.x + movement.z * movement.z;
        boolean isMoving = horizontalSpeedSqr > 0.001;
        float spawnChance = isMoving ? 0.55F : 0.20F;

        if (entity.getRandom().nextFloat() < spawnChance) {
            Level level = entity.level();
            AABB bb = entity.getBoundingBox();
            double lerp = isMoving ? entity.getRandom().nextDouble() * 0.8 : 0.0;
            double x = (bb.minX + entity.getRandom().nextDouble() * (bb.maxX - bb.minX)) - movement.x * lerp;
            double y = (bb.minY + entity.getRandom().nextDouble() * (bb.maxY - bb.minY) * 0.75) - movement.y * lerp;
            double z = (bb.minZ + entity.getRandom().nextDouble() * (bb.maxZ - bb.minZ)) - movement.z * lerp;

            level.addParticle(ParticleTypes.FALLING_WATER, x, y, z, 0.0, 0.0, 0.0);

            // When moving fast (sprinting/riding), occasionally fling small splash droplets
            if (horizontalSpeedSqr > 0.01 && entity.getRandom().nextFloat() < 0.25F) {
                double spreadX = (entity.getRandom().nextDouble() - 0.5) * 0.04;
                double spreadZ = (entity.getRandom().nextDouble() - 0.5) * 0.04;
                level.addParticle(ParticleTypes.SPLASH, x, y, z,
                        movement.x * 0.25 + spreadX, 0.0, movement.z * 0.25 + spreadZ);
            }
        }
    }
}
