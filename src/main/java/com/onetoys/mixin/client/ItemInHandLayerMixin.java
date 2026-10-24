package com.onetoys.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.onetoys.module.gameplay.items.SwordBlockingTweaks;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemInHandLayer.class)
public abstract class ItemInHandLayerMixin {

    @Inject(
            method = "renderArmWithItem",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/ItemInHandRenderer;renderItem(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemDisplayContext;ZLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V"
            )
    )
    private void Onetoys$applyThirdPersonSwordBlockTransform(
            LivingEntity livingEntity,
            ItemStack itemStack,
            ItemDisplayContext displayContext,
            HumanoidArm arm,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight,
            CallbackInfo ci
    ) {
        if (livingEntity.isUsingItem() && livingEntity.getUseItemRemainingTicks() > 0) {
            InteractionHand hand = arm == livingEntity.getMainArm() ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
            if (livingEntity.getUsedItemHand() == hand
                    && itemStack.getUseAnimation() == UseAnim.BLOCK
                    && SwordBlockingTweaks.isSword(itemStack)) {
                float sign = arm == HumanoidArm.LEFT ? -1.0F : 1.0F;
                poseStack.translate(sign * -0.15F, 0.1F, -0.1F);
                poseStack.mulPose(Axis.XP.rotationDegrees(-20.0F));
                poseStack.mulPose(Axis.YP.rotationDegrees(sign * 40.0F));
                poseStack.mulPose(Axis.ZP.rotationDegrees(sign * -35.0F));
            }
        }
    }
}
