package com.orehall.registry;

import com.orehall.Orehall;
import com.orehall.item.StopwatchData;

import net.minecraft.core.registries.Registries;
import net.minecraft.core.component.DataComponentType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class OrehallDataComponents {
    public static final DeferredRegister.DataComponents DATA_COMPONENTS =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, Orehall.MODID);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<StopwatchData>> STOPWATCH =
            DATA_COMPONENTS.registerComponentType("stopwatch", builder -> builder
                    .persistent(StopwatchData.CODEC)
                    .networkSynchronized(StopwatchData.STREAM_CODEC)
            );

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> WATERING_CAN_USES =
            DATA_COMPONENTS.registerComponentType("watering_can_uses", builder -> builder
                    .persistent(com.mojang.serialization.Codec.INT)
                    .networkSynchronized(net.minecraft.network.codec.ByteBufCodecs.VAR_INT)
            );

    public static void register(IEventBus modBus) {
        DATA_COMPONENTS.register(modBus);
    }
}
