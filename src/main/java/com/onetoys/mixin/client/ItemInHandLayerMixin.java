package com.onetoys.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.onetoys.module.gameplay.items.SwordBlockingTweaks;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.ArmedModel;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.model.ItemTransform;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import org.joml.Quaternionf;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemInHandLayer.class)
public abstract class ItemInHandLayerMixin<T extends LivingEntity, M extends EntityModel<T> & ArmedModel> extends RenderLayer<T, M> {

    @Shadow
    @Final
    private ItemInHandRenderer itemInHandRenderer;

    public ItemInHandLayerMixin(RenderLayerParent<T, M> renderer) {
        super(renderer);
    }

    @Inject(method = "renderArmWithItem", at = @At("HEAD"), cancellable = true)
    private void Onetoys$renderThirdPersonSwordBlock(
            LivingEntity livingEntity,
            ItemStack itemStack,
            ItemDisplayContext displayContext,
            HumanoidArm arm,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight,
            CallbackInfo ci
    ) {
        if (!itemStack.isEmpty() && livingEntity.isUsingItem() && livingEntity.getUseItemRemainingTicks() > 0) {
            InteractionHand hand = arm == livingEntity.getMainArm() ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
            if (livingEntity.getUsedItemHand() == hand
                    && itemStack.getUseAnimation() == UseAnim.BLOCK
                    && SwordBlockingTweaks.isSword(itemStack)
                    && SwordBlockingTweaks.isTweakEnabled()) {
                poseStack.pushPose();
                this.getParentModel().translateToHand(arm, poseStack);
                boolean leftHand = arm == HumanoidArm.LEFT;
                onetoys$applyItemBlockingTransform(poseStack, leftHand);
                onetoys$applyItemTransformInverse(livingEntity, itemStack, displayContext, poseStack, leftHand);
                this.itemInHandRenderer.renderItem(livingEntity, itemStack, displayContext, leftHand, poseStack, buffer, packedLight);
                poseStack.popPose();
                ci.cancel();
            }
        }
    }

    @Unique
    private static void onetoys$applyItemBlockingTransform(PoseStack poseStack, boolean leftHand) {
        poseStack.translate((leftHand ? 1.0F : -1.0F) / 16.0F, 0.4375F, 0.0625F);
        // 1.7 blocking transform
        poseStack.translate(leftHand ? -0.035F : 0.05F, leftHand ? 0.045F : 0.0F, leftHand ? -0.135F : -0.1F);
        poseStack.mulPose(Axis.YP.rotationDegrees((leftHand ? -1.0F : 1.0F) * -50.0F));
        poseStack.mulPose(Axis.XP.rotationDegrees(-10.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees((leftHand ? -1.0F : 1.0F) * -60.0F));
        // 1.7 item layer transform
        poseStack.translate(0.0F, 0.1875F, 0.0F);
        poseStack.scale(0.625F, 0.625F, 0.625F);
        poseStack.mulPose(Axis.XP.rotationDegrees(180.0F));
        poseStack.mulPose(Axis.XN.rotationDegrees(-100.0F));
        poseStack.mulPose(Axis.YN.rotationDegrees(leftHand ? 35.0F : 45.0F));
        // 1.7 item renderer transform
        poseStack.translate(0.0F, -0.3F, 0.0F);
        poseStack.scale(1.5F, 1.5F, 1.5F);
        poseStack.mulPose(Axis.YN.rotationDegrees(50.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(335.0F));
        poseStack.translate(-0.9375F, -0.0625F, 0.0F);
        poseStack.translate(0.5F, 0.5F, 0.25F);
        poseStack.mulPose(Axis.YN.rotationDegrees(180.0F));
        poseStack.translate(0.0F, 0.0F, 0.28125F);
    }

    @Unique
    private static void onetoys$applyItemTransformInverse(LivingEntity entity, ItemStack stack, ItemDisplayContext displayContext, PoseStack poseStack, boolean leftHand) {
        Minecraft minecraft = Minecraft.getInstance();
        BakedModel model = minecraft.getItemRenderer().getModel(stack, entity.level(), entity, 0);
        ItemTransform vec = model.getTransforms().getTransform(displayContext);
        if (vec != ItemTransform.NO_TRANSFORM) {
            float angleX = vec.rotation.x();
            float angleY = leftHand ? -vec.rotation.y() : vec.rotation.y();
            float angleZ = leftHand ? -vec.rotation.z() : vec.rotation.z();
            Quaternionf quaternion = new Quaternionf().rotationXYZ(
                    angleX * ((float) Math.PI / 180.0F),
                    angleY * ((float) Math.PI / 180.0F),
                    angleZ * ((float) Math.PI / 180.0F)
            );
            quaternion.conjugate();
            poseStack.scale(1.0F / vec.scale.x(), 1.0F / vec.scale.y(), 1.0F / vec.scale.z());
            poseStack.mulPose(quaternion);
            poseStack.translate((leftHand ? -1.0F : 1.0F) * -vec.translation.x(), -vec.translation.y(), -vec.translation.z());
        }
    }
}
