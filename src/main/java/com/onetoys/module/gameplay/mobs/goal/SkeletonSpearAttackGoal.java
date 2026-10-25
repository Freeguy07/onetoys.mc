package com.onetoys.module.gameplay.mobs.goal;

import com.onetoys.module.gameplay.mobs.SpecialMobTweaks;
import com.onetoys.registry.OnetoysItems;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.RangedAttackGoal;
import net.minecraft.world.entity.monster.AbstractSkeleton;
import net.minecraft.world.entity.monster.RangedAttackMob;

public class SkeletonSpearAttackGoal extends RangedAttackGoal {
    private final AbstractSkeleton skeleton;

    public SkeletonSpearAttackGoal(RangedAttackMob mob, double speedModifier, int attackIntervalMin, float attackRadius) {
        super(mob, speedModifier, attackIntervalMin, attackRadius);
        this.skeleton = (AbstractSkeleton) mob;
    }

    @Override
    public boolean canUse() {
        if (!super.canUse() || !this.skeleton.getMainHandItem().is(OnetoysItems.SPEAR.get())) {
            return false;
        }
        if (SpecialMobTweaks.isSmartSkeletonEnabled()
                && SpecialMobTweaks.isSpecialMob(this.skeleton, SpecialMobTweaks.TYPE_SMART_SKELETON)) {
            LivingEntity target = this.skeleton.getTarget();
            if (target != null && this.skeleton.distanceToSqr(target) <= 16.0D) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        if (!super.canContinueToUse() || !this.skeleton.getMainHandItem().is(OnetoysItems.SPEAR.get())) {
            return false;
        }
        if (SpecialMobTweaks.isSmartSkeletonEnabled()
                && SpecialMobTweaks.isSpecialMob(this.skeleton, SpecialMobTweaks.TYPE_SMART_SKELETON)) {
            LivingEntity target = this.skeleton.getTarget();
            if (target != null && this.skeleton.distanceToSqr(target) <= 16.0D) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void start() {
        super.start();
        this.skeleton.setAggressive(true);
        this.skeleton.startUsingItem(InteractionHand.MAIN_HAND);
    }

    @Override
    public void stop() {
        super.stop();
        this.skeleton.stopUsingItem();
        this.skeleton.setAggressive(false);
    }
}
