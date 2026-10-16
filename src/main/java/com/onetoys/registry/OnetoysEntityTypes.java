package com.onetoys.registry;

import com.onetoys.Onetoys;
import com.onetoys.entity.SprinklerDropletEntity;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class OnetoysEntityTypes {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(Registries.ENTITY_TYPE, Onetoys.MODID);

    public static final DeferredHolder<EntityType<?>, EntityType<SprinklerDropletEntity>> SPRINKLER_DROPLET =
            ENTITY_TYPES.register("sprinkler_droplet", () ->
                    EntityType.Builder.<SprinklerDropletEntity>of(SprinklerDropletEntity::new, MobCategory.MISC)
                            .sized(0.25F, 0.25F)
                            .clientTrackingRange(64)
                            .updateInterval(1)
                            .build("sprinkler_droplet")
            );

    public static void register(IEventBus modBus) {
        ENTITY_TYPES.register(modBus);
    }
}
