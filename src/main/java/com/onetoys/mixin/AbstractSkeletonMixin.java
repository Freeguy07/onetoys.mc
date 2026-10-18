package com.onetoys.mixin;

import com.onetoys.module.gameplay.mobs.SpecialMobTweaks;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RangedBowAttackGoal;
import net.minecraft.world.entity.monster.AbstractSkeleton;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractSkeleton.class)
public abstract class AbstractSkeletonMixin {

    @Shadow
    @Final
    private RangedBowAttackGoal<AbstractSkeleton> bowGoal;

    @Shadow
    @Final
    private MeleeAttackGoal meleeGoal;

    @Inject(method = "reassessWeaponGoal", at = @At("HEAD"), cancellable = true)
    private void Onetoys$preventWarlockMeleeGoal(CallbackInfo ci) {
        AbstractSkeleton skeleton = (AbstractSkeleton) (Object) this;
        if (skeleton.level() != null && !skeleton.level().isClientSide) {
            if (SpecialMobTweaks.isWarlockWitherSkeletonEnabled()
                    && SpecialMobTweaks.isSpecialMob(skeleton, SpecialMobTweaks.TYPE_WARLOCK_WITHER_SKELETON)) {
                skeleton.goalSelector.removeGoal(this.meleeGoal);
                skeleton.goalSelector.removeGoal(this.bowGoal);
                ci.cancel();
            }
        }
    }
}
