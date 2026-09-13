package com.orehall.mixin;

import com.orehall.module.worldgen.GiantOreVeinsDisabler;
import com.orehall.module.worldgen.WorldgenTweaksModule;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.NoiseChunk;
import net.minecraft.world.level.levelgen.OreVeinifier;
import net.minecraft.world.level.levelgen.PositionalRandomFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(OreVeinifier.class)
public abstract class OreVeinifierMixin {

    @Unique
    private static final NoiseChunk.BlockStateFiller OREHALL$NO_OP_FILLER = context -> null;

    @Inject(method = "create", at = @At("RETURN"), cancellable = true)
    private static void orehall$onOreVeinifierCreate(
            DensityFunction veinToggle,
            DensityFunction veinRidged,
            DensityFunction veinGap,
            PositionalRandomFactory random,
            CallbackInfoReturnable<NoiseChunk.BlockStateFiller> cir
    ) {
        NoiseChunk.BlockStateFiller original = cir.getReturnValue();
        if (original == null) {
            return;
        }

        GiantOreVeinsDisabler disabler = WorldgenTweaksModule.getDisabler();
        if (disabler == null || !disabler.isEnabled()) {
            return;
        }

        boolean disableCopper = disabler.isCopperDisabled();
        boolean disableIron = disabler.isIronDisabled();

        if (disableCopper && disableIron) {
            cir.setReturnValue(OREHALL$NO_OP_FILLER);
        } else if (disableCopper) {
            // Copper only generates in Y: [0, 50]; with copper disabled, veins can only generate in Iron's range Y: [-60, -8]
            cir.setReturnValue(context -> {
                int y = context.blockY();
                return (y < -60 || y > -8) ? null : original.calculate(context);
            });
        } else if (disableIron) {
            // Iron only generates in Y: [-60, -8]; with iron disabled, veins can only generate in Copper's range Y: [0, 50]
            cir.setReturnValue(context -> {
                int y = context.blockY();
                return (y < 0 || y > 50) ? null : original.calculate(context);
            });
        }
    }
}
