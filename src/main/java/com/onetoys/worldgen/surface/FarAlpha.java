package com.onetoys.worldgen.surface;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;

public class FarAlpha {
    private static volatile DensityFunction FN;

    public static double at(int x, int z) {
        DensityFunction fn = FN;
        if (fn == null) {
            return 0.0;
        }
        return fn.compute(new DensityFunction.SinglePointContext(x, 64, z));
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
