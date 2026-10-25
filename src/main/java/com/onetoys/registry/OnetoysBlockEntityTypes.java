package com.onetoys.registry;

import com.onetoys.Onetoys;
import com.onetoys.block.entity.QuartzGlassBlockEntity;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class OnetoysBlockEntityTypes {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Onetoys.MODID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<QuartzGlassBlockEntity>> QUARTZ_GLASS =
            BLOCK_ENTITY_TYPES.register("quartz_glass", () ->
                    BlockEntityType.Builder.of(QuartzGlassBlockEntity::new, OnetoysBlocks.QUARTZ_GLASS.get()).build(null)
            );

    public static void register(IEventBus bus) {
        BLOCK_ENTITY_TYPES.register(bus);
    }
}
