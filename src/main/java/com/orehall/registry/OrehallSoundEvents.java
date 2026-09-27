package com.orehall.registry;

import com.orehall.Orehall;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class OrehallSoundEvents {
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS =
            DeferredRegister.create(Registries.SOUND_EVENT, Orehall.MODID);

    public static final DeferredHolder<SoundEvent, SoundEvent> SPRINKLER_ACTIVATE =
            SOUND_EVENTS.register("block.sprinkler.activate",
                    () -> SoundEvent.createVariableRangeEvent(
                            ResourceLocation.fromNamespaceAndPath(Orehall.MODID, "block.sprinkler.activate")
                    ));

    public static final DeferredHolder<SoundEvent, SoundEvent> TINY_SPLASH =
            SOUND_EVENTS.register("water.tiny_splash",
                    () -> SoundEvent.createVariableRangeEvent(
                            ResourceLocation.fromNamespaceAndPath(Orehall.MODID, "water.tiny_splash")
                    ));

    public static void register(IEventBus modBus) {
        SOUND_EVENTS.register(modBus);
    }
}
