package com.onetoys.module.integration.caverns_and_chasms;

import com.onetoys.module.integration.ModIntegrationModule;
import net.neoforged.neoforge.common.ModConfigSpec;

public class CavernsAndChasmsIntegration {
    private final ModIntegrationModule parent;
    private ModConfigSpec.BooleanValue enabledConfig;
    private ModConfigSpec.BooleanValue balancedCopperGearConfig;

    public CavernsAndChasmsIntegration(ModIntegrationModule parent) {
        this.parent = parent;
    }

    public void buildConfig(ModConfigSpec.Builder builder) {
        builder.comment("Integrations and adjustments for the 'Caverns and Chasms' mod.")
                .translation("onetoys.configuration.mod_integration.caverns_and_chasms")
                .push("caverns_and_chasms");

        enabledConfig = builder
                .comment("Master switch to enable or disable Caverns and Chasms integrations.")
                .translation("onetoys.configuration.mod_integration.caverns_and_chasms.enabled")
                .define("enabled", true);

        balancedCopperGearConfig = builder
                .comment("When enabled, copper tools from Caverns & Chasms are converted from iron tier to stone tier, allowing them to mine and drop only what stone tools can.",
                        "Default: true")
                .translation("onetoys.configuration.mod_integration.caverns_and_chasms.balanced_copper_gear")
                .define("balanced_copper_gear", true);

        builder.pop();
    }

    public boolean isEnabled() {
        return enabledConfig == null || enabledConfig.get();
    }

    public boolean isBalancedCopperGearEnabled() {
        return isEnabled() && (balancedCopperGearConfig == null || balancedCopperGearConfig.get());
    }
}
