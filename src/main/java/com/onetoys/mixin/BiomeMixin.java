package com.onetoys.mixin;

import com.onetoys.module.gameplay.weather.HeightWeatherTweaks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.biome.Biome;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Biome.class)
public abstract class BiomeMixin {
    @Shadow
    public abstract boolean hasPrecipitation();

    @Inject(method = "getHeightAdjustedTemperature", at = @At("RETURN"), cancellable = true)
    private void onGetHeightAdjustedTemperature(BlockPos pos, CallbackInfoReturnable<Float> cir) {
        if (HeightWeatherTweaks.isFeatureEnabled() && this.hasPrecipitation()) {
            if (HeightWeatherTweaks.shouldSnowAt(pos)) {
                cir.setReturnValue(Math.min(cir.getReturnValueF(), 0.10F));
            }
        }
    }
}
