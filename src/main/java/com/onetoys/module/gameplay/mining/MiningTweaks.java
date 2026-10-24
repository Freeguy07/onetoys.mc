package com.onetoys.module.gameplay.mining;

import com.onetoys.module.ModuleManager;
import com.onetoys.module.gameplay.GameplayTweaksModule;

import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.common.ModConfigSpec;

public class MiningTweaks {
    private ModConfigSpec.BooleanValue enabledConfig;
    private ModConfigSpec.BooleanValue disableMiningDelayConfig;
    private ModConfigSpec.BooleanValue applyToCreativeConfig;

    public void buildConfig(ModConfigSpec.Builder builder) {
        builder.comment("Tweaks and adjustments to block breaking and mining mechanics.")
                .translation("onetoys.configuration.gameplay_tweaks.mining_tweaks")
                .push("mining_tweaks");

        enabledConfig = builder
                .comment("Master switch to enable or disable the mining tweaks sub-category.")
                .translation("onetoys.configuration.gameplay_tweaks.mining_tweaks.enabled")
                .define("enabled", true);

        disableMiningDelayConfig = builder
                .comment("When enabled, removes the artificial 5-tick (0.25s) cooldown delay between breaking consecutive blocks in Survival mode.")
                .translation("onetoys.configuration.gameplay_tweaks.mining_tweaks.disable_mining_delay")
                .define("disable_mining_delay", true);

        applyToCreativeConfig = builder
                .comment("When enabled, also removes the 5-tick cooldown delay between breaking consecutive blocks in Creative mode (allowing continuous 20 blocks/sec breaking when holding left-click).")
                .translation("onetoys.configuration.gameplay_tweaks.mining_tweaks.apply_to_creative")
                .define("apply_to_creative", false);

        builder.pop();
    }

    public boolean isEnabled() {
        return enabledConfig == null || enabledConfig.get();
    }

    public boolean isDisableMiningDelay() {
        return isEnabled() && (disableMiningDelayConfig == null || disableMiningDelayConfig.get());
    }

    public boolean isApplyToCreative() {
        return isEnabled() && (applyToCreativeConfig != null && applyToCreativeConfig.get());
    }

    public static boolean shouldDisableMiningDelay(GameType mode) {
        if (!ModuleManager.get().isModuleEnabled(GameplayTweaksModule.ID)) {
            return false;
        }
        GameplayTweaksModule module = GameplayTweaksModule.getInstance();
        if (module == null || !module.isEnabled()) {
            return false;
        }
        MiningTweaks tweaks = module.getMiningTweaks();
        if (tweaks == null || !tweaks.isEnabled()) {
            return false;
        }
        if (mode == GameType.CREATIVE) {
            return tweaks.isApplyToCreative();
        }
        return tweaks.isDisableMiningDelay();
    }
}
