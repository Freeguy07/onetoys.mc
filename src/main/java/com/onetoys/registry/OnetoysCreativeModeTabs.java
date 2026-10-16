package com.onetoys.registry;

import com.onetoys.Onetoys;
import com.onetoys.salt.SaltItems;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class OnetoysCreativeModeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Onetoys.MODID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> ONETOYS_TAB = CREATIVE_MODE_TABS.register(
            "onetoys_tab",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.onetoys"))
                    .icon(() -> new ItemStack(OnetoysBlocks.NETHER_LAPIS_ORE.get()))
                    .displayItems((parameters, output) -> {
                        output.accept(OnetoysBlocks.NETHER_LAPIS_ORE.get());
                        output.accept(OnetoysBlocks.TUNGSTEN_ORE.get());
                        output.accept(OnetoysBlocks.RAW_TUNGSTEN_BLOCK.get());
                        output.accept(OnetoysBlocks.TUNGSTEN_BLOCK.get());
                        output.accept(OnetoysItems.RAW_TUNGSTEN.get());
                        output.accept(OnetoysItems.TUNGSTEN_INGOT.get());
                        output.accept(OnetoysItems.TUNGSTEN_NUGGET.get());
                        output.accept(OnetoysItems.DUMBBELL.get());
                        output.accept(OnetoysItems.WATERING_CAN.get());
                        output.accept(OnetoysBlocks.SPRINKLER.get());
                        output.accept(OnetoysItems.COPPER_NUGGET.get());
                        output.accept(OnetoysItems.DIAMOND_SHARD.get());
                        output.accept(OnetoysItems.EMERALD_SHARD.get());
                        output.accept(OnetoysItems.COAL_LUMP.get());
                        output.accept(OnetoysItems.MATTED_WOOL.get());
                        output.accept(OnetoysItems.STOPWATCH.get());
                        output.accept(Onetoys.BARK.get());
                        output.accept(Onetoys.COPPER_PATINA.get());
                        output.accept(Onetoys.BLUESTONE.get());
                        output.accept(Onetoys.CHRYSANTHEMUM.get());
                        output.accept(SaltItems.SALT.get());
                        output.accept(SaltItems.RAW_ROCK_SALT.get());
                        output.accept(SaltItems.SALT_BLOCK.get());
                        output.accept(SaltItems.ROCK_SALT_ORE.get());
                        output.accept(SaltItems.DEEPSLATE_ROCK_SALT_ORE.get());
                        output.accept(SaltItems.RAW_ROCK_SALT_BLOCK.get());
                        output.accept(SaltItems.SMALL_SALT_BUD.get());
                        output.accept(SaltItems.MEDIUM_SALT_BUD.get());
                        output.accept(SaltItems.LARGE_SALT_BUD.get());
                        output.accept(SaltItems.SALT_CLUSTER.get());
                        output.accept(SaltItems.SALT_LAMP.get());
                    })
                    .build()
    );

    public static void register(IEventBus modBus) {
        CREATIVE_MODE_TABS.register(modBus);
    }
}
