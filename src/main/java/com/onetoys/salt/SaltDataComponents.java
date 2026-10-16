package com.onetoys.salt;

import com.mojang.serialization.Codec;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.Unit;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class SaltDataComponents {
    public static final DeferredRegister.DataComponents DATA_COMPONENTS =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, Salt.ID);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Unit>> SALTED =
            DATA_COMPONENTS.registerComponentType("salted", builder ->
                    builder.persistent(Codec.unit(Unit.INSTANCE))
                            .networkSynchronized(StreamCodec.unit(Unit.INSTANCE)));

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Unit>> SWEETENED =
            DATA_COMPONENTS.registerComponentType("sweetened", builder ->
                    builder.persistent(Codec.unit(Unit.INSTANCE))
                            .networkSynchronized(StreamCodec.unit(Unit.INSTANCE)));

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Unit>> SATIATING =
            DATA_COMPONENTS.registerComponentType("satiating", builder ->
                    builder.persistent(Codec.unit(Unit.INSTANCE))
                            .networkSynchronized(StreamCodec.unit(Unit.INSTANCE)));
}

