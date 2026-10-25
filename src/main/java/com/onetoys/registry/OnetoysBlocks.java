package com.onetoys.registry;

import com.onetoys.Onetoys;
import com.onetoys.block.NetherLapisOreBlock;
import com.onetoys.block.QuartzGlassBlock;

import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DropExperienceBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public class OnetoysBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Onetoys.MODID);

    public static final DeferredBlock<Block> NETHER_LAPIS_ORE = BLOCKS.register(
            "nether_lapis_ore",
            () -> new NetherLapisOreBlock(
                    UniformInt.of(2, 5),
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.NETHER)
                            .instrument(NoteBlockInstrument.BASEDRUM)
                            .requiresCorrectToolForDrops()
                            .strength(3.0F, 3.0F)
                            .sound(SoundType.NETHER_ORE)
            )
    );

    public static final DeferredBlock<Block> TUNGSTEN_ORE = BLOCKS.register(
            "tungsten_ore",
            () -> new DropExperienceBlock(
                    UniformInt.of(0, 1),
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.COLOR_BLACK)
                            .instrument(NoteBlockInstrument.BASEDRUM)
                            .requiresCorrectToolForDrops()
                            .strength(3.0F, 3.0F)
                            .sound(SoundType.NETHER_GOLD_ORE)
            )
    );

    public static final DeferredBlock<Block> RAW_TUNGSTEN_BLOCK = BLOCKS.register(
            "raw_tungsten_block",
            () -> new Block(
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.COLOR_BLACK)
                            .instrument(NoteBlockInstrument.BASEDRUM)
                            .requiresCorrectToolForDrops()
                            .strength(5.0F, 6.0F)
                            .sound(SoundType.STONE)
            )
    );

    public static final DeferredBlock<Block> TUNGSTEN_BLOCK = BLOCKS.register(
            "tungsten_block",
            () -> new Block(
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.METAL)
                            .instrument(NoteBlockInstrument.IRON_XYLOPHONE)
                            .requiresCorrectToolForDrops()
                            .strength(5.0F, 6.0F)
                            .sound(SoundType.NETHERITE_BLOCK)
            )
    );

    public static final DeferredBlock<Block> DUMBBELL = BLOCKS.register(
            "dumbbell",
            () -> new com.onetoys.block.DumbbellBlock(
                    BlockBehaviour.Properties.of()
                            .noOcclusion()
                            .isSuffocating((state, level, pos) -> false)
                            .isViewBlocking((state, level, pos) -> false)
                            .isRedstoneConductor((state, level, pos) -> false)
                            .mapColor(MapColor.METAL)
                            .instrument(NoteBlockInstrument.IRON_XYLOPHONE)
                            .requiresCorrectToolForDrops()
                            .strength(5.0F, 1200.0F)
                            .sound(SoundType.ANVIL)
            )
    );

    public static final DeferredBlock<Block> QUARTZ_GLASS = BLOCKS.register(
            "quartz_glass",
            () -> new QuartzGlassBlock(
                    BlockBehaviour.Properties.of()
                            .noOcclusion()
                            .isSuffocating((state, level, pos) -> false)
                            .isViewBlocking((state, level, pos) -> false)
                            .isRedstoneConductor((state, level, pos) -> false)
                            .mapColor(MapColor.QUARTZ)
                            .strength(0.3F)
                            .sound(SoundType.GLASS)
            )
    );

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
    }
}
