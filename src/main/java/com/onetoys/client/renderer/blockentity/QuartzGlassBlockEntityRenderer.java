package com.onetoys.client.renderer.blockentity;

import org.joml.Matrix4f;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.onetoys.Onetoys;
import com.onetoys.block.QuartzGlassBlock;
import com.onetoys.block.entity.QuartzGlassBlockEntity;
import com.onetoys.client.QuartzGlassClientTracker;
import com.onetoys.client.renderer.QuartzGlassRenderTypes;
import com.onetoys.registry.OnetoysBlocks;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public class QuartzGlassBlockEntityRenderer implements BlockEntityRenderer<QuartzGlassBlockEntity> {
    private static final ResourceLocation SPRITE_LOCATION =
            ResourceLocation.fromNamespaceAndPath(Onetoys.MODID, "block/quartz_glass");

    public QuartzGlassBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(QuartzGlassBlockEntity blockEntity, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        BlockState state = blockEntity.getBlockState();
        Level level = blockEntity.getLevel();
        if (level == null) {
            return;
        }

        // When powered by redstone, the block is rendered directly by the chunk mesh as RenderShape.MODEL
        if (state.hasProperty(QuartzGlassBlock.POWERED) && state.getValue(QuartzGlassBlock.POWERED)) {
            return;
        }

        long placedTime = blockEntity.getPlacedGameTime();
        float placementAlpha = 0.0F;
        if (placedTime > 0L) {
            float age = (float) (level.getGameTime() - placedTime) + partialTick;
            if (age >= 0.0F && age < QuartzGlassBlockEntity.PLACEMENT_FADE_TICKS) {
                if (age < 20.0F) {
                    placementAlpha = 1.0F;
                } else {
                    placementAlpha = 1.0F - (age - 20.0F) / 20.0F;
                }
            }
        }
        float holdAlpha = QuartzGlassClientTracker.getAlpha(partialTick);
        float alpha = Math.max(placementAlpha, holdAlpha);

        if (alpha <= 0.001F) {
            return;
        }

        alpha = Mth.clamp(alpha, 0.0F, 1.0F);

        TextureAtlasSprite sprite = Minecraft.getInstance().getModelManager()
                .getAtlas(InventoryMenu.BLOCK_ATLAS)
                .getSprite(SPRITE_LOCATION);

        BlockPos pos = blockEntity.getBlockPos();
        // Uses QUARTZ_GLASS_TRANSLUCENT (COLOR_WRITE without depth write, targeting TRANSLUCENT_TARGET).
        // This completely prevents translucent water behind or in front of it from being culled.
        VertexConsumer builder = bufferSource.getBuffer(QuartzGlassRenderTypes.QUARTZ_GLASS_TRANSLUCENT);
        PoseStack.Pose lastPose = poseStack.last();
        Matrix4f matrix = lastPose.pose();

        float u0 = sprite.getU0();
        float u1 = sprite.getU1();
        float v0 = sprite.getV0();
        float v1 = sprite.getV1();

        for (Direction dir : Direction.values()) {
            if (shouldRenderFace(level, pos, dir)) {
                renderFace(builder, matrix, dir, alpha, packedLight, u0, u1, v0, v1);
            }
        }
    }

    private boolean shouldRenderFace(Level level, BlockPos pos, Direction dir) {
        BlockPos neighborPos = pos.relative(dir);
        BlockState neighborState = level.getBlockState(neighborPos);

        if (neighborState.is(OnetoysBlocks.QUARTZ_GLASS.get())) {
            return false;
        }

        if (neighborState.isSolidRender(level, neighborPos)) {
            return false;
        }

        return true;
    }

    private void renderFace(VertexConsumer builder, Matrix4f matrix, Direction dir, float alpha, int light, float u0, float u1, float v0, float v1) {
        int a = (int) (alpha * 255.0F);
        switch (dir) {
            case DOWN -> {
                vertex(builder, matrix, 0, 0, 1, u0, v1, a, light, 0, -1, 0);
                vertex(builder, matrix, 0, 0, 0, u0, v0, a, light, 0, -1, 0);
                vertex(builder, matrix, 1, 0, 0, u1, v0, a, light, 0, -1, 0);
                vertex(builder, matrix, 1, 0, 1, u1, v1, a, light, 0, -1, 0);
            }
            case UP -> {
                vertex(builder, matrix, 0, 1, 0, u0, v0, a, light, 0, 1, 0);
                vertex(builder, matrix, 0, 1, 1, u0, v1, a, light, 0, 1, 0);
                vertex(builder, matrix, 1, 1, 1, u1, v1, a, light, 0, 1, 0);
                vertex(builder, matrix, 1, 1, 0, u1, v0, a, light, 0, 1, 0);
            }
            case NORTH -> {
                vertex(builder, matrix, 1, 1, 0, u0, v0, a, light, 0, 0, -1);
                vertex(builder, matrix, 1, 0, 0, u0, v1, a, light, 0, 0, -1);
                vertex(builder, matrix, 0, 0, 0, u1, v1, a, light, 0, 0, -1);
                vertex(builder, matrix, 0, 1, 0, u1, v0, a, light, 0, 0, -1);
            }
            case SOUTH -> {
                vertex(builder, matrix, 0, 1, 1, u0, v0, a, light, 0, 0, 1);
                vertex(builder, matrix, 0, 0, 1, u0, v1, a, light, 0, 0, 1);
                vertex(builder, matrix, 1, 0, 1, u1, v1, a, light, 0, 0, 1);
                vertex(builder, matrix, 1, 1, 1, u1, v0, a, light, 0, 0, 1);
            }
            case WEST -> {
                vertex(builder, matrix, 0, 1, 0, u0, v0, a, light, -1, 0, 0);
                vertex(builder, matrix, 0, 0, 0, u0, v1, a, light, -1, 0, 0);
                vertex(builder, matrix, 0, 0, 1, u1, v1, a, light, -1, 0, 0);
                vertex(builder, matrix, 0, 1, 1, u1, v0, a, light, -1, 0, 0);
            }
            case EAST -> {
                vertex(builder, matrix, 1, 1, 1, u0, v0, a, light, 1, 0, 0);
                vertex(builder, matrix, 1, 0, 1, u0, v1, a, light, 1, 0, 0);
                vertex(builder, matrix, 1, 0, 0, u1, v1, a, light, 1, 0, 0);
                vertex(builder, matrix, 1, 1, 0, u1, v0, a, light, 1, 0, 0);
            }
        }
    }

    private void vertex(VertexConsumer builder, Matrix4f matrix, float x, float y, float z, float u, float v, int alpha, int light, float nx, float ny, float nz) {
        builder.addVertex(matrix, x, y, z)
                .setColor(255, 255, 255, alpha)
                .setUv(u, v)
                .setLight(light)
                .setNormal(nx, ny, nz);
    }
}
