package com.orehall.module.gameplay.items;

import java.util.ArrayList;
import java.util.List;

import com.orehall.registry.OrehallAttachments;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentEffectComponents;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

import com.orehall.module.ModuleManager;
import com.orehall.module.gameplay.GameplayTweaksModule;

public class RecoveryCompassTweaks {
    private final ItemTweaks itemTweaks;
    private ModConfigSpec.BooleanValue enabledConfig;
    private ModConfigSpec.BooleanValue persistOnDeathConfig;

    public RecoveryCompassTweaks(ItemTweaks itemTweaks) {
        this.itemTweaks = itemTweaks;
    }

    public RecoveryCompassTweaks() {
        this(null);
    }

    public void buildConfig(ModConfigSpec.Builder builder) {
        builder.comment("Tweaks specific to the recovery compass.")
                .translation("orehall.configuration.gameplay_tweaks.item_tweaks.recovery_compass")
                .push("recovery_compass");

        enabledConfig = builder
                .comment("Enable recovery compass tweaks.")
                .translation("orehall.configuration.gameplay_tweaks.item_tweaks.recovery_compass.enabled")
                .define("enabled", true);

        persistOnDeathConfig = builder
                .comment("When enabled, recovery compasses persist through death.",
                        "They do not drop from the player inventory and are not stored in gravestones.")
                .translation("orehall.configuration.gameplay_tweaks.item_tweaks.recovery_compass.persist_on_death")
                .define("persist_on_death", true);

        builder.pop();
    }

    public boolean isEnabled() {
        return enabledConfig == null || enabledConfig.get();
    }

    public boolean isPersistOnDeathEnabled() {
        if (itemTweaks != null && !itemTweaks.isEnabled()) {
            return false;
        }
        if (!ModuleManager.get().isModuleEnabled(GameplayTweaksModule.ID)) {
            return false;
        }
        return isEnabled() && (persistOnDeathConfig == null || persistOnDeathConfig.get());
    }

    public void onConstruct(IEventBus modBus) {
        // Intercept death at HIGHEST priority to extract recovery compasses before gravestone mods or dropAll() can touch them
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGHEST, this::onLivingDeathPre);
        // Intercept at LOWEST priority to restore compasses if death was cancelled by another mod/totem
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, true, this::onLivingDeathPost);
        // Restore compasses to the cloned player upon respawning
        NeoForge.EVENT_BUS.addListener(this::onPlayerClone);
    }

    private void onLivingDeathPre(LivingDeathEvent event) {
        if (!isPersistOnDeathEnabled()) {
            return;
        }

        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        Inventory inventory = player.getInventory();
        List<SavedRecoveryCompass> saved = new ArrayList<>();

        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (stack.is(Items.RECOVERY_COMPASS)) {
                // Respect Curse of Vanishing: if the compass has the vanishing curse, let vanilla destroy it
                if (EnchantmentHelper.has(stack, EnchantmentEffectComponents.PREVENT_EQUIPMENT_DROP)) {
                    continue;
                }
                saved.add(new SavedRecoveryCompass(slot, stack.copy()));
                inventory.setItem(slot, ItemStack.EMPTY);
            }
        }

        if (!saved.isEmpty()) {
            player.setData(OrehallAttachments.SAVED_RECOVERY_COMPASSES, saved);
        }
    }

    private void onLivingDeathPost(LivingDeathEvent event) {
        if (!isPersistOnDeathEnabled()) {
            return;
        }

        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        // If death was cancelled, immediately put the compasses back into the player's inventory
        if (event.isCanceled()) {
            restoreCompasses(player, player);
        }
    }

    private void onPlayerClone(PlayerEvent.Clone event) {
        if (!isPersistOnDeathEnabled() || !event.isWasDeath()) {
            return;
        }

        Player newPlayer = event.getEntity();
        Player originalPlayer = event.getOriginal();
        restoreCompasses(newPlayer, originalPlayer);
    }

    private void restoreCompasses(Player targetPlayer, Player sourcePlayer) {
        List<SavedRecoveryCompass> saved = sourcePlayer.getData(OrehallAttachments.SAVED_RECOVERY_COMPASSES);
        if (saved == null || saved.isEmpty()) {
            // Also check targetPlayer in case copyOnDeath already transferred it
            saved = targetPlayer.getData(OrehallAttachments.SAVED_RECOVERY_COMPASSES);
        }

        if (saved != null && !saved.isEmpty()) {
            List<SavedRecoveryCompass> toRestore = new ArrayList<>(saved);
            targetPlayer.removeData(OrehallAttachments.SAVED_RECOVERY_COMPASSES);
            sourcePlayer.removeData(OrehallAttachments.SAVED_RECOVERY_COMPASSES);

            Inventory inventory = targetPlayer.getInventory();
            for (SavedRecoveryCompass entry : toRestore) {
                int slot = entry.slot();
                ItemStack stack = entry.stack().copy();
                if (slot >= 0 && slot < inventory.getContainerSize() && inventory.getItem(slot).isEmpty()) {
                    inventory.setItem(slot, stack);
                } else {
                    if (!inventory.add(stack) || !stack.isEmpty()) {
                        targetPlayer.drop(stack, false);
                    }
                }
            }

            if (targetPlayer instanceof ServerPlayer serverPlayer) {
                serverPlayer.inventoryMenu.broadcastChanges();
            }
        }
    }
}
