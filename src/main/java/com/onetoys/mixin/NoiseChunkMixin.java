package com.onetoys.mixin;

import com.onetoys.module.worldgen.GiantOreVeinsDisabler;
import com.onetoys.module.worldgen.WorldgenTweaksModule;
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
    private boolean Onetoys$skipOreVeinsIfBothDisabled(NoiseGeneratorSettings settings) {
        if (!settings.oreVeinsEnabled()) {
            return false;
        }

        GiantOreVeinsDisabler disabler = WorldgenTweaksModule.getDisabler();
        return disabler == null || !disabler.isBothDisabled();
    }
}
