package com.onetoys.mixin;

import com.onetoys.module.gameplay.wetness.WetnessTweaks;
import net.minecraft.world.entity.monster.Spider;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Spider.class)
public abstract class SpiderMixin {

    @Shadow
    public abstract void setClimbing(boolean climbing);

    @Inject(method = "onClimbable", at = @At("HEAD"), cancellable = true)
    private void Onetoys$wetSpiderCannotClimb(CallbackInfoReturnable<Boolean> cir) {
        if (WetnessTweaks.isWetSpidersCannotClimb()) {
            Spider spider = (Spider) (Object) this;
            if (WetnessTweaks.isWet(spider)) {
                cir.setReturnValue(false);
            }
        }
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void Onetoys$clearClimbingState(CallbackInfo ci) {
        if (WetnessTweaks.isWetSpidersCannotClimb()) {
            Spider spider = (Spider) (Object) this;
            if (spider.isClimbing() && WetnessTweaks.isWet(spider)) {
                this.setClimbing(false);
            }
        }
    }
}
