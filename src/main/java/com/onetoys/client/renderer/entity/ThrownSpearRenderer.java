package com.onetoys.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.onetoys.entity.ThrownSpear;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemDisplayContext;

public class ThrownSpearRenderer extends EntityRenderer<ThrownSpear> {
    private final ItemRenderer itemRenderer;

    public ThrownSpearRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.itemRenderer = context.getItemRenderer();
    }

    @Override
    public void render(ThrownSpear entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(Mth.lerp(partialTick, entity.yRotO, entity.getYRot()) - 90.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(Mth.lerp(partialTick, entity.xRotO, entity.getXRot()) - 90.0F));
        // ItemRenderer.renderStatic automatically applies translate(-0.5F, -0.5F, -0.5F).
        // Since the spear shaft is centered at X=0.5, Z=0.5 in the model, ItemRenderer's -0.5 shifts
        // the shaft to X=0.0, Z=0.0. No X or Z translation is needed here.
        // For Y, model elements span from Y=-14 (handle) to Y=+16 (flint tip), with the flint head from Y=11 to 16.
        // ItemRenderer shifts Y by -0.5 blocks (-8 units).
        // By translating Y by -3/16 (-0.1875D), the pivot/surface position aligns at Y=11 (the base of the flint head).
        // This ensures the flint tip extends ~4-5 pixels forward, embedding naturally into impacted blocks just like
        // a vanilla trident, instead of burying half the spear shaft inside the block.
        poseStack.translate(0.0D, -3.0D / 16.0D, 0.0D);
        this.itemRenderer.renderStatic(
                entity.getItem(),
                ItemDisplayContext.NONE,
                packedLight,
                OverlayTexture.NO_OVERLAY,
                poseStack,
                buffer,
                entity.level(),
                entity.getId()
        );
        poseStack.popPose();
        super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(ThrownSpear entity) {
        return InventoryMenu.BLOCK_ATLAS;
    }
}
