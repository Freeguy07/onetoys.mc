package com.orehall.item;

import com.orehall.module.gameplay.GameplayTweaksModule;
import com.orehall.registry.OrehallDataComponents;

import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class StopwatchItem extends Item {

    public StopwatchItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!GameplayTweaksModule.isStopwatchEnabled()) {
            return InteractionResultHolder.pass(stack);
        }

        var component = OrehallDataComponents.STOPWATCH.get();
        StopwatchData current = stack.getOrDefault(component, StopwatchData.DEFAULT);

        if (player.isShiftKeyDown()) {
            // Sneak-right-click resets stopwatch (no-op if already reset)
            if (!current.active() && current.elapsedTicks() <= 0L) {
                return InteractionResultHolder.pass(stack);
            }
            stack.set(component, StopwatchData.DEFAULT);
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.UI_BUTTON_CLICK, SoundSource.PLAYERS, 0.4F, 1.5F);
            if (!level.isClientSide()) {
                player.displayClientMessage(Component.translatable("orehall.actionbar.stopwatch.reset"), true);
            }
            player.swing(hand, true);
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
        }

        long now = level.getGameTime();
        if (current.active()) {
            // Pausing stopwatch
            long elapsed = current.elapsedTicks() + Math.max(0L, now - current.startTick());
            stack.set(component, new StopwatchData(false, 0L, elapsed));
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.UI_BUTTON_CLICK, SoundSource.PLAYERS, 0.4F, 1.5F);
            if (!level.isClientSide()) {
                player.displayClientMessage(Component.translatable(
                        "orehall.actionbar.stopwatch.paused",
                        elapsed, StopwatchData.formatSeconds(elapsed)
                ), true);
            }
        } else {
            // Starting / Resuming stopwatch
            stack.set(component, new StopwatchData(true, now, current.elapsedTicks()));
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.UI_BUTTON_CLICK, SoundSource.PLAYERS, 0.4F, 1.5F);
        }

        player.swing(hand, true);
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }
}
