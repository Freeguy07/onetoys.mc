package com.onetoys.module.content;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Controller and configuration coordinator for Tungsten Tweaks.
 * Manages the tungsten_tweaks subcategory under the content module.
 */
public class TungstenTweaks {
    private final ContentModule parent;
    private ModConfigSpec.BooleanValue enabledConfig;
    private ModConfigSpec.BooleanValue oreGenerationConfig;
    private ModConfigSpec.BooleanValue calciteStripesConfig;
    private ModConfigSpec.BooleanValue dumbbellEnabledConfig;
    private ModConfigSpec.DoubleValue dumbbellGravityConfig;
    private ModConfigSpec.BooleanValue wateringCanEnabledConfig;

    public TungstenTweaks(ContentModule parent) {
        this.parent = parent;
    }

    public void buildConfig(ModConfigSpec.Builder builder) {
        builder.comment("Tweaks and additions for mod content and enhancements.")
                .translation("onetoys.configuration.content.tungsten_tweaks")
                .push("tungsten_tweaks");

        enabledConfig = builder
                .comment("Master switch to enable or disable all features in the Tungsten Tweaks category.")
                .translation("onetoys.configuration.content.tungsten_tweaks.enabled")
                .define("enabled", true);

        oreGenerationConfig = builder
                .comment("When enabled, tungsten ore generates strictly replacing calcite in the Overworld (geodes, stony peaks) and Nether.")
                .translation("onetoys.configuration.content.tungsten_tweaks.ore_generation")
                .define("ore_generation", true);

        calciteStripesConfig = builder
                .comment("When enabled, rare stratified geological blobs composed of layered discs (calcite, gravel, and tuff) generate throughout Nether terrain.")
                .translation("onetoys.configuration.content.tungsten_tweaks.calcite_stripes")
                .define("calcite_stripes", true);

        dumbbellEnabledConfig = builder
                .comment("When enabled, adds the Dumbbell item and placeable falling block.")
                .translation("onetoys.configuration.content.tungsten_tweaks.dumbbell")
                .define("dumbbell", true);

        dumbbellGravityConfig = builder
                .comment("Gravity multiplier added when holding a dumbbell (1.5 = +150% gravity, making gravity 2.5x base).")
                .translation("onetoys.configuration.content.tungsten_tweaks.dumbbell_gravity")
                .defineInRange("dumbbell_gravity", 1.5, 0.0, 10.0);

        wateringCanEnabledConfig = builder
                .comment("When enabled, adds the Watering Can item that simulates rain on blocks.")
                .translation("onetoys.configuration.content.tungsten_tweaks.watering_can")
                .define("watering_can", true);

        builder.pop();
    }

    public boolean isEnabled() {
        return parent.isEnabled() && (enabledConfig == null || enabledConfig.get());
    }

    public boolean isOreGenerationEnabled() {
        return isEnabled() && (oreGenerationConfig == null || oreGenerationConfig.get());
    }

    public boolean isCalciteStripesEnabled() {
        return isEnabled() && (calciteStripesConfig == null || calciteStripesConfig.get());
    }

    public boolean isDumbbellEnabled() {
        return isEnabled() && (dumbbellEnabledConfig == null || dumbbellEnabledConfig.get());
    }

    public double getDumbbellGravity() {
        return dumbbellGravityConfig == null ? 1.5 : dumbbellGravityConfig.get();
    }

    public boolean isWateringCanEnabled() {
        return isEnabled() && (wateringCanEnabledConfig == null || wateringCanEnabledConfig.get());
    }
}
