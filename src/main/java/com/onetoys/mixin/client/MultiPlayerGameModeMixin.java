package com.onetoys.mixin.client;

import com.onetoys.module.gameplay.mining.MiningTweaks;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.GameType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MultiPlayerGameMode.class)
public abstract class MultiPlayerGameModeMixin {

    @Shadow
    private int destroyDelay;

    @Shadow
    private GameType localPlayerMode;

    @Inject(method = "continueDestroyBlock", at = @At("HEAD"))
    private void Onetoys$bypassDestroyDelayAtHead(BlockPos loc, Direction face, CallbackInfoReturnable<Boolean> cir) {
        if (MiningTweaks.shouldDisableMiningDelay(this.localPlayerMode)) {
            this.destroyDelay = 0;
        }
    }

    @Inject(method = "continueDestroyBlock", at = @At("RETURN"))
    private void Onetoys$resetDestroyDelayAtReturn(BlockPos loc, Direction face, CallbackInfoReturnable<Boolean> cir) {
        if (MiningTweaks.shouldDisableMiningDelay(this.localPlayerMode)) {
            this.destroyDelay = 0;
        }
    }

    @Inject(method = "startDestroyBlock", at = @At("TAIL"))
    private void Onetoys$resetDestroyDelayOnStart(BlockPos loc, Direction face, CallbackInfoReturnable<Boolean> cir) {
        if (MiningTweaks.shouldDisableMiningDelay(this.localPlayerMode)) {
            this.destroyDelay = 0;
        }
    }
}
