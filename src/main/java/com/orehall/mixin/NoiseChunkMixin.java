package com.orehall.mixin;

import com.orehall.module.worldgen.GiantOreVeinsDisabler;
import com.orehall.module.worldgen.WorldgenModule;
import net.minecraft.world.level.levelgen.NoiseChunk;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(NoiseChunk.class)
public abstract class NoiseChunkMixin {

    @Redirect(
            method = "<init>",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/levelgen/NoiseGeneratorSettings;oreVeinsEnabled()Z"
            )
    )
    private boolean orehall$skipOreVeinsIfBothDisabled(NoiseGeneratorSettings settings) {
        if (!settings.oreVeinsEnabled()) {
            return false;
        }

        GiantOreVeinsDisabler disabler = WorldgenModule.getDisabler();
        return disabler == null || !disabler.isBothDisabled();
    }
}
