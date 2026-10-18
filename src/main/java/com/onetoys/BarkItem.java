package com.onetoys;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.onetoys.mixin.AxeItemAccessor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.neoforged.neoforge.registries.datamaps.builtin.NeoForgeDataMaps;
import net.neoforged.neoforge.registries.datamaps.builtin.Strippable;

/** Edible bark that can also be pressed onto a stripped log to restore its bark. */
public class BarkItem extends Item {
    private static Map<Block, Block> UNSTRIP_MAP = null;

    private static synchronized Map<Block, Block> getUnstripMap() {
        if (UNSTRIP_MAP == null) {
            Map<Block, Block> map = new HashMap<>();
            AxeItemAccessor.getStrippables().forEach((original, result) -> map.put(result, original));
            var dataMap = BuiltInRegistries.BLOCK.getDataMap(NeoForgeDataMaps.STRIPPABLES);
            if (dataMap != null) {
                dataMap.forEach((key, strippable) -> {
                    Block original = BuiltInRegistries.BLOCK.get(key);
                    if (original != null && strippable != null && strippable.strippedBlock() != null) {
                        map.put(strippable.strippedBlock(), original);
                    }
                });
            }
            UNSTRIP_MAP = Collections.unmodifiableMap(map);
        }
        return UNSTRIP_MAP;
    }

    public static synchronized void invalidateCache() {
        UNSTRIP_MAP = null;
    }

    /** Reverse of vanilla stripping: returns the barked block for a stripped one, or null. */
    public static Block unstrip(Block stripped) {
        return getUnstripMap().get(stripped);
    }

    public BarkItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState state = level.getBlockState(pos);
        Block original = unstrip(state.getBlock());
        if (original == null) {
            return InteractionResult.PASS;
        }

        if (!level.isClientSide) {
            Player player = context.getPlayer();
            BlockState restored = original.withPropertiesOf(state);
            level.setBlock(pos, restored, Block.UPDATE_ALL_IMMEDIATE);
            level.gameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Context.of(player, restored));
            level.playSound(null, pos, SoundEvents.AXE_STRIP, SoundSource.BLOCKS, 1.0F, 1.0F);
            context.getItemInHand().consume(1, player);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
