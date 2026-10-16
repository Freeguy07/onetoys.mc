package com.onetoys.client;

import java.util.Optional;

import com.onetoys.item.StopwatchData;
import com.onetoys.module.gameplay.GameplayTweaksModule;
import com.onetoys.module.gameplay.items.RecoveryCompassTweaks;
import com.onetoys.registry.OnetoysAttachments;
import com.onetoys.registry.OnetoysDataComponents;
import com.onetoys.registry.OnetoysItems;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/**
 * Client-only tick handler maintaining dynamic real-time action bar overlays
 * for the Stopwatch and toggled Recovery Compass while held.
 */
@OnlyIn(Dist.CLIENT)
public class ItemActionBarClientHandler {

    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null) {
            return;
        }

        ItemStack main = player.getMainHandItem();
        ItemStack off = player.getOffhandItem();

        // 1. Dynamic Stopwatch Overlay
        if (GameplayTweaksModule.isStopwatchHotbarDisplayEnabled()) {
            ItemStack stopwatch = main.is(OnetoysItems.STOPWATCH.get()) ? main : (off.is(OnetoysItems.STOPWATCH.get()) ? off : ItemStack.EMPTY);
            if (!stopwatch.isEmpty()) {
                StopwatchData data = stopwatch.get(OnetoysDataComponents.STOPWATCH.get());
                if (data != null) {
                    if (data.active()) {
                        long now = mc.level.getGameTime();
                        long elapsed = data.getDisplayElapsed(now);
                        String sec = StopwatchData.formatSeconds(elapsed);
                        mc.gui.setOverlayMessage(Component.translatable("onetoys.actionbar.stopwatch.ticking", elapsed, sec), false);
                        return;
                    } else if (data.elapsedTicks() > 0L) {
                        String sec = StopwatchData.formatSeconds(data.elapsedTicks());
                        mc.gui.setOverlayMessage(Component.translatable("onetoys.actionbar.stopwatch.paused", data.elapsedTicks(), sec), false);
                        return;
                    }
                }
            }
        }

        // 2. Persistent Toggled Recovery Compass Overlay
        if (RecoveryCompassTweaks.isTweakEnabled() && RecoveryCompassTweaks.get().isHotbarDisplayEnabled()) {
            ItemStack compass = main.is(Items.RECOVERY_COMPASS) ? main : (off.is(Items.RECOVERY_COMPASS) ? off : ItemStack.EMPTY);
            if (!compass.isEmpty() && Boolean.TRUE.equals(player.getData(OnetoysAttachments.RECOVERY_COMPASS_OVERLAY))) {
                Optional<GlobalPos> deathLoc = player.getLastDeathLocation();
                Component message;
                if (deathLoc.isPresent()) {
                    GlobalPos pos = deathLoc.get();
                    BlockPos blockPos = pos.pos();
                    Component dimName = RecoveryCompassTweaks.getDimensionName(pos.dimension());
                    message = Component.translatable(
                            "onetoys.actionbar.recovery_compass.last_death",
                            blockPos.getX(), blockPos.getY(), blockPos.getZ(), dimName
                    );
                } else {
                    message = Component.translatable("onetoys.actionbar.recovery_compass.no_death");
                }
                mc.gui.setOverlayMessage(message, false);
            }
        }
    }
}
