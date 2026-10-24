package com.onetoys.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.onetoys.module.gameplay.mobs.MobAiTweaks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.PathNavigationRegion;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(WalkNodeEvaluator.class)
public abstract class WalkNodeEvaluatorMixin {

    @Unique
    private static final Direction[] Onetoys$CARDINALS = {
            Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST
    };

    @Unique
    private static final int[] Onetoys$JUMP_HEIGHTS = {0, -1, 1};

    @Unique
    private final BlockPos.MutableBlockPos Onetoys$tempPos = new BlockPos.MutableBlockPos();

    @Unique
    private Mob Onetoys$currentMob = null;

    @Unique
    private boolean Onetoys$cachedRailsEnabled = false;

    @Unique
    private boolean Onetoys$cachedGapJumpingEnabled = false;

    @Shadow
    @Final
    private Node[] reusableNeighbors;

    @Shadow
    protected abstract Node findAcceptedNode(int x, int y, int z, int maxStep, double floorLevel, Direction dir, PathType currentType);

    @Shadow
    protected abstract boolean isNeighborValid(Node neighbor, Node current);

    @Shadow
    protected abstract double getFloorLevel(BlockPos pos);

    @Shadow
    protected abstract PathType getCachedPathType(int x, int y, int z);

    @Inject(method = "prepare", at = @At("HEAD"))
    private void Onetoys$cacheConfigOnPrepare(PathNavigationRegion level, Mob mob, CallbackInfo ci) {
        this.Onetoys$currentMob = mob;
        this.Onetoys$cachedRailsEnabled = MobAiTweaks.isAllowWalkingOnRails();
        this.Onetoys$cachedGapJumpingEnabled = MobAiTweaks.isEnableGapJumping();
    }

    @Inject(method = "done", at = @At("HEAD"))
    private void Onetoys$cleanupOnDone(CallbackInfo ci) {
        this.Onetoys$currentMob = null;
    }

    @ModifyExpressionValue(
            method = "getPathTypeWithinMobBB",
            at = @At(
                    value = "FIELD",
                    target = "Lnet/minecraft/world/level/pathfinder/PathType;UNPASSABLE_RAIL:Lnet/minecraft/world/level/pathfinder/PathType;"
            )
    )
    private PathType Onetoys$allowWalkingOnRails(PathType original) {
        if (this.Onetoys$cachedRailsEnabled) {
            return PathType.RAIL;
        }
        return original;
    }

    @Inject(
            method = "getPathTypeFromState(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/pathfinder/PathType;",
            at = @At("RETURN"),
            cancellable = true
    )
    private static void Onetoys$allowWalkingOnSpecialBlocks(BlockGetter level, BlockPos pos, CallbackInfoReturnable<PathType> cir) {
        if (!MobAiTweaks.isAllowWalkingOnSpecialBlocks()) {
            return;
        }

        PathType result = cir.getReturnValue();
        if (result == PathType.LEAVES) {
            cir.setReturnValue(PathType.WALKABLE);
        } else if (result == PathType.TRAPDOOR) {
            BlockState state = level.getBlockState(pos);
            if (state.is(Blocks.LILY_PAD) || state.is(Blocks.BIG_DRIPLEAF)) {
                cir.setReturnValue(PathType.WALKABLE);
            }
        }
    }

    @Inject(method = "getNeighbors", at = @At("RETURN"), cancellable = true)
    private void Onetoys$addGapJumpNeighbors(Node[] neighbors, Node current, CallbackInfoReturnable<Integer> cir) {
        if (!this.Onetoys$cachedGapJumpingEnabled) {
            return;
        }

        int count = cir.getReturnValue();
        if (count >= neighbors.length) {
            return;
        }

        this.Onetoys$tempPos.set(current.x, current.y, current.z);
        double floorLevel = this.getFloorLevel(this.Onetoys$tempPos);
        PathType currentType = this.getCachedPathType(current.x, current.y, current.z);
        int maxStep = this.Onetoys$currentMob != null
                ? (int) Math.floor(Math.max(1.0F, this.Onetoys$currentMob.maxUpStep()))
                : 1;

        for (int d = 0; d < Onetoys$CARDINALS.length; d++) {
            Direction dir = Onetoys$CARDINALS[d];
            Node immediate = this.reusableNeighbors[dir.get2DDataValue()];

            // An empty gap exists if the immediate adjacent block in this direction has no valid walkable ground node
            if (immediate == null || !this.isNeighborValid(immediate, current)) {
                int gapX = current.x + dir.getStepX();
                int gapZ = current.z + dir.getStepZ();

                PathType gapType = this.getCachedPathType(gapX, current.y, gapZ);
                if (gapType != PathType.OPEN && gapType != PathType.WALKABLE) {
                    continue;
                }

                PathType aboveGap = this.getCachedPathType(gapX, current.y + 1, gapZ);
                if (aboveGap != PathType.OPEN && aboveGap != PathType.WALKABLE) {
                    continue;
                }

                int targetX = current.x + (dir.getStepX() << 1);
                int targetZ = current.z + (dir.getStepZ() << 1);

                // Check landing candidate heights: same level (0), step down (-1), or step up (+1)
                for (int h = 0; h < Onetoys$JUMP_HEIGHTS.length; h++) {
                    int dy = Onetoys$JUMP_HEIGHTS[h];
                    Node jumpNode = this.findAcceptedNode(
                            targetX,
                            current.y + dy,
                            targetZ,
                            maxStep,
                            floorLevel,
                            dir,
                            currentType
                    );

                    if (jumpNode != null && !jumpNode.closed && this.isNeighborValid(jumpNode, current)) {
                        boolean alreadyPresent = false;
                        for (int i = 0; i < count; i++) {
                            if (neighbors[i] == jumpNode) {
                                alreadyPresent = true;
                                break;
                            }
                        }

                        if (!alreadyPresent && count < neighbors.length) {
                            jumpNode.costMalus += 1.5F;
                            neighbors[count++] = jumpNode;
                            break;
                        }
                    }
                }
            }
        }

        cir.setReturnValue(count);
    }
}
