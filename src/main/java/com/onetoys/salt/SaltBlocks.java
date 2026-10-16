package com.onetoys.salt;

import com.onetoys.salt.block.SaltBlock;
import com.onetoys.salt.block.SaltCauldronBlock;
import com.onetoys.salt.block.SaltClusterBlock;
import com.onetoys.salt.block.SaltSandBlock;
import net.minecraft.core.cauldron.CauldronInteraction;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public class SaltBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Salt.ID);

    public static final DeferredBlock<SaltSandBlock> SALT_BLOCK = BLOCKS.register("salt_block",
            () -> new SaltSandBlock(0xe7d5cf, BlockBehaviour.Properties.ofFullCopy(Blocks.SAND)
                    .mapColor(MapColor.COLOR_LIGHT_GRAY)
                    .randomTicks()));

    public static final DeferredBlock<SaltBlock> ROCK_SALT_ORE = BLOCKS.register("rock_salt_ore",
            () -> new SaltBlock(Blocks.STONE.defaultBlockState(), BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_LIGHT_GRAY)
                    .randomTicks()
                    .strength(2.5F)
                    .requiresCorrectToolForDrops()
                    .sound(SaltSounds.Types.SALT)));

    public static final DeferredBlock<SaltBlock> DEEPSLATE_ROCK_SALT_ORE = BLOCKS.register("deepslate_rock_salt_ore",
            () -> new SaltBlock(Blocks.DEEPSLATE.defaultBlockState(), BlockBehaviour.Properties.ofFullCopy(ROCK_SALT_ORE.get())
                    .mapColor(MapColor.COLOR_GRAY)));

    public static final DeferredBlock<SaltBlock> RAW_ROCK_SALT_BLOCK = BLOCKS.register("raw_rock_salt_block",
            () -> new SaltBlock(BlockBehaviour.Properties.ofFullCopy(ROCK_SALT_ORE.get())
                    .sound(SaltSounds.Types.SALT_CLUSTER)));

    public static final DeferredBlock<SaltClusterBlock> SALT_CLUSTER = BLOCKS.register("salt_cluster",
            () -> new SaltClusterBlock(7, 3, BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_LIGHT_GRAY)
                    .noOcclusion()
                    .randomTicks()
                    .strength(1.5F)
                    .sound(SaltSounds.Types.SALT_CLUSTER)
                    .lightLevel(state -> 3)
                    .dynamicShape()));

    public static final DeferredBlock<SaltClusterBlock> LARGE_SALT_BUD = BLOCKS.register("large_salt_bud",
            () -> new SaltClusterBlock(5, 3, BlockBehaviour.Properties.ofFullCopy(SALT_CLUSTER.get())
                    .lightLevel(state -> 2)
                    .sound(SaltSounds.Types.LARGE_SALT_BUD)));

    public static final DeferredBlock<SaltClusterBlock> MEDIUM_SALT_BUD = BLOCKS.register("medium_salt_bud",
            () -> new SaltClusterBlock(4, 3, BlockBehaviour.Properties.ofFullCopy(SALT_CLUSTER.get())
                    .lightLevel(state -> 2)
                    .sound(SaltSounds.Types.MEDIUM_SALT_BUD)));

    public static final DeferredBlock<SaltClusterBlock> SMALL_SALT_BUD = BLOCKS.register("small_salt_bud",
            () -> new SaltClusterBlock(3, 4, BlockBehaviour.Properties.ofFullCopy(SALT_CLUSTER.get())
                    .lightLevel(state -> 1)
                    .sound(SaltSounds.Types.SMALL_SALT_BUD)));

    public static final DeferredBlock<SaltCauldronBlock> SALT_CAULDRON = BLOCKS.register("salt_cauldron",
            () -> new SaltCauldronBlock(Biome.Precipitation.RAIN, CauldronInteraction.EMPTY,
                    BlockBehaviour.Properties.ofFullCopy(Blocks.CAULDRON)));

    public static final DeferredBlock<SaltBlock> SALT_LAMP = BLOCKS.register("salt_lamp",
            () -> new SaltBlock(Blocks.SPRUCE_SLAB.defaultBlockState(),
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.COLOR_ORANGE)
                            .sound(SaltSounds.Types.SALT)
                            .randomTicks()
                            .strength(2f)
                            .lightLevel(blockState -> 15)));
}

