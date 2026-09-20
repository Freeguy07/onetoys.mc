package com.orehall.registry;

import com.orehall.Orehall;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public class OrehallItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Orehall.MODID);

    public static final DeferredItem<BlockItem> NETHER_LAPIS_ORE = ITEMS.registerSimpleBlockItem(OrehallBlocks.NETHER_LAPIS_ORE);
    public static final DeferredItem<BlockItem> TUNGSTEN_ORE = ITEMS.registerSimpleBlockItem(OrehallBlocks.TUNGSTEN_ORE);
    public static final DeferredItem<BlockItem> RAW_TUNGSTEN_BLOCK = ITEMS.registerSimpleBlockItem(OrehallBlocks.RAW_TUNGSTEN_BLOCK);
    public static final DeferredItem<BlockItem> TUNGSTEN_BLOCK = ITEMS.registerSimpleBlockItem(OrehallBlocks.TUNGSTEN_BLOCK);

    public static final DeferredItem<Item> RAW_TUNGSTEN = ITEMS.registerSimpleItem("raw_tungsten");
    public static final DeferredItem<Item> TUNGSTEN_INGOT = ITEMS.registerSimpleItem("tungsten_ingot");
    public static final DeferredItem<Item> TUNGSTEN_NUGGET = ITEMS.registerSimpleItem("tungsten_nugget");

    public static final DeferredItem<Item> COPPER_NUGGET = ITEMS.registerSimpleItem("copper_nugget");
    public static final DeferredItem<Item> DIAMOND_SHARD = ITEMS.registerSimpleItem("diamond_shard");
    public static final DeferredItem<Item> EMERALD_SHARD = ITEMS.registerSimpleItem("emerald_shard");
    public static final DeferredItem<Item> COAL_LUMP = ITEMS.registerSimpleItem("coal_lump");
    public static final DeferredItem<Item> MATTED_WOOL = ITEMS.registerSimpleItem("matted_wool");
    public static final DeferredItem<Item> STOPWATCH = ITEMS.registerItem(
            "stopwatch",
            com.orehall.item.StopwatchItem::new,
            new Item.Properties().stacksTo(1)
    );
    public static final DeferredItem<com.orehall.item.DumbbellItem> DUMBBELL = ITEMS.registerItem(
            "dumbbell",
            props -> new com.orehall.item.DumbbellItem(OrehallBlocks.DUMBBELL.get(), props),
            new Item.Properties().stacksTo(64).attributes(com.orehall.item.DumbbellItem.createAttributes(1.5))
    );
    public static final DeferredItem<com.orehall.item.WateringCanItem> WATERING_CAN = ITEMS.registerItem(
            "watering_can",
            com.orehall.item.WateringCanItem::new,
            new Item.Properties().stacksTo(1)
    );

    public static void register(IEventBus modBus) {
        ITEMS.register(modBus);
    }
}
