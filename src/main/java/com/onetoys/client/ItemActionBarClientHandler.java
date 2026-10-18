package com.onetoys.client;

import java.util.Optional;

import com.onetoys.item.StopwatchData;
import com.onetoys.mixin.client.GuiAccessor;
import com.onetoys.module.gameplay.GameplayTweaksModule;
import com.onetoys.module.gameplay.items.RecoveryCompassTweaks;
import com.onetoys.registry.OnetoysAttachments;
import com.onetoys.registry.OnetoysDataComponents;
import com.onetoys.registry.OnetoysItems;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/**
 * Client-only tick handler maintaining dynamic real-time action bar overlays
 * for the Stopwatch and toggled Recovery Compass while held.
 */
public class ItemActionBarClientHandler {

    private static Component lastSentMessage = null;

    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null) {
            return;
        }

        Gui gui = mc.gui;
        GuiAccessor guiAccessor = (GuiAccessor) gui;
        int currentOverlayTime = guiAccessor.getOverlayMessageTime();
        Component currentOverlayMessage = guiAccessor.getOverlayMessageString();

        // If a foreign (vanilla or other mod) message is currently displayed, do not overwrite it
        boolean isForeignMessageActive = currentOverlayTime > 0
                && (lastSentMessage == null || (currentOverlayMessage != null && !currentOverlayMessage.equals(lastSentMessage)));

        if (isForeignMessageActive) {
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
                        Component message = Component.translatable("onetoys.actionbar.stopwatch.ticking", elapsed, sec);
                        gui.setOverlayMessage(message, false);
                        lastSentMessage = message;
                        return;
                    } else if (data.elapsedTicks() > 0L) {
                        long elapsed = data.elapsedTicks();
                        String sec = StopwatchData.formatSeconds(elapsed);
                        Component message = Component.translatable("onetoys.actionbar.stopwatch.paused", elapsed, sec);
                        boolean isOurCurrentMessage = currentOverlayTime > 0 && lastSentMessage != null && lastSentMessage.equals(message);
                        if (!isOurCurrentMessage || currentOverlayTime <= 10) {
                            gui.setOverlayMessage(message, false);
                            lastSentMessage = message;
                        }
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
                boolean isOurCurrentMessage = currentOverlayTime > 0 && lastSentMessage != null && lastSentMessage.equals(message);
                if (!isOurCurrentMessage || currentOverlayTime <= 10) {
                    gui.setOverlayMessage(message, false);
                    lastSentMessage = message;
                }
                return;
            }
        }

        // If neither stopwatch nor compass is active, clear lastSentMessage once expired
        if (currentOverlayTime <= 0) {
            lastSentMessage = null;
        }
    }
}
