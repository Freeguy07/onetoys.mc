package com.onetoys.client;

import com.onetoys.module.gameplay.mobs.SpecialMobTweaks;
import com.onetoys.module.gameplay.mobs.goal.SlipperySlimeDashGoal;
import com.onetoys.registry.OnetoysAttachments;
import net.minecraft.client.Minecraft;
import net.minecraft.client.ParticleStatus;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

public class SpecialMobClientHelper {

    public static void onClientEntityTick(EntityTickEvent.Pre event) {
        if (!event.getEntity().level().isClientSide()) {
            return;
        }

        if (event.getEntity() instanceof Slime slime) {
            int swell = slime.getData(OnetoysAttachments.SLIME_SWELL_TICKS);
            if (swell > 0) {
                if (swell < SlipperySlimeDashGoal.MAX_SWELL_TICKS) {
                    slime.setData(OnetoysAttachments.SLIME_SWELL_TICKS, swell + 1);
                } else if (swell > SlipperySlimeDashGoal.MAX_SWELL_TICKS + 15) {
                    slime.setData(OnetoysAttachments.SLIME_SWELL_TICKS, 0);
                }
            }
        }

        if (!SpecialMobTweaks.isTweakEnabled()) {
            return;
        }

        if (!(event.getEntity() instanceof Mob mob) || !mob.isAlive() || mob.isInvisible()) {
            return;
        }

        String specialType = mob.getData(OnetoysAttachments.SPECIAL_MOB_TYPE);
        if (specialType == null || specialType.isEmpty() || !SpecialMobTweaks.isTypeEnabled(specialType)) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        ParticleStatus particleStatus = mc.options.particles().get();
        if (particleStatus == ParticleStatus.MINIMAL) {
            return;
        }
        if (particleStatus == ParticleStatus.DECREASED && (mob.tickCount & 1) != 0) {
            return;
        }

        LivingEntity camera = mc.cameraEntity instanceof LivingEntity livingCamera ? livingCamera : mc.player;
        if (camera != null && mob.distanceToSqr(camera) > 3025.0D) { // 55 blocks
            return;
        }

        Level level = mob.level();
        RandomSource random = mob.getRandom();
        AABB bb = mob.getBoundingBox();
        double width = bb.maxX - bb.minX;
        double height = bb.maxY - bb.minY;
        double centerX = (bb.minX + bb.maxX) * 0.5D;
        double centerZ = (bb.minZ + bb.maxZ) * 0.5D;

        Vec3 vel = mob.getDeltaMovement();
        boolean isMoving = (vel.x * vel.x + vel.z * vel.z) > 0.002D;

        // 1. Ambient crackling sparks around the outer contour of the mob
        int count = isMoving ? 2 : 1;
        for (int i = 0; i < count; i++) {
            if (random.nextFloat() < 0.65F) {
                float angle = random.nextFloat() * Mth.TWO_PI;
                double radius = (width * 0.55D) + random.nextDouble() * 0.18D;
                double x = centerX + Mth.cos(angle) * radius;
                double y = bb.minY + 0.15D + random.nextDouble() * (height * 0.85D);
                double z = centerZ + Mth.sin(angle) * radius;

                // Outward and slightly upward spark velocity
                double vx = Mth.cos(angle) * (0.04D + random.nextDouble() * 0.06D) - vel.x * 0.2D;
                double vy = 0.02D + random.nextDouble() * 0.06D;
                double vz = Mth.sin(angle) * (0.04D + random.nextDouble() * 0.06D) - vel.z * 0.2D;

                level.addParticle(ParticleTypes.ELECTRIC_SPARK, x, y, z, vx, vy, vz);
            }
        }

        // 2. Periodic golden/orange spark glints (WAX_ON / CRIT) for better visibility in the dark
        if (((mob.tickCount + mob.getId()) % 4 == 0) && random.nextFloat() < 0.75F) {
            float angle = random.nextFloat() * Mth.TWO_PI;
            double radius = width * 0.5D;
            double x = centerX + Mth.cos(angle) * radius;
            double y = bb.minY + height * (0.3D + random.nextDouble() * 0.65D);
            double z = centerZ + Mth.sin(angle) * radius;

            level.addParticle(
                    ParticleTypes.WAX_ON,
                    x, y, z,
                    (random.nextDouble() - 0.5D) * 0.15D,
                    0.05D + random.nextDouble() * 0.1D,
                    (random.nextDouble() - 0.5D) * 0.15D
            );
        }

        // 3. Occasional mini-burst of flying sparks (every ~1.5 seconds) so the player can spot special mobs from afar
        if ((mob.tickCount + (mob.getId() * 7)) % 30 == 0) {
            double burstY = bb.minY + height * 0.6D;
            for (int i = 0; i < 4; i++) {
                float angle = (Mth.TWO_PI * i / 4.0F) + random.nextFloat() * 0.5F;
                double speed = 0.10D + random.nextDouble() * 0.08D;
                double vx = Mth.cos(angle) * speed;
                double vy = 0.06D + random.nextDouble() * 0.08D;
                double vz = Mth.sin(angle) * speed;

                level.addParticle(
                        ParticleTypes.ELECTRIC_SPARK,
                        centerX + Mth.cos(angle) * (width * 0.35D),
                        burstY,
                        centerZ + Mth.sin(angle) * (width * 0.35D),
                        vx, vy, vz
                );
            }
            if (SpecialMobTweaks.TYPE_WARLOCK_WITHER_SKELETON.equals(specialType)) {
                level.addParticle(
                        ParticleTypes.SOUL_FIRE_FLAME,
                        centerX + (random.nextDouble() - 0.5D) * width * 0.6D,
                        bb.minY + height * 0.8D,
                        centerZ + (random.nextDouble() - 0.5D) * width * 0.6D,
                        0.0D, 0.03D, 0.0D
                );
            }
        }
    }
}
