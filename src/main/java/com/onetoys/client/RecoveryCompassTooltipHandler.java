package com.onetoys.client;

import java.util.List;
import java.util.Optional;

import com.onetoys.module.gameplay.items.RecoveryCompassTweaks;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

/**
 * Client-only tooltip handler for displaying death coordinates on recovery compasses.
 */
public class RecoveryCompassTooltipHandler {
    private static final Component NO_DEATH = Component.translatable("onetoys.tooltip.recovery_compass.no_death").withStyle(ChatFormatting.GRAY);
    private static final Component HOLD_SHIFT = Component.translatable("onetoys.tooltip.recovery_compass.hold_shift").withStyle(ChatFormatting.DARK_GRAY);

    public static void onItemTooltip(ItemTooltipEvent event) {
        if (!event.getItemStack().is(Items.RECOVERY_COMPASS)) {
            return;
        }

        RecoveryCompassTweaks tweaks = RecoveryCompassTweaks.get();
        if (tweaks == null || !tweaks.isTooltipEnabled()) {
            return;
        }

        Player player = event.getEntity();
        if (player == null) {
            return;
        }

        List<Component> tooltip = event.getToolTip();

        if (Screen.hasShiftDown()) {
            Optional<GlobalPos> deathLoc = player.getLastDeathLocation();
            if (deathLoc.isPresent()) {
                GlobalPos pos = deathLoc.get();
                BlockPos blockPos = pos.pos();
                Component dimName = RecoveryCompassTweaks.getDimensionName(pos.dimension());
                tooltip.add(Component.translatable(
                        "onetoys.tooltip.recovery_compass.last_death",
                        blockPos.getX(), blockPos.getY(), blockPos.getZ(), dimName
                ).withStyle(ChatFormatting.GOLD));
            } else {
                tooltip.add(NO_DEATH);
            }
        } else {
            tooltip.add(HOLD_SHIFT);
        }
    }
}
