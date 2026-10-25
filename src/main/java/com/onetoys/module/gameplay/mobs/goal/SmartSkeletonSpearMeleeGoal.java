package com.onetoys.module.gameplay.mobs.goal;

import com.onetoys.module.gameplay.mobs.SpecialMobTweaks;
import com.onetoys.registry.OnetoysItems;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.monster.AbstractSkeleton;

public class SmartSkeletonSpearMeleeGoal extends MeleeAttackGoal {
    private final AbstractSkeleton skeleton;

    public SmartSkeletonSpearMeleeGoal(AbstractSkeleton skeleton, double speedModifier, boolean followingTargetEvenIfNotSeen) {
        super(skeleton, speedModifier, followingTargetEvenIfNotSeen);
        this.skeleton = skeleton;
    }

    @Override
    public boolean canUse() {
        if (!SpecialMobTweaks.isSmartSkeletonEnabled()
                || !SpecialMobTweaks.isSpecialMob(this.skeleton, SpecialMobTweaks.TYPE_SMART_SKELETON)
                || !this.skeleton.getMainHandItem().is(OnetoysItems.SPEAR.get())) {
            return false;
        }
        LivingEntity target = this.skeleton.getTarget();
        if (target == null || !target.isAlive() || this.skeleton.distanceToSqr(target) > 16.0D) {
            return false;
        }
        return super.canUse();
    }

    @Override
    public boolean canContinueToUse() {
        if (!SpecialMobTweaks.isSmartSkeletonEnabled()
                || !SpecialMobTweaks.isSpecialMob(this.skeleton, SpecialMobTweaks.TYPE_SMART_SKELETON)
                || !this.skeleton.getMainHandItem().is(OnetoysItems.SPEAR.get())) {
            return false;
        }
        LivingEntity target = this.skeleton.getTarget();
        if (target == null || !target.isAlive() || this.skeleton.distanceToSqr(target) > 36.0D) {
            return false;
        }
        return super.canContinueToUse();
    }
}
