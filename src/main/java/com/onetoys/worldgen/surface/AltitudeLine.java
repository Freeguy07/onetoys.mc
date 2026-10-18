package com.onetoys.worldgen.surface;

import com.mojang.serialization.MapCodec;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.world.level.levelgen.SurfaceRules;

import java.util.function.IntSupplier;

public class AltitudeLine implements SurfaceRules.ConditionSource {
    public static final double LINE_SHIFT = 100.0;

    private final IntSupplier baseY;
    private final int surfaceDepthMult;
    private final KeyDispatchDataCodec<AltitudeLine> codec;

    public AltitudeLine(IntSupplier baseY, int surfaceDepthMult) {
        this.baseY = baseY;
        this.surfaceDepthMult = surfaceDepthMult;
        // Dummy codec: condition is never serialized
        this.codec = KeyDispatchDataCodec.of(MapCodec.unit(this));
    }

    @Override
    public KeyDispatchDataCodec<? extends SurfaceRules.ConditionSource> codec() {
        return codec;
    }

    @Override
    public SurfaceRules.Condition apply(SurfaceRules.Context ctx) {
        return new SurfaceRules.Condition() {
            private int lastX = Integer.MIN_VALUE;
            private int lastZ = Integer.MIN_VALUE;
            private double cachedThreshold = Double.NaN;

            @Override
            public boolean test() {
                int x = ctx.blockX;
                int z = ctx.blockZ;
                if (x != lastX || z != lastZ) {
                    lastX = x;
                    lastZ = z;
                    double a = FarAlpha.at(x, z);
                    if (a <= 0.0) {
                        cachedThreshold = Double.NaN;
                    } else {
                        cachedThreshold = baseY.getAsInt() + (1.0 - a) * LINE_SHIFT + ctx.surfaceDepth * surfaceDepthMult;
                    }
                }
                if (Double.isNaN(cachedThreshold)) {
                    return false;
                }
                return ctx.blockY >= cachedThreshold;
            }
        };
    }
}
