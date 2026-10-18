package com.onetoys.salt;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;

public class SaltTags {
    public static class Items {
        public static final TagKey<Item> CAN_BE_SALTED = ItemTags.create(
                ResourceLocation.fromNamespaceAndPath(Salt.ID, "can_be_salted"));
        public static final TagKey<Item> FORGE_SALTS = ItemTags.create(
                ResourceLocation.fromNamespaceAndPath("forge", "salts"));
        public static final TagKey<Item> COMMON_SALTS = ItemTags.create(
                ResourceLocation.fromNamespaceAndPath("c", "salts"));
    }

    public static class Blocks {
        public static final TagKey<Block> HEATERS = BlockTags.create(
                ResourceLocation.fromNamespaceAndPath(Salt.ID, "heaters"));
        public static final TagKey<Block> SALT_CLUSTER_GROWABLES = BlockTags.create(
                ResourceLocation.fromNamespaceAndPath(Salt.ID, "salt_cluster_growables"));
        public static final TagKey<Block> SALT_DISSOLVABLES = BlockTags.create(
                ResourceLocation.fromNamespaceAndPath(Salt.ID, "salt_dissolvables"));
        public static final TagKey<Block> MELTABLES = BlockTags.create(
                ResourceLocation.fromNamespaceAndPath(Salt.ID, "meltables"));
        public static final TagKey<Block> SALT_CLUSTER_REPLACEABLES = BlockTags.create(
                ResourceLocation.fromNamespaceAndPath(Salt.ID, "salt_cluster_replaceables"));
    }

    public static class Biomes {
        public static final TagKey<Biome> HAS_ROCK_SALT_DEPOSITS = TagKey.create(
                Registries.BIOME, ResourceLocation.fromNamespaceAndPath(Salt.ID, "has_rock_salt_deposits"));
    }
}
