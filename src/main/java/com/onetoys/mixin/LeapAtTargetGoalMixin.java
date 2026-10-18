package com.onetoys.mixin;

import com.onetoys.module.gameplay.mobs.MobAiTweaks;
import java.util.EnumSet;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LeapAtTargetGoal;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LeapAtTargetGoal.class)
public abstract class LeapAtTargetGoalMixin extends Goal {

    @Unique
    private static final EnumSet<Goal.Flag> Onetoys$JUMP_ONLY_FLAGS = EnumSet.of(Goal.Flag.JUMP);

    @Unique
    private static final EnumSet<Goal.Flag> Onetoys$VANILLA_FLAGS = EnumSet.of(Goal.Flag.JUMP, Goal.Flag.MOVE);

    @Unique
    private boolean Onetoys$jumpOnlyApplied = false;

    @Inject(method = "<init>(Lnet/minecraft/world/entity/Mob;F)V", at = @At("RETURN"))
    private void Onetoys$onInit(Mob mob, float yd, CallbackInfo ci) {
        if (MobAiTweaks.isFixLeapAttackAndHeadSnap()) {
            this.setFlags(Onetoys$JUMP_ONLY_FLAGS);
            this.Onetoys$jumpOnlyApplied = true;
        }
    }

    @Inject(method = "canUse", at = @At("HEAD"))
    private void Onetoys$onCanUse(CallbackInfoReturnable<Boolean> cir) {
        boolean shouldApply = MobAiTweaks.isFixLeapAttackAndHeadSnap();
        if (shouldApply != this.Onetoys$jumpOnlyApplied) {
            this.setFlags(shouldApply ? Onetoys$JUMP_ONLY_FLAGS : Onetoys$VANILLA_FLAGS);
            this.Onetoys$jumpOnlyApplied = shouldApply;
        }
    }
}
