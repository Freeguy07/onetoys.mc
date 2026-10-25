package com.onetoys.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.onetoys.module.gameplay.items.SwordBlockingTweaks;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemInHandRenderer.class)
public abstract class ItemInHandRendererMixin {

    @Shadow
    @Final
    private Minecraft minecraft;

    @Shadow
    protected abstract void applyItemArmAttackTransform(PoseStack poseStack, HumanoidArm arm, float swingProgress);

    @Inject(method = "itemUsed", at = @At("HEAD"), cancellable = true)
    private void Onetoys$cancelSwordBlockReequip(InteractionHand interactionHand, CallbackInfo ci) {
        if (this.minecraft.player != null
                && this.minecraft.player.isUsingItem()
                && this.minecraft.player.getUsedItemHand() == interactionHand
                && SwordBlockingTweaks.isBlockingWithSword(this.minecraft.player)) {
            ci.cancel();
        }
    }

    @Inject(
            method = "renderArmWithItem",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/ItemInHandRenderer;renderItem(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemDisplayContext;ZLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
                    ordinal = 1
            )
    )
    private void Onetoys$applyFirstPersonSwordBlockTransform(
            AbstractClientPlayer player,
            float partialTicks,
            float pitch,
            InteractionHand hand,
            float swingProgress,
            ItemStack stack,
            float equippedProgress,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int combinedLight,
            CallbackInfo ci
    ) {
        if (player.isUsingItem()
                && player.getUseItemRemainingTicks() > 0
                && player.getUsedItemHand() == hand
                && stack.getUseAnimation() == UseAnim.BLOCK
                && SwordBlockingTweaks.isSword(stack)) {
            HumanoidArm humanoidArm = hand == InteractionHand.MAIN_HAND ? player.getMainArm() : player.getMainArm().getOpposite();
            int horizontalSign = humanoidArm == HumanoidArm.RIGHT ? 1 : -1;
            this.applyItemArmAttackTransform(poseStack, humanoidArm, swingProgress);
            poseStack.translate(horizontalSign * -0.14142136F, 0.08F, 0.14142136F);
            poseStack.mulPose(Axis.XP.rotationDegrees(-102.25F));
            poseStack.mulPose(Axis.YP.rotationDegrees(horizontalSign * 13.365F));
            poseStack.mulPose(Axis.ZP.rotationDegrees(horizontalSign * 78.05F));
        }
    }
}
