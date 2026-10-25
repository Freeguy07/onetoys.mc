package com.onetoys.registry;

import com.onetoys.Onetoys;
import com.onetoys.entity.TangledThreadProjectile;
import com.onetoys.entity.ThrownSpear;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class OnetoysEntityTypes {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(Registries.ENTITY_TYPE, Onetoys.MODID);

    public static final DeferredHolder<EntityType<?>, EntityType<TangledThreadProjectile>> TANGLED_THREAD =
            ENTITY_TYPES.register("tangled_thread", () ->
                    EntityType.Builder.<TangledThreadProjectile>of(TangledThreadProjectile::new, MobCategory.MISC)
                            .sized(0.25F, 0.25F)
                            .clientTrackingRange(4)
                            .updateInterval(10)
                            .build("tangled_thread")
            );

    public static final DeferredHolder<EntityType<?>, EntityType<ThrownSpear>> SPEAR =
            ENTITY_TYPES.register("spear", () ->
                    EntityType.Builder.<ThrownSpear>of(ThrownSpear::new, MobCategory.MISC)
                            .sized(0.5F, 0.5F)
                            .clientTrackingRange(4)
                            .updateInterval(20)
                            .build("spear")
            );

    public static void register(IEventBus modBus) {
        ENTITY_TYPES.register(modBus);
    }
}
