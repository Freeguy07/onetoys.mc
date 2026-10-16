package com.onetoys.salt;

import com.onetoys.Onetoys;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.level.block.DispenserBlock;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;

public class Salt {
    public static final String ID = Onetoys.MODID;

    public static ResourceLocation resource(String path) {
        return ResourceLocation.fromNamespaceAndPath(ID, path);
    }

    public static void init(IEventBus modEventBus, ModContainer modContainer) {
        SaltBlocks.BLOCKS.register(modEventBus);
        SaltItems.ITEMS.register(modEventBus);
        SaltSounds.SOUNDS.register(modEventBus);
        SaltDataComponents.DATA_COMPONENTS.register(modEventBus);
        SaltRecipeSerializers.RECIPE_SERIALIZERS.register(modEventBus);
        SaltWorldGenFeatures.FEATURES.register(modEventBus);
        SaltCriteriaTriggers.CRITERIA_TRIGGERS.register(modEventBus);
    }

    public static void registerDispenserBehaviors() {
        DispenserBlock.registerBehavior(SaltItems.SALT.get(), Melting.SALT_DISPENSER_BEHAVIOR);
    }

    public static void onBuildCreativeTabs(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.INGREDIENTS) {
            event.accept(SaltItems.SALT);
            event.accept(SaltItems.RAW_ROCK_SALT);
        }

        if (event.getTabKey() == CreativeModeTabs.NATURAL_BLOCKS) {
            event.accept(SaltItems.SALT_BLOCK);
            event.accept(SaltItems.ROCK_SALT_ORE);
            event.accept(SaltItems.DEEPSLATE_ROCK_SALT_ORE);
            event.accept(SaltItems.RAW_ROCK_SALT_BLOCK);
        }

        if (event.getTabKey() == CreativeModeTabs.BUILDING_BLOCKS) {
            event.accept(SaltItems.SMALL_SALT_BUD);
            event.accept(SaltItems.MEDIUM_SALT_BUD);
            event.accept(SaltItems.LARGE_SALT_BUD);
            event.accept(SaltItems.SALT_CLUSTER);
            event.accept(SaltItems.SALT_LAMP);
        }
    }
}
