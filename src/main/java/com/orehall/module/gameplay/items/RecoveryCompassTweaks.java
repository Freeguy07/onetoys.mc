package com.orehall.module.gameplay.items;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.orehall.module.ModuleManager;
import com.orehall.module.gameplay.GameplayTweaksModule;
import com.orehall.registry.OrehallAttachments;

import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentEffectComponents;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

public class RecoveryCompassTweaks {
    private static RecoveryCompassTweaks instance;

    private final ItemTweaks itemTweaks;
    private ModConfigSpec.BooleanValue enabledConfig;
    private ModConfigSpec.BooleanValue persistOnDeathConfig;
    private ModConfigSpec.BooleanValue deathCoordinatesTooltipConfig;
    private ModConfigSpec.BooleanValue deathCoordinatesHotbarDisplayConfig;

    public RecoveryCompassTweaks(ItemTweaks itemTweaks) {
        this.itemTweaks = itemTweaks;
        instance = this;
    }

    public RecoveryCompassTweaks() {
        this(null);
    }

    public static RecoveryCompassTweaks get() {
        return instance;
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

        deathCoordinatesTooltipConfig = builder
                .comment("When enabled, holding Shift while hovering over a recovery compass displays the coordinates and dimension of your last death.")
                .translation("orehall.configuration.gameplay_tweaks.item_tweaks.recovery_compass.death_coordinates_tooltip")
                .define("death_coordinates_tooltip", true);

        deathCoordinatesHotbarDisplayConfig = builder
                .comment("When enabled, right-clicking with a recovery compass in hand displays your last death coordinates above the hotbar.")
                .translation("orehall.configuration.gameplay_tweaks.item_tweaks.recovery_compass.death_coordinates_hotbar_display")
                .define("death_coordinates_hotbar_display", true);

        builder.pop();
    }

    public boolean isEnabled() {
        return enabledConfig == null || enabledConfig.get();
    }

    public static boolean isTweakEnabled() {
        if (!ModuleManager.get().isModuleEnabled(GameplayTweaksModule.ID)) {
            return false;
        }
        RecoveryCompassTweaks tweaks = instance;
        if (tweaks == null) {
            return false;
        }
        if (tweaks.itemTweaks != null && !tweaks.itemTweaks.isEnabled()) {
            return false;
        }
        return tweaks.isEnabled();
    }

    public boolean isPersistOnDeathEnabled() {
        return isTweakEnabled() && (persistOnDeathConfig == null || persistOnDeathConfig.get());
    }

    public boolean isTooltipEnabled() {
        return isTweakEnabled() && (deathCoordinatesTooltipConfig == null || deathCoordinatesTooltipConfig.get());
    }

    public boolean isHotbarDisplayEnabled() {
        return isTweakEnabled() && (deathCoordinatesHotbarDisplayConfig == null || deathCoordinatesHotbarDisplayConfig.get());
    }

    public static Component getDimensionName(ResourceKey<Level> dimension) {
        if (dimension.equals(Level.OVERWORLD)) {
            return Component.translatable("orehall.dimension.overworld");
        } else if (dimension.equals(Level.NETHER)) {
            return Component.translatable("orehall.dimension.the_nether");
        } else if (dimension.equals(Level.END)) {
            return Component.translatable("orehall.dimension.the_end");
        } else {
            String path = dimension.location().getPath();
            String formatted = Arrays.stream(path.split("_"))
                    .map(word -> word.isEmpty() ? "" : Character.toUpperCase(word.charAt(0)) + word.substring(1))
                    .collect(Collectors.joining(" "));
            return Component.literal(formatted);
        }
    }

    public void onConstruct(IEventBus modBus) {
        // Intercept death at HIGHEST priority to extract recovery compasses before gravestone mods or dropAll() can touch them
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGHEST, this::onLivingDeathPre);
        // Intercept at LOWEST priority to restore compasses if death was cancelled by another mod/totem
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, true, this::onLivingDeathPost);
        // Restore compasses to the cloned player upon respawning
        NeoForge.EVENT_BUS.addListener(this::onPlayerClone);
        // Right-click actionbar display
        NeoForge.EVENT_BUS.addListener(this::onRightClickItem);
    }

    private void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (!isHotbarDisplayEnabled()) {
            return;
        }

        ItemStack stack = event.getItemStack();
        if (!stack.is(Items.RECOVERY_COMPASS)) {
            return;
        }

        Player player = event.getEntity();
        Level level = event.getLevel();

        if (!level.isClientSide()) {
            boolean current = player.getData(OrehallAttachments.RECOVERY_COMPASS_OVERLAY);
            boolean next = !current;
            player.setData(OrehallAttachments.RECOVERY_COMPASS_OVERLAY, next);

            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.UI_BUTTON_CLICK, SoundSource.PLAYERS, 0.4F, 1.5F);

            if (next) {
                Optional<GlobalPos> deathLoc = player.getLastDeathLocation();
                Component message;
                if (deathLoc.isPresent()) {
                    GlobalPos pos = deathLoc.get();
                    BlockPos blockPos = pos.pos();
                    Component dimName = getDimensionName(pos.dimension());
                    message = Component.translatable(
                            "orehall.actionbar.recovery_compass.last_death",
                            blockPos.getX(), blockPos.getY(), blockPos.getZ(), dimName
                    );
                } else {
                    message = Component.translatable("orehall.actionbar.recovery_compass.no_death");
                }
                player.displayClientMessage(message, true);
            } else {
                player.displayClientMessage(Component.translatable("orehall.actionbar.recovery_compass.overlay_disabled"), true);
            }
        }

        event.setCancellationResult(InteractionResult.sidedSuccess(level.isClientSide()));
        event.setCanceled(true);
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
