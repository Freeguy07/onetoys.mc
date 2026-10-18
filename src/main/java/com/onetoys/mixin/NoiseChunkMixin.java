package com.onetoys.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.onetoys.module.worldgen.GiantOreVeinsDisabler;
import com.onetoys.module.worldgen.WorldgenTweaksModule;
import net.minecraft.world.level.levelgen.NoiseChunk;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(NoiseChunk.class)
public abstract class NoiseChunkMixin {

    @WrapOperation(
            method = "<init>",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/levelgen/NoiseGeneratorSettings;oreVeinsEnabled()Z"
            )
    )
    private boolean Onetoys$skipOreVeinsIfBothDisabled(NoiseGeneratorSettings settings, Operation<Boolean> original) {
        if (!original.call(settings)) {
            return false;
        }

        GiantOreVeinsDisabler disabler = WorldgenTweaksModule.getDisabler();
        return disabler == null || !disabler.isBothDisabled();
    }
}
