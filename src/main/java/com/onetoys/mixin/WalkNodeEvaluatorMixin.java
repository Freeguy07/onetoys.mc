package com.onetoys.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.onetoys.module.gameplay.mobs.MobAiTweaks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(WalkNodeEvaluator.class)
public abstract class WalkNodeEvaluatorMixin {

    @ModifyExpressionValue(
            method = "getPathTypeWithinMobBB",
            at = @At(
                    value = "FIELD",
                    target = "Lnet/minecraft/world/level/pathfinder/PathType;UNPASSABLE_RAIL:Lnet/minecraft/world/level/pathfinder/PathType;"
            )
    )
    private PathType Onetoys$allowWalkingOnRails(PathType original) {
        if (MobAiTweaks.isAllowWalkingOnRailsAndSpecialBlocks()) {
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
        if (!MobAiTweaks.isAllowWalkingOnRailsAndSpecialBlocks()) {
            return;
        }

        PathType result = cir.getReturnValue();
        if (result == PathType.LEAVES) {
            BlockState state = level.getBlockState(pos);
            if (state.getBlock() instanceof LeavesBlock) {
                cir.setReturnValue(PathType.WALKABLE);
            }
        } else if (result == PathType.TRAPDOOR) {
            BlockState state = level.getBlockState(pos);
            if (state.is(Blocks.LILY_PAD) || state.is(Blocks.BIG_DRIPLEAF)) {
                cir.setReturnValue(PathType.WALKABLE);
            }
        }
    }
}
