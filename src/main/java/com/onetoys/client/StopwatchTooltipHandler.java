package com.onetoys.client;

import java.util.List;

import com.onetoys.item.StopwatchData;
import com.onetoys.registry.OnetoysDataComponents;
import com.onetoys.registry.OnetoysItems;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

/**
 * Client-only tooltip handler for the Stopwatch item matching the Recovery Compass UX pattern.
 */
public class StopwatchTooltipHandler {
    private static final Component HOLD_SHIFT = Component.translatable("onetoys.tooltip.stopwatch.hold_shift").withStyle(ChatFormatting.DARK_GRAY);
    private static final Component NO_TIME = Component.translatable("onetoys.tooltip.stopwatch.no_time").withStyle(ChatFormatting.GRAY);
    private static final Component HINT_PAUSE = Component.translatable("onetoys.tooltip.stopwatch.hint.pause").withStyle(ChatFormatting.GRAY);
    private static final Component HINT_RESUME = Component.translatable("onetoys.tooltip.stopwatch.hint.resume").withStyle(ChatFormatting.GRAY);
    private static final Component HINT_RESET = Component.translatable("onetoys.tooltip.stopwatch.hint.reset").withStyle(ChatFormatting.DARK_GRAY);
    private static final Component HINT_START = Component.translatable("onetoys.tooltip.stopwatch.hint.start").withStyle(ChatFormatting.DARK_GRAY);

    public static void onItemTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        if (!stack.is(OnetoysItems.STOPWATCH.get())) {
            return;
        }

        if (!com.onetoys.module.gameplay.GameplayTweaksModule.isStopwatchEnabled()) {
            return;
        }

        List<Component> tooltip = event.getToolTip();

        if (Screen.hasShiftDown()) {
            StopwatchData data = stack.get(OnetoysDataComponents.STOPWATCH.get());
            if (data == null || (!data.active() && data.elapsedTicks() <= 0L)) {
                tooltip.add(NO_TIME);
                tooltip.add(HINT_START);
                return;
            }

            Player player = event.getEntity();
            long currentTick = (player != null) ? player.level().getGameTime() : 0L;
            long elapsed = data.getDisplayElapsed(currentTick);
            String seconds = StopwatchData.formatSeconds(elapsed);

            if (data.active()) {
                tooltip.add(Component.translatable("onetoys.tooltip.stopwatch.elapsed", elapsed, seconds)
                        .withStyle(ChatFormatting.GOLD));
                tooltip.add(HINT_PAUSE);
            } else {
                tooltip.add(Component.translatable("onetoys.tooltip.stopwatch.paused", elapsed, seconds)
                        .withStyle(ChatFormatting.GOLD));
                tooltip.add(HINT_RESUME);
                tooltip.add(HINT_RESET);
            }
        } else {
            tooltip.add(HOLD_SHIFT);
        }
    }
}
