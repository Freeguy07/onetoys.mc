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
    public static final DeferredItem<BlockItem> CHROME_ORE = ITEMS.registerSimpleBlockItem(OrehallBlocks.CHROME_ORE);
    public static final DeferredItem<BlockItem> RAW_CHROME_BLOCK = ITEMS.registerSimpleBlockItem(OrehallBlocks.RAW_CHROME_BLOCK);
    public static final DeferredItem<BlockItem> CHROME_BLOCK = ITEMS.registerSimpleBlockItem(OrehallBlocks.CHROME_BLOCK);

    public static final DeferredItem<Item> RAW_CHROME = ITEMS.registerSimpleItem("raw_chrome");
    public static final DeferredItem<Item> CHROME_INGOT = ITEMS.registerSimpleItem("chrome_ingot");
    public static final DeferredItem<Item> CHROME_NUGGET = ITEMS.registerSimpleItem("chrome_nugget");

    public static final DeferredItem<Item> COPPER_NUGGET = ITEMS.registerSimpleItem("copper_nugget");
    public static final DeferredItem<Item> DIAMOND_SHARD = ITEMS.registerSimpleItem("diamond_shard");
    public static final DeferredItem<Item> EMERALD_SHARD = ITEMS.registerSimpleItem("emerald_shard");
    public static final DeferredItem<Item> COAL_LUMP = ITEMS.registerSimpleItem("coal_lump");

    public static void register(IEventBus modBus) {
        ITEMS.register(modBus);
    }
}
