package com.onetoys.client;

import com.onetoys.module.gameplay.GameplayTweaksModule;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Items;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

/**
 * Client-only tooltip handler to display disabled status on charcoal when charcoal is disabled.
 */
@OnlyIn(Dist.CLIENT)
public class CharcoalTooltipHandler {
    private static final Component DISABLED_TOOLTIP = Component.translatable("onetoys.tooltip.charcoal_disabled").withStyle(ChatFormatting.RED);

    public static void onItemTooltip(ItemTooltipEvent event) {
        if (event.getItemStack().is(Items.CHARCOAL) && GameplayTweaksModule.isDisableCharcoal()) {
            event.getToolTip().add(DISABLED_TOOLTIP);
        }
    }
}
