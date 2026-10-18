package com.onetoys.registry;

import com.mojang.serialization.MapCodec;
import com.onetoys.Onetoys;
import com.onetoys.worldgen.density.DistanceProfile;
import com.onetoys.worldgen.density.FarLerp;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class OnetoysDensityFunctions {
    public static final DeferredRegister<MapCodec<? extends DensityFunction>> TYPES =
            DeferredRegister.create(Registries.DENSITY_FUNCTION_TYPE, Onetoys.MODID);

    public static final DeferredHolder<MapCodec<? extends DensityFunction>, MapCodec<DistanceProfile>> DISTANCE_PROFILE =
            TYPES.register("distance_profile", () -> DistanceProfile.MAP_CODEC);
    public static final DeferredHolder<MapCodec<? extends DensityFunction>, MapCodec<FarLerp>> FAR_LERP =
            TYPES.register("far_lerp", () -> FarLerp.MAP_CODEC);

    public static void register(IEventBus modBus) {
        TYPES.register(modBus);
    }
}
