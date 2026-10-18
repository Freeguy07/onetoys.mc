package com.onetoys.registry;

import com.onetoys.Onetoys;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class OnetoysSoundEvents {
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS =
            DeferredRegister.create(Registries.SOUND_EVENT, Onetoys.MODID);

    public static final DeferredHolder<SoundEvent, SoundEvent> TINY_SPLASH =
            SOUND_EVENTS.register("water.tiny_splash",
                    () -> SoundEvent.createVariableRangeEvent(
                            ResourceLocation.fromNamespaceAndPath(Onetoys.MODID, "water.tiny_splash")
                    ));

    public static void register(IEventBus modBus) {
        SOUND_EVENTS.register(modBus);
    }
}
