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
    private ModConfigSpec.BooleanValue allowWalkingOnRailsAndSpecialBlocksConfig;
    private ModConfigSpec.BooleanValue enablePathfindingBackoffConfig;
    private ModConfigSpec.IntValue sensingCacheIntervalConfig;
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

        allowWalkingOnRailsAndSpecialBlocksConfig = builder
                .comment("Allows mobs to freely pathfind and walk across rails, lily pads, dripleaves, and leaves instead of treating them as impassable barriers.")
                .translation("onetoys.configuration.gameplay_tweaks.mob_tweaks.ai_tweaks.allow_walking_on_rails_and_special_blocks")
                .define("allow_walking_on_rails_and_special_blocks", true);

        enablePathfindingBackoffConfig = builder
                .comment("Enables dynamic backoff penalties when A* pathfinding fails repeatedly against unreachable targets (MC-17630, MC-182325) and allows direct movement when already close without invoking A*.")
                .translation("onetoys.configuration.gameplay_tweaks.mob_tweaks.ai_tweaks.enable_pathfinding_backoff")
                .define("enable_pathfinding_backoff", true);

        sensingCacheIntervalConfig = builder
                .comment("Interval in ticks between Sensing line-of-sight cache flushes (1 = vanilla behavior every tick, 4 = reduces raycast overhead by 75%).")
                .translation("onetoys.configuration.gameplay_tweaks.mob_tweaks.ai_tweaks.sensing_cache_interval")
                .defineInRange("sensing_cache_interval", 4, 1, 20);

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

    public static boolean isAllowWalkingOnRailsAndSpecialBlocks() {
        return isTweakEnabled() && (instance.allowWalkingOnRailsAndSpecialBlocksConfig == null || instance.allowWalkingOnRailsAndSpecialBlocksConfig.get());
    }

    public static boolean isEnablePathfindingBackoff() {
        return isTweakEnabled() && (instance.enablePathfindingBackoffConfig == null || instance.enablePathfindingBackoffConfig.get());
    }

    public static boolean isPathfindingBackoffEnabled() {
        return isEnablePathfindingBackoff();
    }

    public static int getSensingCacheInterval() {
        if (!isTweakEnabled()) {
            return 1;
        }
        return instance.sensingCacheIntervalConfig != null ? instance.sensingCacheIntervalConfig.get() : 4;
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
