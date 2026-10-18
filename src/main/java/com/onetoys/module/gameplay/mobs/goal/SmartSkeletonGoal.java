package com.onetoys.module.gameplay.mobs.goal;

import com.onetoys.Onetoys;
import com.onetoys.module.gameplay.mobs.SpecialMobTweaks;
import com.onetoys.registry.OnetoysAttachments;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.AbstractSkeleton;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class SmartSkeletonGoal extends Goal {

    public static final ResourceLocation BONE_SWORD_DAMAGE_ID =
            ResourceLocation.fromNamespaceAndPath(Onetoys.MODID, "smart_skeleton_bone_damage");

    // Stone sword deals +4.0 attack damage (total 6.0 on skeleton whose base is 2.0)
    private static final AttributeModifier BONE_SWORD_MODIFIER = new AttributeModifier(
            BONE_SWORD_DAMAGE_ID,
            4.0D,
            AttributeModifier.Operation.ADD_VALUE
    );

    private static final double MELEE_ENGAGE_DIST_SQR = 16.0D;  // 4.0 blocks
    private static final double RANGED_RESUME_DIST_SQR = 49.0D; // 7.0 blocks
    private static final int MIN_SWITCH_COOLDOWN_TICKS = 20;

    private final AbstractSkeleton skeleton;
    private int switchCooldown = 0;

    public SmartSkeletonGoal(AbstractSkeleton skeleton) {
        this.skeleton = skeleton;
    }

    @Override
    public boolean canUse() {
        if (!SpecialMobTweaks.isSmartSkeletonEnabled()) {
            if (isHoldingTemporaryBone()) {
                restoreBow();
            }
            return false;
        }
        return SpecialMobTweaks.isSpecialMob(this.skeleton, SpecialMobTweaks.TYPE_SMART_SKELETON);
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
    public void tick() {
        if (this.switchCooldown > 0) {
            this.switchCooldown--;
        }

        LivingEntity target = this.skeleton.getTarget();
        boolean hasValidTarget = target != null && target.isAlive();

        if (!hasValidTarget) {
            if (isHoldingTemporaryBone()) {
                restoreBow();
            }
            return;
        }

        double distSqr = this.skeleton.distanceToSqr(target);
        if (isHoldingTemporaryBone()) {
            // Ensure residual bow strafing is completely cleared while fighting in melee
            if (this.skeleton.xxa != 0.0F) {
                this.skeleton.setXxa(0.0F);
            }

            // Switch back to bow when target retreats beyond 7.0 blocks
            if (distSqr >= RANGED_RESUME_DIST_SQR && this.switchCooldown <= 0) {
                restoreBow();
                this.switchCooldown = MIN_SWITCH_COOLDOWN_TICKS;
            } else {
                ensureBoneDamageModifier(true);
            }
        } else {
            // Switch from bow to bone when target closes in within 4.0 blocks
            if (distSqr <= MELEE_ENGAGE_DIST_SQR
                    && this.switchCooldown <= 0
                    && this.skeleton.getMainHandItem().getItem() instanceof BowItem) {
                switchToBone(target);
                this.switchCooldown = MIN_SWITCH_COOLDOWN_TICKS;
            }
        }
    }

    @Override
    public void stop() {
        if (isHoldingTemporaryBone()) {
            restoreBow();
        }
    }

    private boolean isHoldingTemporaryBone() {
        ItemStack storedBow = this.skeleton.getData(OnetoysAttachments.SMART_SKELETON_STORED_BOW);
        return !storedBow.isEmpty() && this.skeleton.getMainHandItem().is(Items.BONE);
    }

    private void switchToBone(LivingEntity target) {
        ItemStack currentMainHand = this.skeleton.getMainHandItem();
        if (currentMainHand.getItem() instanceof BowItem) {
            // Stop bow drawing and clear residual RangedBowAttackGoal strafing state
            this.skeleton.stopUsingItem();
            this.skeleton.getMoveControl().strafe(0.0F, 0.0F);
            this.skeleton.setXxa(0.0F);
            this.skeleton.setZza(0.0F);

            this.skeleton.setData(OnetoysAttachments.SMART_SKELETON_STORED_BOW, currentMainHand.copy());
            ensureBoneDamageModifier(true);
            this.skeleton.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.BONE));

            // Immediately start pathing toward the melee target
            if (target != null) {
                this.skeleton.getNavigation().moveTo(target, 1.2D);
            }
        }
    }

    public void restoreBow() {
        ItemStack storedBow = this.skeleton.getData(OnetoysAttachments.SMART_SKELETON_STORED_BOW);
        ensureBoneDamageModifier(false);
        if (!storedBow.isEmpty()) {
            this.skeleton.stopUsingItem();
            this.skeleton.getNavigation().stop();
            this.skeleton.setXxa(0.0F);
            this.skeleton.setZza(0.0F);

            this.skeleton.setData(OnetoysAttachments.SMART_SKELETON_STORED_BOW, ItemStack.EMPTY);
            this.skeleton.setItemSlot(EquipmentSlot.MAINHAND, storedBow.copy());
        }
    }

    private void ensureBoneDamageModifier(boolean active) {
        AttributeInstance attackDamage = this.skeleton.getAttribute(Attributes.ATTACK_DAMAGE);
        if (attackDamage == null) {
            return;
        }
        if (active) {
            if (!attackDamage.hasModifier(BONE_SWORD_DAMAGE_ID)) {
                attackDamage.addTransientModifier(BONE_SWORD_MODIFIER);
            }
        } else {
            if (attackDamage.hasModifier(BONE_SWORD_DAMAGE_ID)) {
                attackDamage.removeModifier(BONE_SWORD_DAMAGE_ID);
            }
        }
    }
}
