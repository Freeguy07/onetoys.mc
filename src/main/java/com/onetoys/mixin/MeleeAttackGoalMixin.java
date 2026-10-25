package com.onetoys.mixin;

import com.onetoys.module.gameplay.mobs.MobAiTweaks;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MeleeAttackGoal.class)
public abstract class MeleeAttackGoalMixin {

    @Shadow
    @Final
    protected PathfinderMob mob;

    @Shadow
    @Final
    private double speedModifier;

    @Shadow
    private int ticksUntilNextPathRecalculation;

    @Shadow
    private int ticksUntilNextAttack;

    @Shadow
    private long lastCanUseCheck;

    @Unique
    private long Onetoys$nextAttackGameTime = 0L;

    @Inject(method = "canUse", at = @At("HEAD"), cancellable = true)
    private void Onetoys$onCanUse(CallbackInfoReturnable<Boolean> cir) {
        if (MobAiTweaks.isFixTargetLossAndCombatPause()) {
            LivingEntity target = this.mob.getTarget();
            if (target != null && target.level() != this.mob.level()) {
                cir.setReturnValue(false);
                return;
            }
            // Fix MC-198068: 20-tick pause due to lastCanUseCheck throttling
            if (target != null && target.isAlive() && this.mob.distanceToSqr(target) <= 16.0D) {
                this.lastCanUseCheck = 0L;
            }
        }
    }

    @Inject(method = "canContinueToUse", at = @At("HEAD"), cancellable = true)
    private void Onetoys$onCanContinueToUse(CallbackInfoReturnable<Boolean> cir) {
        if (MobAiTweaks.isFixTargetLossAndCombatPause()) {
            LivingEntity target = this.mob.getTarget();
            if (target == null || !target.isAlive() || target.level() != this.mob.level()) {
                cir.setReturnValue(false);
                return;
            }
            if (target instanceof Player player && (player.isSpectator() || player.isCreative())) {
                cir.setReturnValue(false);
                return;
            }
            // Fix MC-198068 / MC-278596: keep goal active at close range even if navigation finishes
            if (this.mob.distanceToSqr(target) <= 16.0D
                    && Math.abs(target.getY() - this.mob.getY()) <= 2.5D
                    && this.mob.isWithinRestriction(target.blockPosition())
                    && this.mob.getSensing().hasLineOfSight(target)) {
                cir.setReturnValue(true);
            }
        }
    }

    @Inject(method = "start", at = @At("TAIL"))
    private void Onetoys$onStart(CallbackInfo ci) {
        // Fix MC-191642: prevent instant double-attack when MeleeAttackGoal restarts
        if (MobAiTweaks.isFixTargetLossAndCombatPause()) {
            long now = this.mob.level().getGameTime();
            if (this.Onetoys$nextAttackGameTime > now) {
                this.ticksUntilNextAttack = (int) (this.Onetoys$nextAttackGameTime - now);
            }
        }
    }

    @Inject(method = "resetAttackCooldown", at = @At("TAIL"))
    private void Onetoys$onResetAttackCooldown(CallbackInfo ci) {
        this.Onetoys$nextAttackGameTime = this.mob.level().getGameTime() + this.ticksUntilNextAttack;
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void Onetoys$onTick(CallbackInfo ci) {
        LivingEntity target = this.mob.getTarget();
        if (target == null) {
            return;
        }

        if (MobAiTweaks.isFixTargetLossAndCombatPause()) {
            // When the mob is hurt (e.g. knocked back after taking damage), immediately prompt
            // a fast path recalculation so the mob reacts instantly instead of standing still.
            if (this.mob.hurtTime > 0 && this.ticksUntilNextPathRecalculation > 2) {
                this.ticksUntilNextPathRecalculation = 2;
            }

            // If navigation finished or was interrupted (e.g. knockback) while in combat reach,
            // step directly towards the target using MoveControl so the mob never freezes.
            if (this.mob.distanceToSqr(target) <= 16.0D
                    && Math.abs(target.getY() - this.mob.getY()) <= 2.0D
                    && this.mob.getNavigation().isDone()
                    && this.mob.getSensing().hasLineOfSight(target)) {
                this.mob.getMoveControl().setWantedPosition(target.getX(), target.getY(), target.getZ(), this.speedModifier);
            }
        }
    }
}
