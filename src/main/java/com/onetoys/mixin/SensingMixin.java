package com.onetoys.mixin;

import com.onetoys.module.gameplay.mobs.MobAiTweaks;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.sensing.Sensing;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Sensing.class)
public abstract class SensingMixin {

    @Shadow
    @Final
    private Mob mob;

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void Onetoys$throttleSensingCacheClear(CallbackInfo ci) {
        int interval = MobAiTweaks.getSensingCacheInterval();
        if (interval > 1 && ((this.mob.tickCount + this.mob.getId()) % interval != 0)) {
            ci.cancel();
        }
    }
}
