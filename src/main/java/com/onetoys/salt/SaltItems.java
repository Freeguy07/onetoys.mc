package com.onetoys.salt;

import com.onetoys.salt.item.SaltItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public class SaltItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Salt.ID);

    public static final DeferredItem<SaltItem> SALT = ITEMS.register("salt",
            () -> new SaltItem(new Item.Properties()));

    public static final DeferredItem<Item> RAW_ROCK_SALT = ITEMS.register("raw_rock_salt",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<BlockItem> SALT_BLOCK = ITEMS.register("salt_block",
            () -> new BlockItem(SaltBlocks.SALT_BLOCK.get(), new Item.Properties()));

    public static final DeferredItem<BlockItem> ROCK_SALT_ORE = ITEMS.register("rock_salt_ore",
            () -> new BlockItem(SaltBlocks.ROCK_SALT_ORE.get(), new Item.Properties()));

    public static final DeferredItem<BlockItem> DEEPSLATE_ROCK_SALT_ORE = ITEMS.register("deepslate_rock_salt_ore",
            () -> new BlockItem(SaltBlocks.DEEPSLATE_ROCK_SALT_ORE.get(), new Item.Properties()));

    public static final DeferredItem<BlockItem> RAW_ROCK_SALT_BLOCK = ITEMS.register("raw_rock_salt_block",
            () -> new BlockItem(SaltBlocks.RAW_ROCK_SALT_BLOCK.get(), new Item.Properties()));

    public static final DeferredItem<BlockItem> SALT_CLUSTER = ITEMS.register("salt_cluster",
            () -> new BlockItem(SaltBlocks.SALT_CLUSTER.get(), new Item.Properties()));

    public static final DeferredItem<BlockItem> LARGE_SALT_BUD = ITEMS.register("large_salt_bud",
            () -> new BlockItem(SaltBlocks.LARGE_SALT_BUD.get(), new Item.Properties()));

    public static final DeferredItem<BlockItem> MEDIUM_SALT_BUD = ITEMS.register("medium_salt_bud",
            () -> new BlockItem(SaltBlocks.MEDIUM_SALT_BUD.get(), new Item.Properties()));

    public static final DeferredItem<BlockItem> SMALL_SALT_BUD = ITEMS.register("small_salt_bud",
            () -> new BlockItem(SaltBlocks.SMALL_SALT_BUD.get(), new Item.Properties()));

    public static final DeferredItem<BlockItem> SALT_LAMP = ITEMS.register("salt_lamp",
            () -> new BlockItem(SaltBlocks.SALT_LAMP.get(), new Item.Properties()));
}

