package com.onetoys.module.gameplay.sleep;

import com.onetoys.module.ModuleManager;
import com.onetoys.module.gameplay.GameplayTweaksModule;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerSetSpawnEvent;

public class SleepingTweaks {
    private ModConfigSpec.BooleanValue enabledConfig;
    private ModConfigSpec.BooleanValue bedsDontSetSpawnPointConfig;
    private final RespawnAnchorTweaks respawnAnchorTweaks = new RespawnAnchorTweaks();

    public void buildConfig(ModConfigSpec.Builder builder) {
        builder.comment("Tweaks and adjustments to sleeping and beds.")
                .translation("onetoys.configuration.gameplay_tweaks.sleeping_tweaks")
                .push("sleeping_tweaks");

        enabledConfig = builder
                .comment("Master switch to enable or disable the sleeping tweaks sub-category.")
                .translation("onetoys.configuration.gameplay_tweaks.sleeping_tweaks.enabled")
                .define("enabled", true);

        bedsDontSetSpawnPointConfig = builder
                .comment("When enabled, sleeping in or clicking on beds will not set or override the player's respawn point.",
                        "Forced spawn points (e.g. /spawnpoint command) and respawn anchors remain unaffected.")
                .translation("onetoys.configuration.gameplay_tweaks.sleeping_tweaks.beds_dont_set_spawn_point")
                .define("beds_dont_set_spawn_point", true);

        respawnAnchorTweaks.buildConfig(builder);

        builder.pop();
    }

    public RespawnAnchorTweaks getRespawnAnchorTweaks() {
        return respawnAnchorTweaks;
    }

    public void onCommonSetup(FMLCommonSetupEvent event) {
        respawnAnchorTweaks.onCommonSetup(event);
    }

    public boolean isEnabled() {
        return enabledConfig == null || enabledConfig.get();
    }

    public boolean isBedsDontSetSpawnPointEnabled() {
        if (!ModuleManager.get().isModuleEnabled(GameplayTweaksModule.ID)) {
            return false;
        }
        return isEnabled() && (bedsDontSetSpawnPointConfig == null || bedsDontSetSpawnPointConfig.get());
    }

    public void onConstruct(IEventBus modBus) {
        NeoForge.EVENT_BUS.addListener(this::onPlayerSetSpawn);
    }

    private void onPlayerSetSpawn(PlayerSetSpawnEvent event) {
        if (!isBedsDontSetSpawnPointEnabled()) {
            return;
        }

        // Do not cancel forced spawnpoints (e.g. commands like /spawnpoint)
        if (event.isForced()) {
            return;
        }

        BlockPos newSpawn = event.getNewSpawn();
        if (newSpawn == null) {
            return;
        }

        if (!(event.getEntity() instanceof ServerPlayer serverPlayer)) {
            return;
        }

        Level level = serverPlayer.level();
        Level targetLevel = level.getServer() != null
                ? level.getServer().getLevel(event.getSpawnLevel())
                : (level.dimension().equals(event.getSpawnLevel()) ? level : null);

        if (targetLevel != null && targetLevel.isLoaded(newSpawn)) {
            BlockState state = targetLevel.getBlockState(newSpawn);
            if (state.is(BlockTags.BEDS)
                    || state.getBlock() instanceof BedBlock
                    || state.getBlock().isBed(state, targetLevel, newSpawn, serverPlayer)) {
                event.setCanceled(true);
            }
        }
    }
}
