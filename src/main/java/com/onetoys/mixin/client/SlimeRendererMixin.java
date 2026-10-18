package com.onetoys.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.onetoys.module.gameplay.mobs.goal.SlipperySlimeDashGoal;
import com.onetoys.registry.OnetoysAttachments;
import net.minecraft.client.renderer.entity.SlimeRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.monster.Slime;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SlimeRenderer.class)
public abstract class SlimeRendererMixin {

    @Inject(
            method = "scale(Lnet/minecraft/world/entity/monster/Slime;Lcom/mojang/blaze3d/vertex/PoseStack;F)V",
            at = @At("TAIL")
    )
    private void Onetoys$scaleSwellingSlipperySlime(Slime slime, PoseStack poseStack, float partialTickTime, CallbackInfo ci) {
        int swellTicks = slime.getData(OnetoysAttachments.SLIME_SWELL_TICKS);
        if (swellTicks > 0) {
            float progress = Mth.clamp(((float) swellTicks + partialTickTime) / (float) SlipperySlimeDashGoal.MAX_SWELL_TICKS, 0.0F, 1.0F);
            float swellScale = 1.0F + 0.30F * Mth.sin(progress * Mth.HALF_PI);
            float wobble = Mth.sin(((float) slime.tickCount + partialTickTime) * 1.4F) * 0.04F * progress;
            poseStack.scale(swellScale + wobble, swellScale - wobble * 0.5F, swellScale + wobble);
        }
    }
}
