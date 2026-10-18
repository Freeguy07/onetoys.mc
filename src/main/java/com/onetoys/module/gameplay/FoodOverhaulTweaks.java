package com.onetoys.module.gameplay;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Controller and configuration coordinator for Food & Survival overhaul mechanics.
 * Manages the food_overhaul subcategory under the gameplay_tweaks module.
 */
public class FoodOverhaulTweaks {
    private final GameplayTweaksModule parent;
    private ModConfigSpec.BooleanValue enabledConfig;

    public FoodOverhaulTweaks(GameplayTweaksModule parent) {
        this.parent = parent;
    }

    public void buildConfig(ModConfigSpec.Builder builder) {
        builder.comment("Valheim-inspired food and survival overhaul mechanics.")
                .translation("onetoys.configuration.gameplay_tweaks.food_overhaul")
                .push("food_overhaul");

        enabledConfig = builder
                .comment("Master switch to enable or disable all features in the Food & Survival category.")
                .translation("onetoys.configuration.gameplay_tweaks.food_overhaul.enabled")
                .define("enabled", true);

        com.onetoys.sol_valheim.ModConfig.buildConfig(builder);

        builder.pop();
    }

    public boolean isEnabled() {
        return parent.isEnabled() && (enabledConfig == null || enabledConfig.get());
    }
}
