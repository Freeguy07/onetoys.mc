package com.onetoys.mixin.client;

import com.onetoys.module.gameplay.items.SwordBlockingTweaks;
import net.minecraft.client.model.AgeableListModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
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
    public ModelPart head;

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
            boolean isRightArm = (entity.getMainArm() == HumanoidArm.RIGHT)
                    ? (entity.getUsedItemHand() == InteractionHand.MAIN_HAND)
                    : (entity.getUsedItemHand() == InteractionHand.OFF_HAND);
            ModelPart arm = isRightArm ? this.rightArm : this.leftArm;
            float walkSwing = Mth.cos(limbSwing * 0.6662F + (isRightArm ? (float) Math.PI : 0.0F)) * limbSwingAmount * 0.5F;
            float headPitchOffset = Mth.clamp(this.head.xRot, -0.4F, 0.4F) * 0.25F;
            arm.xRot = walkSwing * 0.3F - 0.58F + headPitchOffset;
            arm.yRot = isRightArm ? -((float) Math.PI / 6.0F) : ((float) Math.PI / 6.0F);
            arm.zRot = 0.0F;
        }
    }
}
