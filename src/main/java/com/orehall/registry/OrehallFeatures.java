package com.orehall.registry;

import com.orehall.Orehall;
import com.orehall.worldgen.feature.CalciteStrataConfiguration;
import com.orehall.worldgen.feature.CalciteStrataFeature;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class OrehallFeatures {
    public static final DeferredRegister<Feature<?>> FEATURES =
            DeferredRegister.create(Registries.FEATURE, Orehall.MODID);

    public static final DeferredHolder<Feature<?>, CalciteStrataFeature> CALCITE_STRATA =
            FEATURES.register("calcite_strata", () -> new CalciteStrataFeature(CalciteStrataConfiguration.CODEC));

    public static void register(IEventBus modBus) {
        FEATURES.register(modBus);
    }
}
