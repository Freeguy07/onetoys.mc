package com.onetoys.worldgen.surface;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;

public class FarAlpha {
    private static volatile DensityFunction FN;

    private static final class MutablePointContext implements DensityFunction.FunctionContext {
        int x;
        int z;

        @Override
        public int blockX() {
            return x;
        }

        @Override
        public int blockY() {
            return 64;
        }

        @Override
        public int blockZ() {
            return z;
        }
    }

    private static final ThreadLocal<MutablePointContext> CONTEXT = ThreadLocal.withInitial(MutablePointContext::new);

    public static double at(int x, int z) {
        DensityFunction fn = FN;
        if (fn == null) {
            return 0.0;
        }
        MutablePointContext ctx = CONTEXT.get();
        ctx.x = x;
        ctx.z = z;
        return fn.compute(ctx);
    }

    public static void onServerAboutToStart(ServerAboutToStartEvent event) {
        FN = event.getServer()
                .registryAccess()
                .registryOrThrow(Registries.DENSITY_FUNCTION)
                .get(ResourceLocation.fromNamespaceAndPath("onetoys", "far_alpha"));
    }

    public static void onServerStopped(ServerStoppedEvent event) {
        FN = null;
    }
}
