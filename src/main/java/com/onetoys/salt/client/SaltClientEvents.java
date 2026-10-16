package com.onetoys.salt.client;

import com.onetoys.salt.Salt;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ModelEvent;

public class SaltClientEvents {
    public static final ModelResourceLocation SALTED_OVERLAY_MODEL =
            ModelResourceLocation.standalone(ResourceLocation.fromNamespaceAndPath(Salt.ID, "item/salted_overlay"));
    public static final ModelResourceLocation SUGAR_OVERLAY_MODEL =
            ModelResourceLocation.standalone(ResourceLocation.fromNamespaceAndPath(Salt.ID, "item/sugar_overlay"));
    public static final ModelResourceLocation CHRYSANTHEMUM_OVERLAY_MODEL =
            ModelResourceLocation.standalone(ResourceLocation.fromNamespaceAndPath(Salt.ID, "item/chrysanthemum_overlay"));

    public static void init(IEventBus modEventBus) {
        modEventBus.addListener(SaltClientEvents::registerModels);
        modEventBus.addListener(SaltClientEvents::onBakingCompleted);
    }

    public static void registerModels(ModelEvent.RegisterAdditional event) {
        event.register(SALTED_OVERLAY_MODEL);
        event.register(SUGAR_OVERLAY_MODEL);
        event.register(CHRYSANTHEMUM_OVERLAY_MODEL);
    }

    public static void onBakingCompleted(ModelEvent.BakingCompleted event) {
        LayeredBakedModel.Cache.clear();
    }
}

