package com.orehall.registry;

import com.orehall.Orehall;
import com.orehall.block.NetherLapisOreBlock;

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

public class OrehallBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Orehall.MODID);

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

    public static final DeferredBlock<Block> CHROME_ORE = BLOCKS.register(
            "chrome_ore",
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

    public static final DeferredBlock<Block> RAW_CHROME_BLOCK = BLOCKS.register(
            "raw_chrome_block",
            () -> new Block(
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.COLOR_BLACK)
                            .instrument(NoteBlockInstrument.BASEDRUM)
                            .requiresCorrectToolForDrops()
                            .strength(5.0F, 6.0F)
                            .sound(SoundType.STONE)
            )
    );

    public static final DeferredBlock<Block> CHROME_BLOCK = BLOCKS.register(
            "chrome_block",
            () -> new Block(
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.METAL)
                            .instrument(NoteBlockInstrument.IRON_XYLOPHONE)
                            .requiresCorrectToolForDrops()
                            .strength(5.0F, 6.0F)
                            .sound(SoundType.NETHERITE_BLOCK)
            )
    );

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
    }
}
