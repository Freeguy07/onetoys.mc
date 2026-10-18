package com.onetoys.mixin;

import com.onetoys.module.gameplay.mobs.MobAiTweaks;
import javax.annotation.Nullable;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(TargetGoal.class)
public abstract class TargetGoalMixin {

    @Shadow
    @Final
    protected Mob mob;

    @Shadow
    protected int unseenTicks;

    @Shadow
    @Nullable
    protected LivingEntity targetMob;

    @Inject(method = "canContinueToUse", at = @At("HEAD"), cancellable = true)
    private void Onetoys$onCanContinueToUse(CallbackInfoReturnable<Boolean> cir) {
        if (!MobAiTweaks.isFixTargetLossAndCombatPause()) {
            return;
        }

        LivingEntity target = this.mob.getTarget();
        if (target == null) {
            target = this.targetMob;
        }

        // Fix MC-149563 (Dimension Sanity Check)
        if (target != null && target.level() != this.mob.level()) {
            cir.setReturnValue(false);
            return;
        }

        // Prevent close-range target loss due to obstructed line-of-sight raycasts
        if (target != null && target.isAlive() && !target.isSpectator()) {
            double distSqr = this.mob.distanceToSqr(target);
            boolean recentlyInCombat = (this.mob.getLastHurtByMob() == target && this.mob.tickCount - this.mob.getLastHurtByMobTimestamp() < 100)
                    || (this.mob.getLastHurtMob() == target && this.mob.tickCount - this.mob.getLastHurtMobTimestamp() < 100);

            if (distSqr <= 25.0D || recentlyInCombat) {
                this.unseenTicks = 0;
            }
        }
    }
}
