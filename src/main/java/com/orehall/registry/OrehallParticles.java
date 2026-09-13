package com.orehall.registry;

import com.orehall.Orehall;

import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class OrehallParticles {
    public static final DeferredRegister<ParticleType<?>> PARTICLE_TYPES =
            DeferredRegister.create(Registries.PARTICLE_TYPE, Orehall.MODID);

    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> DRIPPING_NETHER_LAPIS =
            PARTICLE_TYPES.register("dripping_nether_lapis", () -> new SimpleParticleType(false));

    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> FALLING_NETHER_LAPIS =
            PARTICLE_TYPES.register("falling_nether_lapis", () -> new SimpleParticleType(false));

    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> LANDING_NETHER_LAPIS =
            PARTICLE_TYPES.register("landing_nether_lapis", () -> new SimpleParticleType(false));

    public static void register(IEventBus modBus) {
        PARTICLE_TYPES.register(modBus);
    }
}
