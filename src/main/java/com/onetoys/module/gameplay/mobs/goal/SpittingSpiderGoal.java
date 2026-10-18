package com.onetoys.module.gameplay.mobs.goal;

import com.onetoys.entity.TangledThreadProjectile;
import com.onetoys.module.gameplay.mobs.SpecialMobTweaks;
import com.onetoys.registry.OnetoysItems;
import java.util.EnumSet;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.monster.Spider;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

public class SpittingSpiderGoal extends Goal {

    private static final int SPIT_INTERVAL_TICKS = 110; // ~5.5 seconds between spits
    private static final double RETREAT_TRIGGER_DIST_SQR = 25.0D; // 5.0 blocks
    private static final double RETREAT_SAFE_DIST_SQR = 49.0D;    // 7.0 blocks
    private static final double APPROACH_DIST_SQR = 121.0D;       // 11.0 blocks

    private final Spider spider;
    private int attackCooldown = 40;
    private int retreatRecalcTicks = 0;
    private boolean retreating = false;

    public SpittingSpiderGoal(Spider spider) {
        this.spider = spider;
        // Claim MOVE, LOOK, and JUMP so vanilla SpiderAttackGoal and LeapAtTargetGoal cannot execute
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK, Goal.Flag.JUMP));
    }

    @Override
    public boolean canUse() {
        if (!SpecialMobTweaks.isSpittingSpiderEnabled()
                || !SpecialMobTweaks.isSpecialMob(this.spider, SpecialMobTweaks.TYPE_SPITTING_SPIDER)) {
            return false;
        }
        LivingEntity target = this.spider.getTarget();
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
        this.attackCooldown = 40;
        this.retreatRecalcTicks = 0;
        this.retreating = false;
        this.spider.getMoveControl().strafe(0.0F, 0.0F);
        this.spider.setXxa(0.0F);
    }

    @Override
    public void stop() {
        this.retreating = false;
        this.spider.getNavigation().stop();
        this.spider.getMoveControl().strafe(0.0F, 0.0F);
        this.spider.setXxa(0.0F);
        this.spider.setZza(0.0F);
    }

    @Override
    public void tick() {
        LivingEntity target = this.spider.getTarget();
        if (target == null) {
            return;
        }

        double distSqr = this.spider.distanceToSqr(target);
        boolean hasLineOfSight = this.spider.getSensing().hasLineOfSight(target);

        // State transitions with hysteresis so the spider never oscillates back and forth
        if (distSqr < RETREAT_TRIGGER_DIST_SQR) {
            this.retreating = true;
        } else if (distSqr >= RETREAT_SAFE_DIST_SQR) {
            this.retreating = false;
        }

        if (this.retreatRecalcTicks > 0) {
            this.retreatRecalcTicks--;
        }

        if (this.retreating) {
            // Smoothly run away from the target using navigation instead of blind backward strafing
            this.spider.getLookControl().setLookAt(target, 30.0F, 30.0F);
            if (this.retreatRecalcTicks <= 0 || this.spider.getNavigation().isDone()) {
                this.retreatRecalcTicks = 12;
                Vec3 awayPos = DefaultRandomPos.getPosAway(this.spider, 10, 5, target.position());
                if (awayPos != null && target.distanceToSqr(awayPos) > distSqr) {
                    this.spider.getNavigation().moveTo(awayPos.x, awayPos.y, awayPos.z, 1.25D);
                } else {
                    // Direct fallback step away if cornered
                    Vec3 dir = this.spider.position().subtract(target.position());
                    double len = Math.sqrt(dir.x * dir.x + dir.z * dir.z);
                    if (len > 0.001D) {
                        double destX = this.spider.getX() + (dir.x / len) * 5.0D;
                        double destZ = this.spider.getZ() + (dir.z / len) * 5.0D;
                        this.spider.getNavigation().moveTo(destX, this.spider.getY(), destZ, 1.25D);
                    }
                }
            }
        } else if (distSqr > APPROACH_DIST_SQR || !hasLineOfSight) {
            // Move closer until within comfortable spitting range and line of sight
            this.spider.getLookControl().setLookAt(target, 30.0F, 30.0F);
            if (this.retreatRecalcTicks <= 0 || this.spider.getNavigation().isDone()) {
                this.retreatRecalcTicks = 10;
                this.spider.getNavigation().moveTo(target, 1.0D);
            }
        } else {
            // Optimal firing zone (5.0 - 11.0 blocks): hold position, face the target steadily, and do not jitter
            this.spider.getNavigation().stop();
            this.spider.getMoveControl().strafe(0.0F, 0.0F);
            this.spider.setXxa(0.0F);
            this.spider.setZza(0.0F);
            this.spider.getLookControl().setLookAt(target, 30.0F, 30.0F);
            this.spider.setYRot(Mth.rotateIfNecessary(this.spider.getYRot(), this.spider.yHeadRot, 0.0F));
        }

        if (this.attackCooldown > 0) {
            this.attackCooldown--;
        }

        if (this.attackCooldown <= 0 && hasLineOfSight && distSqr >= 9.0D && distSqr <= 196.0D) {
            // Do not spam another cobweb on a target that is already trapped in a cobweb
            if (isTargetAlreadyInCobweb(target)) {
                this.attackCooldown = 30;
                return;
            }
            spitTangledThread(target);
            this.attackCooldown = SPIT_INTERVAL_TICKS;
        }
    }

    private boolean isTargetAlreadyInCobweb(LivingEntity target) {
        BlockPos feetPos = target.blockPosition();
        return target.level().getBlockState(feetPos).is(Blocks.COBWEB)
                || target.level().getBlockState(feetPos.above()).is(Blocks.COBWEB);
    }

    private void spitTangledThread(LivingEntity target) {
        TangledThreadProjectile projectile = new TangledThreadProjectile(this.spider.level(), this.spider);
        projectile.setItem(new ItemStack(OnetoysItems.MATTED_WOOL.get()));
        projectile.setPos(
                this.spider.getX(),
                this.spider.getEyeY() - 0.1D,
                this.spider.getZ()
        );

        double dx = target.getX() - projectile.getX();
        double dy = target.getY(0.3333333333333333D) - projectile.getY();
        double dz = target.getZ() - projectile.getZ();
        double horizontalDist = Math.sqrt(dx * dx + dz * dz);

        projectile.shoot(dx, dy + horizontalDist * 0.18D, dz, 1.35F, 3.0F);
        this.spider.playSound(
                SoundEvents.LLAMA_SPIT,
                1.0F,
                0.75F + this.spider.getRandom().nextFloat() * 0.25F
        );
        this.spider.level().addFreshEntity(projectile);
    }
}
