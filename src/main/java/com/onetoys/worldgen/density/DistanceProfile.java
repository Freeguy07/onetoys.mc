package com.onetoys.worldgen.density;

import java.util.List;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.onetoys.module.worldgen.WorldgenTweaksModule;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.util.Mth;
import net.minecraft.world.level.levelgen.DensityFunction;

/**
 * Value in 0..1 as a function of horizontal distance from (0,0), optionally repeating every `period` blocks.
 * If period <= 0.0, the value is clamped to the final point's value once beyond it.
 * Between consecutive keyframes the value is eased with smoothstep (flat at every keyframe).
 */
public record DistanceProfile(double period, List<Point> points) implements DensityFunction.SimpleFunction {

    public record Point(double d, double v) {
        public static final Codec<Point> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.doubleRange(0.0, 3.0E7).fieldOf("d").forGetter(Point::d),
                Codec.doubleRange(0.0, 1.0).fieldOf("v").forGetter(Point::v)
        ).apply(i, Point::new));
    }

    public static final MapCodec<DistanceProfile> MAP_CODEC = RecordCodecBuilder.<DistanceProfile>mapCodec(i -> i.group(
            Codec.doubleRange(0.0, 3.0E7).optionalFieldOf("period", 0.0).forGetter(DistanceProfile::period),
            Point.CODEC.listOf().fieldOf("points").forGetter(DistanceProfile::points)
    ).apply(i, DistanceProfile::new)).flatXmap(DistanceProfile::validate, DataResult::success);

    private static final KeyDispatchDataCodec<DistanceProfile> CODEC = KeyDispatchDataCodec.of(MAP_CODEC);

    private static DataResult<DistanceProfile> validate(DistanceProfile p) {
        List<Point> pts = p.points();
        if (pts.size() < 2) return DataResult.error(() -> "distance_profile needs at least 2 points");
        if (pts.get(0).d() != 0.0) return DataResult.error(() -> "distance_profile: first point must have d = 0");
        for (int i = 1; i < pts.size(); i++) {
            if (pts.get(i).d() <= pts.get(i - 1).d()) return DataResult.error(() -> "distance_profile: points must be strictly increasing in d");
        }
        if (p.period() > 0.0) {
            if (pts.get(pts.size() - 1).d() != p.period()) return DataResult.error(() -> "distance_profile: last point must have d = period");
            if (pts.get(0).v() != pts.get(pts.size() - 1).v()) return DataResult.error(() -> "distance_profile: first and last v must be equal so the cycle is continuous");
        }
        return DataResult.success(p);
    }

    @Override
    public double compute(FunctionContext ctx) {
        if (!WorldgenTweaksModule.isTerrainTweaksEnabled()) {
            return 0.0D;
        }
        double x = ctx.blockX(), z = ctx.blockZ();
        double rawD = Math.sqrt(x * x + z * z);
        double d = period > 0.0 ? rawD % period : rawD;
        if (d >= points.get(points.size() - 1).d()) {
            return points.get(points.size() - 1).v();
        }
        for (int i = 1; i < points.size(); i++) {
            Point b = points.get(i);
            if (d <= b.d()) {
                Point a = points.get(i - 1);
                double t = (d - a.d()) / (b.d() - a.d());
                t = t * t * (3.0 - 2.0 * t);
                return Mth.lerp(t, a.v(), b.v());
            }
        }
        return points.get(points.size() - 1).v();
    }

    @Override public double minValue() { return 0.0; }
    @Override public double maxValue() { return 1.0; }
    @Override public KeyDispatchDataCodec<? extends DensityFunction> codec() { return CODEC; }
}
