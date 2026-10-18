package com.onetoys.module.worldgen;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Controller and configuration coordinator for altitude surface skin.
 */
public class TerrainTweaks {
    private ModConfigSpec.BooleanValue enabledConfig;

    public void buildConfig(ModConfigSpec.Builder builder) {
        builder.comment("Tweaks for altitude surface skin.")
                .translation("onetoys.configuration.worldgen.terrain_tweaks")
                .push("terrain_tweaks");

        enabledConfig = builder
                .comment("Gates only the altitude surface skin (density-function terrain is data-driven and unaffected).")
                .translation("onetoys.configuration.worldgen.terrain_tweaks.enabled")
                .define("enabled", true);

        builder.pop();
    }

    public boolean isEnabled() {
        return enabledConfig == null || enabledConfig.get();
    }
}
