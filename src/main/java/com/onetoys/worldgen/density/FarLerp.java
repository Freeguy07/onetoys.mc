package com.onetoys.worldgen.density;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.onetoys.module.worldgen.WorldgenTweaksModule;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.util.Mth;
import net.minecraft.world.level.levelgen.DensityFunction;

/** lerp(alpha, near, far). Evaluates only one branch when alpha is exactly 0 or 1. */
public record FarLerp(DensityFunction alpha, DensityFunction near, DensityFunction far)
        implements DensityFunction.SimpleFunction {
    public static final MapCodec<FarLerp> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            DensityFunction.HOLDER_HELPER_CODEC.fieldOf("alpha").forGetter(FarLerp::alpha),
            DensityFunction.HOLDER_HELPER_CODEC.fieldOf("near").forGetter(FarLerp::near),
            DensityFunction.HOLDER_HELPER_CODEC.fieldOf("far").forGetter(FarLerp::far)
    ).apply(i, FarLerp::new));
    private static final KeyDispatchDataCodec<FarLerp> CODEC = KeyDispatchDataCodec.of(MAP_CODEC);

    @Override
    public double compute(FunctionContext ctx) {
        if (!WorldgenTweaksModule.isTerrainTweaksEnabled()) {
            return this.near.compute(ctx);
        }
        double a = alpha.compute(ctx);
        if (a <= 0.0) return near.compute(ctx);
        if (a >= 1.0) return far.compute(ctx);
        return Mth.lerp(a, near.compute(ctx), far.compute(ctx));
    }

    // CRITICAL: must recurse into children, otherwise NoiseChunk never wires cache/noise markers inside them.
    @Override
    public DensityFunction mapAll(Visitor visitor) {
        return visitor.apply(new FarLerp(alpha.mapAll(visitor), near.mapAll(visitor), far.mapAll(visitor)));
    }

    @Override public double minValue() { return Math.min(near.minValue(), far.minValue()); }
    @Override public double maxValue() { return Math.max(near.maxValue(), far.maxValue()); }
    @Override public KeyDispatchDataCodec<? extends DensityFunction> codec() { return CODEC; }
}
