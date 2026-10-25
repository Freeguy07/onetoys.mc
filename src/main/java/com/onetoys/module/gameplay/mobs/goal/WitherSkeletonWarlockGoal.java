package com.onetoys.module.gameplay.mobs.goal;

import com.onetoys.module.gameplay.mobs.SpecialMobTweaks;
import java.util.EnumSet;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.monster.WitherSkeleton;
import net.minecraft.world.entity.projectile.WitherSkull;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

public class WitherSkeletonWarlockGoal extends Goal {

    private static final int NORMAL_SKULL_INTERVAL_TICKS = 140; // 7 seconds
    private static final int BLUE_SKULL_INTERVAL_TICKS = 600;   // 30 seconds (half a minute)
    private static final double RETREAT_TRIGGER_DIST_SQR = 36.0D; // 6.0 blocks
    private static final double RETREAT_SAFE_DIST_SQR = 64.0D;    // 8.0 blocks
    private static final double APPROACH_DIST_SQR = 196.0D;       // 14.0 blocks

    private final WitherSkeleton witherSkeleton;
    private int normalSkullCooldown = 60;
    private int blueSkullCooldown = 600;
    private int navRecalcTicks = 0;
    private boolean retreating = false;

    public WitherSkeletonWarlockGoal(WitherSkeleton witherSkeleton) {
        this.witherSkeleton = witherSkeleton;
        // Claim MOVE, LOOK, and JUMP so vanilla MeleeAttackGoal is completely overridden
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK, Goal.Flag.JUMP));
    }

    @Override
    public boolean canUse() {
        if (!SpecialMobTweaks.isWarlockWitherSkeletonEnabled()
                || !SpecialMobTweaks.isSpecialMob(this.witherSkeleton, SpecialMobTweaks.TYPE_WARLOCK_WITHER_SKELETON)) {
            return false;
        }
        LivingEntity target = this.witherSkeleton.getTarget();
        return target != null && target.isAlive();
    }

    @Override
    public boolean canContinueToUse() {
        return this.canUse();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void start() {
        this.witherSkeleton.setAggressive(true);
        this.retreating = false;
        this.navRecalcTicks = 0;
        this.witherSkeleton.getMoveControl().strafe(0.0F, 0.0F);
        this.witherSkeleton.setXxa(0.0F);
        if (!this.witherSkeleton.getMainHandItem().isEmpty()) {
            this.witherSkeleton.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
        }
    }

    @Override
    public void stop() {
        this.retreating = false;
        this.witherSkeleton.setAggressive(false);
        this.witherSkeleton.getNavigation().stop();
        this.witherSkeleton.getMoveControl().strafe(0.0F, 0.0F);
        this.witherSkeleton.setXxa(0.0F);
        this.witherSkeleton.setZza(0.0F);
    }

    @Override
    public void tick() {
        if (!this.witherSkeleton.getMainHandItem().isEmpty()) {
            this.witherSkeleton.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
        }

        LivingEntity target = this.witherSkeleton.getTarget();
        if (target == null) {
            return;
        }

        double distSqr = this.witherSkeleton.distanceToSqr(target);
        boolean hasLineOfSight = this.witherSkeleton.getSensing().hasLineOfSight(target);

        if (distSqr < RETREAT_TRIGGER_DIST_SQR) {
            this.retreating = true;
        } else if (distSqr >= RETREAT_SAFE_DIST_SQR) {
            this.retreating = false;
        }

        if (this.navRecalcTicks > 0) {
            this.navRecalcTicks--;
        }

        if (this.retreating) {
            this.witherSkeleton.getLookControl().setLookAt(target, 30.0F, 30.0F);
            if (this.navRecalcTicks <= 0 || this.witherSkeleton.getNavigation().isDone()) {
                this.navRecalcTicks = 12;
                Vec3 awayPos = DefaultRandomPos.getPosAway(this.witherSkeleton, 12, 6, target.position());
                if (awayPos != null && target.distanceToSqr(awayPos) > distSqr) {
                    this.witherSkeleton.getNavigation().moveTo(awayPos.x, awayPos.y, awayPos.z, 1.2D);
                } else {
                    Vec3 dir = this.witherSkeleton.position().subtract(target.position());
                    double len = Math.sqrt(dir.x * dir.x + dir.z * dir.z);
                    if (len > 0.001D) {
                        double destX = this.witherSkeleton.getX() + (dir.x / len) * 6.0D;
                        double destZ = this.witherSkeleton.getZ() + (dir.z / len) * 6.0D;
                        this.witherSkeleton.getNavigation().moveTo(destX, this.witherSkeleton.getY(), destZ, 1.2D);
                    }
                }
            }
        } else if (distSqr > APPROACH_DIST_SQR || !hasLineOfSight) {
            this.witherSkeleton.getLookControl().setLookAt(target, 30.0F, 30.0F);
            if (this.navRecalcTicks <= 0 || this.witherSkeleton.getNavigation().isDone()) {
                this.navRecalcTicks = 10;
                this.witherSkeleton.getNavigation().moveTo(target, 1.0D);
            }
        } else {
            // Optimal casting zone (6.0 - 14.0 blocks): stand firm and aim at the target
            this.witherSkeleton.getNavigation().stop();
            this.witherSkeleton.getMoveControl().strafe(0.0F, 0.0F);
            this.witherSkeleton.setXxa(0.0F);
            this.witherSkeleton.setZza(0.0F);
            this.witherSkeleton.getLookControl().setLookAt(target, 30.0F, 30.0F);
            this.witherSkeleton.setYRot(Mth.rotateIfNecessary(this.witherSkeleton.getYRot(), this.witherSkeleton.yHeadRot, 0.0F));
        }

        if (this.normalSkullCooldown > 0) {
            this.normalSkullCooldown--;
        }
        if (this.blueSkullCooldown > 0) {
            this.blueSkullCooldown--;
        }

        if (hasLineOfSight && distSqr <= 400.0D) {
            if (this.blueSkullCooldown <= 0) {
                shootWitherSkull(target, true);
                this.blueSkullCooldown = BLUE_SKULL_INTERVAL_TICKS;
                this.normalSkullCooldown = NORMAL_SKULL_INTERVAL_TICKS;
            } else if (this.normalSkullCooldown <= 0) {
                shootWitherSkull(target, false);
                this.normalSkullCooldown = NORMAL_SKULL_INTERVAL_TICKS;
            }
        }
    }

    private void shootWitherSkull(LivingEntity target, boolean dangerousBlueSkull) {
        this.witherSkeleton.swing(InteractionHand.MAIN_HAND);
        if (!this.witherSkeleton.isSilent()) {
            this.witherSkeleton.level().levelEvent(null, 1024, this.witherSkeleton.blockPosition(), 0);
        } else {
            this.witherSkeleton.playSound(SoundEvents.WITHER_SHOOT, 1.0F, 1.0F);
        }

        double headX = this.witherSkeleton.getX();
        double headY = this.witherSkeleton.getEyeY() - 0.1D;
        double headZ = this.witherSkeleton.getZ();

        double dx = target.getX() - headX;
        double dy = target.getY(0.5D) - headY;
        double dz = target.getZ() - headZ;
        Vec3 dir = new Vec3(dx, dy, dz).normalize();

        WitherSkull skull = new WitherSkull(this.witherSkeleton.level(), this.witherSkeleton, dir);
        skull.setOwner(this.witherSkeleton);
        if (dangerousBlueSkull) {
            skull.setDangerous(true);
        }
        skull.setPosRaw(headX + dir.x * 0.5D, headY, headZ + dir.z * 0.5D);
        this.witherSkeleton.level().addFreshEntity(skull);
    }
}
