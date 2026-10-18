package com.onetoys.worldgen.density;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.util.Mth;
import net.minecraft.world.level.levelgen.DensityFunction;

/** 0..1 smoothstep of horizontal distance from (0,0): 0 inside `start`, 1 beyond `end`. */
public record DistanceBlend(double start, double end) implements DensityFunction.SimpleFunction {
    public static final MapCodec<DistanceBlend> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.doubleRange(0.0, 3.0E7).fieldOf("start").forGetter(DistanceBlend::start),
            Codec.doubleRange(1.0, 3.0E7).fieldOf("end").forGetter(DistanceBlend::end)
    ).apply(i, DistanceBlend::new));
    private static final KeyDispatchDataCodec<DistanceBlend> CODEC = KeyDispatchDataCodec.of(MAP_CODEC);

    @Override
    public double compute(FunctionContext ctx) {
        double x = ctx.blockX(), z = ctx.blockZ();
        double t = Mth.clamp((Math.sqrt(x * x + z * z) - start) / (end - start), 0.0, 1.0);
        return t * t * (3.0 - 2.0 * t);
    }

    @Override public double minValue() { return 0.0; }
    @Override public double maxValue() { return 1.0; }
    @Override public KeyDispatchDataCodec<? extends DensityFunction> codec() { return CODEC; }
}
