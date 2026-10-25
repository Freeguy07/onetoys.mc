package com.onetoys.registry;

import com.onetoys.Onetoys;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

public final class OnetoysTags {
    private OnetoysTags() {
    }

    public static final class Items {
        private Items() {
        }

        public static final TagKey<Item> PRIMITIVE = ItemTags.create(
                ResourceLocation.fromNamespaceAndPath(Onetoys.MODID, "primitive")
        );
        public static final TagKey<Item> PRIMITIVE_WEAPONS = ItemTags.create(
                ResourceLocation.fromNamespaceAndPath(Onetoys.MODID, "primitive_weapons")
        );
        public static final TagKey<Item> COMMON_PRIMITIVE_WEAPONS = ItemTags.create(
                ResourceLocation.fromNamespaceAndPath("c", "primitive_weapons")
        );

        public static final TagKey<Item> CANNOT_BLOCK_SWORDS = ItemTags.create(
                ResourceLocation.fromNamespaceAndPath(Onetoys.MODID, "cannot_block_swords")
        );
        public static final TagKey<Item> CANNOT_BLOCK = ItemTags.create(
                ResourceLocation.fromNamespaceAndPath(Onetoys.MODID, "cannot_block")
        );
        public static final TagKey<Item> COMMON_CANNOT_BLOCK_SWORDS = ItemTags.create(
                ResourceLocation.fromNamespaceAndPath("c", "cannot_block_swords")
        );

        public static final TagKey<Item> UNBREAKABLE = ItemTags.create(
                ResourceLocation.fromNamespaceAndPath(Onetoys.MODID, "unbreakable")
        );
        public static final TagKey<Item> UNBREAKABLE_ITEMS = ItemTags.create(
                ResourceLocation.fromNamespaceAndPath(Onetoys.MODID, "unbreakable_items")
        );
        public static final TagKey<Item> COMMON_UNBREAKABLE = ItemTags.create(
                ResourceLocation.fromNamespaceAndPath("c", "unbreakable")
        );
    }
}
