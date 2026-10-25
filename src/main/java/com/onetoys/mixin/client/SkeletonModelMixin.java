package com.onetoys.mixin.client;

import com.onetoys.module.gameplay.GameplayTweaksModule;
import com.onetoys.registry.OnetoysAttachments;
import com.onetoys.registry.OnetoysItems;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.SkeletonModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SkeletonModel.class)
public abstract class SkeletonModelMixin<T extends Mob & RangedAttackMob> extends HumanoidModel<T> {

    protected SkeletonModelMixin(ModelPart root) {
        super(root);
    }

    @Inject(
            method = "prepareMobModel(Lnet/minecraft/world/entity/Mob;FFF)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/model/HumanoidModel;prepareMobModel(Lnet/minecraft/world/entity/LivingEntity;FFF)V"
            )
    )
    private void Onetoys$prepareCustomWeaponArmPoses(T entity, float limbSwing, float limbSwingAmount, float partialTick, CallbackInfo ci) {
        ItemStack mainHand = entity.getItemInHand(InteractionHand.MAIN_HAND);
        ItemStack offHand = entity.getItemInHand(InteractionHand.OFF_HAND);

        if (GameplayTweaksModule.isSpearEnabled() && (mainHand.is(OnetoysItems.SPEAR.get()) || offHand.is(OnetoysItems.SPEAR.get()))) {
            if (entity.isUsingItem()) {
                boolean mainSpear = mainHand.is(OnetoysItems.SPEAR.get());
                boolean rightHanded = entity.getMainArm() == HumanoidArm.RIGHT;
                boolean useRightArm = mainSpear ? rightHanded : !rightHanded;
                if (useRightArm) {
                    this.rightArmPose = HumanoidModel.ArmPose.THROW_SPEAR;
                } else {
                    this.leftArmPose = HumanoidModel.ArmPose.THROW_SPEAR;
                }
            }
            return;
        }

        boolean mainCrossbow = mainHand.getItem() instanceof CrossbowItem;
        boolean offCrossbow = offHand.getItem() instanceof CrossbowItem;
        if (!mainCrossbow && !offCrossbow) {
            return;
        }

        ItemStack crossbowStack = mainCrossbow ? mainHand : offHand;
        boolean isCharging = entity.getData(OnetoysAttachments.SKELETON_CHARGING_CROSSBOW) && entity.isUsingItem();
        HumanoidModel.ArmPose pose = null;
        if (isCharging) {
            pose = HumanoidModel.ArmPose.CROSSBOW_CHARGE;
        } else if (entity.isAggressive() || CrossbowItem.isCharged(crossbowStack)) {
            pose = HumanoidModel.ArmPose.CROSSBOW_HOLD;
        }

        if (pose != null) {
            boolean rightHanded = entity.getMainArm() == HumanoidArm.RIGHT;
            boolean useRightArm = mainCrossbow ? rightHanded : !rightHanded;
            if (useRightArm) {
                this.rightArmPose = pose;
            } else {
                this.leftArmPose = pose;
            }
        }
    }

    @Inject(
            method = "setupAnim(Lnet/minecraft/world/entity/Mob;FFFFF)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/model/HumanoidModel;setupAnim(Lnet/minecraft/world/entity/LivingEntity;FFFFF)V",
                    shift = At.Shift.AFTER
            ),
            cancellable = true
    )
    private void Onetoys$preventZombieMeleePoseWithCustomWeapons(
            T entity,
            float limbSwing,
            float limbSwingAmount,
            float ageInTicks,
            float netHeadYaw,
            float headPitch,
            CallbackInfo ci
    ) {
        if (onetoys$isHoldingCrossbow(entity) || (entity.isUsingItem() && onetoys$isHoldingSpear(entity))) {
            ci.cancel();
        }
    }

    @Unique
    private static boolean onetoys$isHoldingCrossbow(Mob entity) {
        return entity.getMainHandItem().getItem() instanceof CrossbowItem
                || entity.getOffhandItem().getItem() instanceof CrossbowItem;
    }

    @Unique
    private static boolean onetoys$isHoldingSpear(Mob entity) {
        if (!GameplayTweaksModule.isSpearEnabled()) {
            return false;
        }
        return entity.getMainHandItem().is(OnetoysItems.SPEAR.get())
                || entity.getOffhandItem().is(OnetoysItems.SPEAR.get());
    }
}
