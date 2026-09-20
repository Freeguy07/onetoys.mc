package com.orehall.registry;

import com.orehall.Orehall;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class OrehallCreativeModeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Orehall.MODID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> OREHALL_TAB = CREATIVE_MODE_TABS.register(
            "orehall_tab",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.orehall"))
                    .icon(() -> new ItemStack(OrehallBlocks.NETHER_LAPIS_ORE.get()))
                    .displayItems((parameters, output) -> {
                        output.accept(OrehallBlocks.NETHER_LAPIS_ORE.get());
                        output.accept(OrehallBlocks.TUNGSTEN_ORE.get());
                        output.accept(OrehallBlocks.RAW_TUNGSTEN_BLOCK.get());
                        output.accept(OrehallBlocks.TUNGSTEN_BLOCK.get());
                        output.accept(OrehallItems.RAW_TUNGSTEN.get());
                        output.accept(OrehallItems.TUNGSTEN_INGOT.get());
                        output.accept(OrehallItems.TUNGSTEN_NUGGET.get());
                        output.accept(OrehallItems.DUMBBELL.get());
                        output.accept(OrehallItems.WATERING_CAN.get());
                        output.accept(OrehallItems.COPPER_NUGGET.get());
                        output.accept(OrehallItems.DIAMOND_SHARD.get());
                        output.accept(OrehallItems.EMERALD_SHARD.get());
                        output.accept(OrehallItems.COAL_LUMP.get());
                        output.accept(OrehallItems.MATTED_WOOL.get());
                        output.accept(OrehallItems.STOPWATCH.get());
                    })
                    .build()
    );

    public static void register(IEventBus modBus) {
        CREATIVE_MODE_TABS.register(modBus);
    }
}
