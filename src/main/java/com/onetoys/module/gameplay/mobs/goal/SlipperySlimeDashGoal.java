package com.onetoys.module.gameplay.mobs.goal;

import com.onetoys.Onetoys;
import com.onetoys.module.gameplay.mobs.SpecialMobTweaks;
import com.onetoys.registry.OnetoysAttachments;
import java.util.EnumSet;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.phys.Vec3;

public class SlipperySlimeDashGoal extends Goal {

    public static final int MAX_SWELL_TICKS = 22;
    private static final int DASH_DURATION_TICKS = 14;
    private static final int COOLDOWN_TICKS = 45;

    private static final ResourceLocation DASH_STEP_HEIGHT_ID =
            ResourceLocation.fromNamespaceAndPath(Onetoys.MODID, "slippery_slime_dash_step_height");

    // Base entity step height is 0.6; adding +0.45 gives 1.05 blocks (steps up 1 full block during dash)
    private static final AttributeModifier DASH_STEP_MODIFIER = new AttributeModifier(
            DASH_STEP_HEIGHT_ID,
            0.45D,
            AttributeModifier.Operation.ADD_VALUE
    );

    private final Slime slime;
    private int swellTicks = 0;
    private int dashTicks = 0;
    private int cooldownTicks = 0;
    private float lockedDashYaw = 0.0F;

    public SlipperySlimeDashGoal(Slime slime) {
        this.slime = slime;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK, Goal.Flag.JUMP));
    }

    @Override
    public boolean canUse() {
        if (!SpecialMobTweaks.isSlipperySlimeEnabled()
                || !SpecialMobTweaks.isSpecialMob(this.slime, SpecialMobTweaks.TYPE_SLIPPERY_SLIME)) {
            return false;
        }
        if (this.cooldownTicks > 0) {
            this.cooldownTicks--;
            return false;
        }
        if (!this.slime.onGround() || this.slime.isInWaterOrBubble() || this.slime.isInLava()) {
            return false;
        }
        LivingEntity target = this.slime.getTarget();
        if (target == null || !target.isAlive()) {
            return false;
        }
        double distSqr = this.slime.distanceToSqr(target);
        return distSqr >= 2.25D && distSqr <= 81.0D && this.slime.getSensing().hasLineOfSight(target);
    }

    @Override
    public boolean canContinueToUse() {
        if (!SpecialMobTweaks.isSlipperySlimeEnabled()
                || !SpecialMobTweaks.isSpecialMob(this.slime, SpecialMobTweaks.TYPE_SLIPPERY_SLIME)) {
            return false;
        }
        return this.swellTicks > 0 || this.dashTicks > 0;
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void start() {
        this.swellTicks = 1;
        this.dashTicks = 0;
        this.slime.setData(OnetoysAttachments.SLIME_SWELL_TICKS, 1);

        LivingEntity target = this.slime.getTarget();
        if (target != null) {
            this.slime.lookAt(target, 360.0F, 360.0F);
            this.lockedDashYaw = this.slime.getYRot();
        }
    }

    @Override
    public void tick() {
        LivingEntity target = this.slime.getTarget();

        if (this.swellTicks > 0) {
            // Stop completely while swelling up
            Vec3 vel = this.slime.getDeltaMovement();
            this.slime.setDeltaMovement(0.0D, vel.y, 0.0D);
            this.slime.setZza(0.0F);
            this.slime.setXxa(0.0F);
            this.slime.setSpeed(0.0F);

            if (target != null && target.isAlive() && this.swellTicks < MAX_SWELL_TICKS - 4) {
                this.slime.lookAt(target, 30.0F, 30.0F);
                this.lockedDashYaw = this.slime.getYRot();
            } else {
                this.slime.setYRot(this.lockedDashYaw);
                this.slime.yHeadRot = this.lockedDashYaw;
                this.slime.yBodyRot = this.lockedDashYaw;
            }

            this.slime.targetSquish = -0.45F;

            if (this.swellTicks == 1) {
                this.slime.playSound(SoundEvents.SLIME_SQUISH, 1.0F, 0.65F);
            }

            this.swellTicks++;

            if (this.swellTicks >= MAX_SWELL_TICKS) {
                // Transition from swelling to strong forward ground dash
                this.swellTicks = 0;
                this.slime.setData(OnetoysAttachments.SLIME_SWELL_TICKS, 0);
                this.dashTicks = DASH_DURATION_TICKS;
                setDashStepHeight(true);

                this.slime.targetSquish = 0.85F;
                this.slime.playSound(SoundEvents.SLIME_ATTACK, 1.2F, 0.85F);
                applyForwardSlideDash(1.15D);
            } else {
                this.slime.setData(OnetoysAttachments.SLIME_SWELL_TICKS, this.swellTicks);
            }
        } else if (this.dashTicks > 0) {
            this.slime.setYRot(this.lockedDashYaw);
            this.slime.yHeadRot = this.lockedDashYaw;
            this.slime.yBodyRot = this.lockedDashYaw;

            // Keep sliding strictly forward along the ground
            double speedFactor = 0.55D + 0.50D * ((double) this.dashTicks / DASH_DURATION_TICKS);
            applyForwardSlideDash(speedFactor);

            this.dashTicks--;
            if (this.dashTicks <= 0) {
                setDashStepHeight(false);
                this.cooldownTicks = COOLDOWN_TICKS;
            }
        }
    }

    @Override
    public void stop() {
        this.swellTicks = 0;
        this.dashTicks = 0;
        this.cooldownTicks = COOLDOWN_TICKS;
        this.slime.setData(OnetoysAttachments.SLIME_SWELL_TICKS, 0);
        setDashStepHeight(false);
    }

    private void applyForwardSlideDash(double speed) {
        float yawRad = this.lockedDashYaw * Mth.DEG_TO_RAD;
        double dx = -Mth.sin(yawRad) * speed;
        double dz = Mth.cos(yawRad) * speed;
        Vec3 current = this.slime.getDeltaMovement();
        // Do not jump into the air; keep vertical velocity <= 0 so it slides along the ground
        double dy = current.y > 0.0D ? 0.0D : current.y;
        this.slime.setDeltaMovement(dx, dy, dz);
        this.slime.hasImpulse = true;
    }

    private void setDashStepHeight(boolean active) {
        AttributeInstance stepAttr = this.slime.getAttribute(Attributes.STEP_HEIGHT);
        if (stepAttr == null) {
            return;
        }
        if (active) {
            if (!stepAttr.hasModifier(DASH_STEP_HEIGHT_ID)) {
                stepAttr.addTransientModifier(DASH_STEP_MODIFIER);
            }
        } else {
            if (stepAttr.hasModifier(DASH_STEP_HEIGHT_ID)) {
                stepAttr.removeModifier(DASH_STEP_HEIGHT_ID);
            }
        }
    }
}
