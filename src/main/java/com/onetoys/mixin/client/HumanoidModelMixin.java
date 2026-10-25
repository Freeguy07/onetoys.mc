package com.onetoys.mixin.client;

import com.onetoys.module.gameplay.items.SwordBlockingTweaks;
import net.minecraft.client.model.AgeableListModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HumanoidModel.class)
public abstract class HumanoidModelMixin<T extends LivingEntity> extends AgeableListModel<T> {

    @Shadow
    public ModelPart rightArm;

    @Shadow
    public ModelPart leftArm;

    @Inject(
            method = "setupAnim(Lnet/minecraft/world/entity/LivingEntity;FFFFF)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/model/HumanoidModel;setupAttackAnimation(Lnet/minecraft/world/entity/LivingEntity;F)V"
            )
    )
    private void Onetoys$adjustSwordBlockingArmPose(
            T entity,
            float limbSwing,
            float limbSwingAmount,
            float ageInTicks,
            float netHeadYaw,
            float headPitch,
            CallbackInfo ci
    ) {
        if (SwordBlockingTweaks.isBlockingWithSword(entity)) {
            InteractionHand rightHand = entity.getMainArm() == HumanoidArm.RIGHT
                    ? InteractionHand.MAIN_HAND
                    : InteractionHand.OFF_HAND;
            if (entity.getUsedItemHand() == rightHand) {
                this.rightArm.xRot = this.rightArm.xRot - ((float) Math.PI * 2.0F) / 10.0F;
                this.rightArm.yRot = -((float) Math.PI / 6.0F);
            } else {
                this.leftArm.xRot = this.leftArm.xRot - ((float) Math.PI * 2.0F) / 10.0F;
                this.leftArm.yRot = (float) Math.PI / 6.0F;
            }
        }
    }
}
