package com.onetoys;

import java.util.Optional;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.WeatheringCopper;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;

/** Advances copper oxidation by one stage when used on an unwaxed, not fully oxidized copper block. */
public class CopperPatinaItem extends Item {
    public CopperPatinaItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState state = level.getBlockState(pos);
        Optional<BlockState> next = Optional.ofNullable(net.neoforged.neoforge.common.DataMapHooks.getNextOxidizedStage(state.getBlock()))
                .or(() -> WeatheringCopper.getNext(state.getBlock()))
                .map(block -> block.withPropertiesOf(state));
        if (next.isEmpty()) {
            return InteractionResult.PASS;
        }

        if (!level.isClientSide) {
            Player player = context.getPlayer();
            level.setBlock(pos, next.get(), Block.UPDATE_ALL_IMMEDIATE);
            level.gameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Context.of(player, next.get()));
            level.playSound(null, pos, SoundEvents.AXE_SCRAPE, SoundSource.BLOCKS, 1.0F, 1.0F);
            level.levelEvent(null, 3005, pos, 0);
            context.getItemInHand().consume(1, player);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}

