package com.onetoys.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.onetoys.module.worldgen.WorldgenTweaksModule;
import com.onetoys.worldgen.surface.OnetoysSurfaceRules;
import net.minecraft.core.Holder;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.SurfaceRules;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(NoiseBasedChunkGenerator.class)
public abstract class NoiseBasedChunkGeneratorMixin {

    @Shadow
    @Final
    private Holder<NoiseGeneratorSettings> settings;

    @WrapOperation(
            method = "buildSurface(Lnet/minecraft/world/level/chunk/ChunkAccess;Lnet/minecraft/world/level/levelgen/WorldGenerationContext;Lnet/minecraft/world/level/levelgen/RandomState;Lnet/minecraft/world/level/StructureManager;Lnet/minecraft/world/level/biome/BiomeManager;Lnet/minecraft/core/Registry;Lnet/minecraft/world/level/levelgen/blending/Blender;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/levelgen/NoiseGeneratorSettings;surfaceRule()Lnet/minecraft/world/level/levelgen/SurfaceRules$RuleSource;"
            )
    )
    private SurfaceRules.RuleSource onetoys$wrapBuildSurfaceRule(NoiseGeneratorSettings settings, Operation<SurfaceRules.RuleSource> original) {
        SurfaceRules.RuleSource rule = original.call(settings);
        if (WorldgenTweaksModule.isTerrainTweaksEnabled() && this.settings.is(NoiseGeneratorSettings.OVERWORLD)) {
            return SurfaceRules.sequence(OnetoysSurfaceRules.skin(), rule);
        }
        return rule;
    }

    @WrapOperation(
            method = "applyCarvers",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/levelgen/NoiseGeneratorSettings;surfaceRule()Lnet/minecraft/world/level/levelgen/SurfaceRules$RuleSource;"
            )
    )
    private SurfaceRules.RuleSource onetoys$wrapCarverSurfaceRule(NoiseGeneratorSettings settings, Operation<SurfaceRules.RuleSource> original) {
        SurfaceRules.RuleSource rule = original.call(settings);
        if (WorldgenTweaksModule.isTerrainTweaksEnabled() && this.settings.is(NoiseGeneratorSettings.OVERWORLD)) {
            return SurfaceRules.sequence(OnetoysSurfaceRules.skin(), rule);
        }
        return rule;
    }
}
