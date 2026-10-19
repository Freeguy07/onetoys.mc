package com.onetoys.registry;

import com.onetoys.Onetoys;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

public class OnetoysFeatures {
    public static final DeferredRegister<Feature<?>> FEATURES =
            DeferredRegister.create(Registries.FEATURE, Onetoys.MODID);

    public static void register(IEventBus modBus) {
        FEATURES.register(modBus);
    }
}