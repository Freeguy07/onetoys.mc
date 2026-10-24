package com.onetoys.mixin;

import com.onetoys.module.gameplay.mobs.MobAiTweaks;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;
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

    @Unique
    private final BlockPos.MutableBlockPos Onetoys$tempNavPos = new BlockPos.MutableBlockPos();

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
    private void Onetoys$followThePathTweaks(CallbackInfo ci) {
        if (this.path == null || this.path.isDone()) {
            this.Onetoys$sameColumnTicks = 0;
            return;
        }

        Node nextNode = this.path.getNextNode();

        // Gap jumping execution: trigger jump when approaching a chasm towards the next node
        if (MobAiTweaks.isEnableGapJumping() && this.mob.onGround()) {
            int mobBlockX = this.mob.getBlockX();
            int mobBlockZ = this.mob.getBlockZ();
            int diffX = nextNode.x - mobBlockX;
            int diffZ = nextNode.z - mobBlockZ;
            int absX = Math.abs(diffX);
            int absZ = Math.abs(diffZ);

            if ((absX == 2 && absZ == 0) || (absX == 0 && absZ == 2)) {
                int stepX = Integer.signum(diffX);
                int stepZ = Integer.signum(diffZ);

                Vec3 delta = this.mob.getDeltaMovement();
                // Ensure the mob has forward velocity toward the gap before initiating leap
                if ((stepX != 0 && delta.x * stepX > 0.01D) || (stepZ != 0 && delta.z * stepZ > 0.01D)) {
                    this.Onetoys$tempNavPos.set(mobBlockX + stepX, this.mob.getBlockY() - 1, mobBlockZ + stepZ);
                    BlockState gapState = this.mob.level().getBlockState(this.Onetoys$tempNavPos);

                    if (gapState.isAir() || gapState.getCollisionShape(this.mob.level(), this.Onetoys$tempNavPos).isEmpty()) {
                        double offX = stepX != 0 ? (stepX > 0 ? (mobBlockX + 1.0D - this.mob.getX()) : (this.mob.getX() - mobBlockX)) : 0.0D;
                        double offZ = stepZ != 0 ? (stepZ > 0 ? (mobBlockZ + 1.0D - this.mob.getZ()) : (this.mob.getZ() - mobBlockZ)) : 0.0D;
                        double distToEdge = Math.max(offX, offZ);

                        if (distToEdge <= 0.65D) {
                            this.mob.getJumpControl().jump();
                        }
                    }
                }
            }
        }

        // Anti-stuck rotation: advance node if stuck in the exact same column out of combat
        if (MobAiTweaks.isFixSpinningAndStuckNavigation()) {
            if (this.mob.getTarget() == null) {
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
            } else {
                this.Onetoys$sameColumnTicks = 0;
            }
        } else {
            this.Onetoys$sameColumnTicks = 0;
        }
    }
}
