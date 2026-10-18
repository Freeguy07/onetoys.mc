package com.onetoys.mixin;

import com.onetoys.module.gameplay.mobs.MobAiTweaks;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.Path;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PathNavigation.class)
public abstract class PathNavigationMixin {

    @Shadow
    @Final
    protected Mob mob;

    @Shadow
    @Nullable
    protected Path path;

    @Shadow
    protected float maxDistanceToWaypoint;

    @Unique
    private int Onetoys$sameColumnTicks = 0;

    @Unique
    private long Onetoys$lastColumnPos = Long.MIN_VALUE;

    @Inject(method = "shouldRecomputePath(Lnet/minecraft/core/BlockPos;)Z", at = @At("HEAD"), cancellable = true)
    private void Onetoys$limitBlockUpdatePathRecompute(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        if (MobAiTweaks.isLimitBlockUpdatePathRecompute() && this.mob.blockPosition().distSqr(pos) > 64.0D) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "followThePath", at = @At("TAIL"))
    private void Onetoys$fixSpinningAndStuckNavigation(CallbackInfo ci) {
        if (!MobAiTweaks.isFixSpinningAndStuckNavigation() || this.path == null || this.path.isDone()) {
            this.Onetoys$sameColumnTicks = 0;
            return;
        }

        Node nextNode = this.path.getNextNode();
        if (this.mob.getBlockX() == nextNode.x
                && this.mob.getBlockZ() == nextNode.z
                && Math.abs(this.mob.getY() - (double) nextNode.y) < 1.25D) {
            long currentColumnPos = BlockPos.asLong(nextNode.x, nextNode.y, nextNode.z);
            if (this.Onetoys$lastColumnPos != currentColumnPos) {
                this.Onetoys$lastColumnPos = currentColumnPos;
                this.Onetoys$sameColumnTicks = 1;
            } else {
                this.Onetoys$sameColumnTicks++;
            }

            if (this.Onetoys$sameColumnTicks > 8) {
                this.path.advance();
                this.Onetoys$sameColumnTicks = 0;
            }
        } else {
            this.Onetoys$sameColumnTicks = 0;
        }
    }
}
