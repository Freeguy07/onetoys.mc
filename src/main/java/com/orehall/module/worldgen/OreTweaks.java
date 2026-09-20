package com.orehall.module.worldgen;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Controller and configuration coordinator for ore generation tweaks.
 * Manages the ore_tweaks subcategory under worldgen.
 */
public class OreTweaks {
    private final GiantOreVeinsDisabler giantOreVeinsDisabler = new GiantOreVeinsDisabler(this);
    private ModConfigSpec.BooleanValue enabledConfig;
    private ModConfigSpec.BooleanValue netherLockedLapisConfig;

    public void buildConfig(ModConfigSpec.Builder builder) {
        builder.comment("Tweaks and adjustments to ore generation.")
                .translation("orehall.configuration.worldgen.ore_tweaks")
                .push("ore_tweaks");

        enabledConfig = builder
                .comment("Master switch to enable or disable all ore generation tweaks.")
                .translation("orehall.configuration.worldgen.ore_tweaks.enabled")
                .define("enabled", true);

        netherLockedLapisConfig = builder
                .comment("When enabled, vanilla lapis ore is prevented from spawning in the Overworld,",
                        "and instead spawns exclusively in the Nether as Nether Lapis Ore.")
                .translation("orehall.configuration.worldgen.ore_tweaks.nether_locked_lapis")
                .define("nether_locked_lapis", true);

        giantOreVeinsDisabler.buildConfig(builder);

        builder.pop();
    }

    public boolean isEnabled() {
        return enabledConfig == null || enabledConfig.get();
    }

    public boolean isNetherLockedLapisEnabled() {
        return isEnabled() && (netherLockedLapisConfig == null || netherLockedLapisConfig.get());
    }

    public GiantOreVeinsDisabler getGiantOreVeinsDisabler() {
        return giantOreVeinsDisabler;
    }
}
