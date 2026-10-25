package com.onetoys.client;

import com.onetoys.block.QuartzGlassBlock;
import com.onetoys.block.entity.QuartzGlassBlockEntity;
import com.onetoys.registry.OnetoysBlocks;
import com.onetoys.registry.OnetoysItems;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderHighlightEvent;

public class QuartzGlassClientTracker {
    private static final float TRANSITION_SPEED = 0.08F;
    private static float currentAlpha = 0.0F;
    private static float prevAlpha = 0.0F;

    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.isPaused()) {
            return;
        }

        LocalPlayer player = mc.player;
        boolean holding = player != null && (
                player.getMainHandItem().is(OnetoysItems.QUARTZ_GLASS.get()) ||
                player.getOffhandItem().is(OnetoysItems.QUARTZ_GLASS.get())
        );

        prevAlpha = currentAlpha;
        float target = holding ? 1.0F : 0.0F;

        if (currentAlpha < target) {
            currentAlpha = Math.min(target, currentAlpha + TRANSITION_SPEED);
        } else if (currentAlpha > target) {
            currentAlpha = Math.max(target, currentAlpha - TRANSITION_SPEED);
        }
    }

    public static void onRenderHighlight(RenderHighlightEvent.Block event) {
        BlockHitResult target = event.getTarget();
        BlockPos pos = target.getBlockPos();
        Minecraft mc = Minecraft.getInstance();
        Level level = mc.level;
        if (level == null) {
            return;
        }

        BlockState state = level.getBlockState(pos);
        if (!state.is(OnetoysBlocks.QUARTZ_GLASS.get())) {
            return;
        }

        // If powered by redstone, it is visible to everyone -> show outline
        if (state.hasProperty(QuartzGlassBlock.POWERED) && state.getValue(QuartzGlassBlock.POWERED)) {
            return;
        }

        // If freshly placed, it is visible during placement reveal -> show outline
        if (level.getBlockEntity(pos) instanceof QuartzGlassBlockEntity be && be.isPlacementRevealed()) {
            return;
        }

        // If held in hand, it is visible to the player -> show outline
        if (currentAlpha > 0.01F) {
            return;
        }

        // Otherwise, it is completely invisible -> suppress the bounding box highlight!
        event.setCanceled(true);
    }

    public static float getAlpha(float partialTick) {
        return Mth.clamp(Mth.lerp(partialTick, prevAlpha, currentAlpha), 0.0F, 1.0F);
    }
}
