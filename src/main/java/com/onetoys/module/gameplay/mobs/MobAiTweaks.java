package com.onetoys.module.gameplay.mobs;

import com.onetoys.module.gameplay.GameplayTweaksModule;
import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Subsystem and configuration controller for mob AI bug fixes and TPS optimizations.
 */
public class MobAiTweaks {
    private static MobAiTweaks instance;

    private ModConfigSpec.BooleanValue enabledConfig;
    private ModConfigSpec.BooleanValue fixTargetLossAndCombatPauseConfig;
    private ModConfigSpec.BooleanValue fixLeapAttackAndHeadSnapConfig;
    private ModConfigSpec.BooleanValue fixSpinningAndStuckNavigationConfig;
    private ModConfigSpec.BooleanValue allowWalkingOnRailsConfig;
    private ModConfigSpec.BooleanValue enableGapJumpingConfig;
    private ModConfigSpec.BooleanValue allowWalkingOnSpecialBlocksConfig;
    private ModConfigSpec.BooleanValue reducePlayerNoSpawnRadiusConfig;
    private ModConfigSpec.DoubleValue playerNoSpawnDistanceConfig;
    private ModConfigSpec.BooleanValue limitBlockUpdatePathRecomputeConfig;
    private ModConfigSpec.BooleanValue throttleZombieEggScanConfig;
    private ModConfigSpec.BooleanValue enableDistanceAiThrottlingConfig;
    private ModConfigSpec.IntValue aiThrottleStartDistanceConfig;

    public MobAiTweaks() {
        instance = this;
    }

    public static MobAiTweaks get() {
        return instance;
    }

    public static MobAiTweaks getInstance() {
        return instance;
    }

    public void buildConfig(ModConfigSpec.Builder builder) {
        builder.comment("Tweaks, bug fixes, and TPS optimizations for vanilla mob AI.")
                .translation("onetoys.configuration.gameplay_tweaks.mob_tweaks.ai_tweaks")
                .push("ai_tweaks");

        enabledConfig = builder
                .comment("Master switch for mob AI tweaks, bug fixes, and TPS optimizations.")
                .translation("onetoys.configuration.gameplay_tweaks.mob_tweaks.ai_tweaks.enabled")
                .define("enabled", true);

        fixTargetLossAndCombatPauseConfig = builder
                .comment("Fixes mobs losing targets at close range, 1-second combat chase pauses (MC-198068), double attacks upon target switch (MC-191642), and target dimension mismatch (MC-149563).")
                .translation("onetoys.configuration.gameplay_tweaks.mob_tweaks.ai_tweaks.fix_target_loss_and_combat_pause")
                .define("fix_target_loss_and_combat_pause", true);

        fixLeapAttackAndHeadSnapConfig = builder
                .comment("Allows spiders and wolves to deal damage mid-air during leap attacks (MC-49129) and eliminates erratic head snapping during combat (MC-248707).")
                .translation("onetoys.configuration.gameplay_tweaks.mob_tweaks.ai_tweaks.fix_leap_attack_and_head_snap")
                .define("fix_leap_attack_and_head_snap", true);

        fixSpinningAndStuckNavigationConfig = builder
                .comment("Prevents mobs from spinning continuously on slabs, farmland, scaffolding (MC-157645) and in shallow water (MC-129293).")
                .translation("onetoys.configuration.gameplay_tweaks.mob_tweaks.ai_tweaks.fix_spinning_and_stuck_navigation")
                .define("fix_spinning_and_stuck_navigation", true);

        allowWalkingOnRailsConfig = builder
                .comment("Allows mobs to freely pathfind and walk across rails instead of treating them as impassable barriers.")
                .translation("onetoys.configuration.gameplay_tweaks.mob_tweaks.ai_tweaks.allow_walking_on_rails")
                .define("allow_walking_on_rails", true);

        enableGapJumpingConfig = builder
                .comment("Allows mobs to leap across 1-block empty spaces and gaps between platforms (parkour traversal).")
                .translation("onetoys.configuration.gameplay_tweaks.mob_tweaks.ai_tweaks.enable_gap_jumping")
                .define("enable_gap_jumping", true);

        allowWalkingOnSpecialBlocksConfig = builder
                .comment("Allows mobs to walk across leaves, lily pads, and big dripleaves.")
                .translation("onetoys.configuration.gameplay_tweaks.mob_tweaks.ai_tweaks.allow_walking_on_special_blocks")
                .define("allow_walking_on_special_blocks", true);

        reducePlayerNoSpawnRadiusConfig = builder
                .comment("Reduces the minimum no-spawn perimeter around players from vanilla's 24 blocks (576 blocks^2) to a tight 3x3 block perimeter.")
                .translation("onetoys.configuration.gameplay_tweaks.mob_tweaks.ai_tweaks.reduce_player_no_spawn_radius")
                .define("reduce_player_no_spawn_radius", true);

        playerNoSpawnDistanceConfig = builder
                .comment("Minimum spawn distance in blocks from players when reduced no-spawn radius is enabled (1.5 blocks = 3x3 block box).")
                .translation("onetoys.configuration.gameplay_tweaks.mob_tweaks.ai_tweaks.player_no_spawn_distance")
                .defineInRange("player_no_spawn_distance", 1.5D, 0.0D, 24.0D);

        limitBlockUpdatePathRecomputeConfig = builder
                .comment("Limits reactive path recalculations in ServerLevel#sendBlockUpdated to mobs within 8 blocks of the modified block.")
                .translation("onetoys.configuration.gameplay_tweaks.mob_tweaks.ai_tweaks.limit_block_update_path_recompute")
                .define("limit_block_update_path_recompute", true);

        throttleZombieEggScanConfig = builder
                .comment("Throttles heavy 3D block scanning in RemoveBlockGoal (zombie turtle egg targeting).")
                .translation("onetoys.configuration.gameplay_tweaks.mob_tweaks.ai_tweaks.throttle_zombie_egg_scan")
                .define("throttle_zombie_egg_scan", true);

        enableDistanceAiThrottlingConfig = builder
                .comment("Dynamically reduces goalSelector and targetSelector tick rates (DAB) for distant mobs out of combat.")
                .translation("onetoys.configuration.gameplay_tweaks.mob_tweaks.ai_tweaks.enable_distance_ai_throttling")
                .define("enable_distance_ai_throttling", true);

        aiThrottleStartDistanceConfig = builder
                .comment("Distance in blocks to the nearest player at which dynamic AI throttling begins.")
                .translation("onetoys.configuration.gameplay_tweaks.mob_tweaks.ai_tweaks.ai_throttle_start_distance")
                .defineInRange("ai_throttle_start_distance", 32, 16, 128);

        builder.pop();
    }

    public boolean isEnabled() {
        return enabledConfig == null || enabledConfig.get();
    }

    public static boolean isTweakEnabled() {
        return GameplayTweaksModule.getInstance() != null
                && GameplayTweaksModule.getInstance().isEnabled()
                && GameplayTweaksModule.getInstance().getMobTweaks().isEnabled()
                && instance != null
                && instance.isEnabled();
    }

    public static boolean isFixTargetLossAndCombatPause() {
        return isTweakEnabled() && (instance.fixTargetLossAndCombatPauseConfig == null || instance.fixTargetLossAndCombatPauseConfig.get());
    }

    public static boolean isFixLeapAttackAndHeadSnap() {
        return isTweakEnabled() && (instance.fixLeapAttackAndHeadSnapConfig == null || instance.fixLeapAttackAndHeadSnapConfig.get());
    }

    public static boolean isFixSpinningAndStuckNavigation() {
        return isTweakEnabled() && (instance.fixSpinningAndStuckNavigationConfig == null || instance.fixSpinningAndStuckNavigationConfig.get());
    }

    public static boolean isAllowWalkingOnRails() {
        return isTweakEnabled() && (instance.allowWalkingOnRailsConfig == null || instance.allowWalkingOnRailsConfig.get());
    }

    public static boolean isEnableGapJumping() {
        return isTweakEnabled() && (instance.enableGapJumpingConfig == null || instance.enableGapJumpingConfig.get());
    }

    public static boolean isAllowWalkingOnSpecialBlocks() {
        return isTweakEnabled() && (instance.allowWalkingOnSpecialBlocksConfig == null || instance.allowWalkingOnSpecialBlocksConfig.get());
    }

    public static boolean isReducePlayerNoSpawnRadius() {
        return isTweakEnabled() && (instance.reducePlayerNoSpawnRadiusConfig == null || instance.reducePlayerNoSpawnRadiusConfig.get());
    }

    public static double getPlayerNoSpawnDistanceSquared() {
        if (!isReducePlayerNoSpawnRadius()) {
            return 576.0D;
        }
        double dist = instance.playerNoSpawnDistanceConfig != null ? instance.playerNoSpawnDistanceConfig.get() : 1.5D;
        return dist * dist;
    }

    public static boolean isLimitBlockUpdatePathRecompute() {
        return isTweakEnabled() && (instance.limitBlockUpdatePathRecomputeConfig == null || instance.limitBlockUpdatePathRecomputeConfig.get());
    }

    public static boolean isThrottleZombieEggScan() {
        return isTweakEnabled() && (instance.throttleZombieEggScanConfig == null || instance.throttleZombieEggScanConfig.get());
    }

    public static boolean isEnableDistanceAiThrottling() {
        return isTweakEnabled() && (instance.enableDistanceAiThrottlingConfig == null || instance.enableDistanceAiThrottlingConfig.get());
    }

    public static boolean isDistanceAiThrottlingEnabled() {
        return isEnableDistanceAiThrottling();
    }

    public static int getAiThrottleStartDistance() {
        if (!isTweakEnabled()) {
            return 32;
        }
        return instance.aiThrottleStartDistanceConfig != null ? instance.aiThrottleStartDistanceConfig.get() : 32;
    }
}
