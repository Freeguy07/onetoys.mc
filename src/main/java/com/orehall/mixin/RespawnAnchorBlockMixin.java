package com.orehall.mixin;

import com.orehall.module.gameplay.sleep.RespawnAnchorTweaks;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RespawnAnchorBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(RespawnAnchorBlock.class)
public abstract class RespawnAnchorBlockMixin {

    @Inject(method = "canSetSpawn", at = @At("HEAD"), cancellable = true)
    private static void orehall$canSetSpawn(Level level, CallbackInfoReturnable<Boolean> cir) {
        if (RespawnAnchorTweaks.isTweakEnabled()) {
            cir.setReturnValue(!RespawnAnchorTweaks.isDimensionBlacklisted(level));
        }
    }

    @Inject(method = "isRespawnFuel", at = @At("HEAD"), cancellable = true)
    private static void orehall$isRespawnFuel(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        if (RespawnAnchorTweaks.isTweakEnabled()) {
            cir.setReturnValue(RespawnAnchorTweaks.isValidFuel(stack));
        }
    }
}
