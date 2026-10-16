package com.onetoys.module.worldgen;

import net.neoforged.neoforge.common.ModConfigSpec;

public class GiantOreVeinsDisabler {
    private final OreTweaks oreTweaks;
    private ModConfigSpec.BooleanValue enabledConfig;
    private ModConfigSpec.BooleanValue disableCopperVeinsConfig;
    private ModConfigSpec.BooleanValue disableIronVeinsConfig;

    public GiantOreVeinsDisabler(OreTweaks oreTweaks) {
        this.oreTweaks = oreTweaks;
    }

    public GiantOreVeinsDisabler() {
        this(null);
    }

    public void buildConfig(ModConfigSpec.Builder builder) {
        builder.comment("Disabler for vanilla giant copper and iron ore veins.")
                .translation("onetoys.configuration.worldgen.ore_tweaks.giant_ore_veins_disabler")
                .push("giant_ore_veins_disabler");

        enabledConfig = builder
                .comment("Enable the giant ore veins disabler sub-category.")
                .translation("onetoys.configuration.worldgen.ore_tweaks.giant_ore_veins_disabler.enabled")
                .define("enabled", true);

        disableCopperVeinsConfig = builder
                .comment("When enabled, giant copper ore veins (and their granite deposits) are prevented from generating in world generation.",
                        "Standard ore blob deposits (e.g. ore_copper_small, ore_copper_large) are untouched.")
                .translation("onetoys.configuration.worldgen.ore_tweaks.giant_ore_veins_disabler.disable_copper_veins")
                .define("disable_copper_veins", true);

        disableIronVeinsConfig = builder
                .comment("When enabled, giant iron ore veins (and their tuff deposits) are prevented from generating in world generation.",
                        "Standard ore blob deposits (e.g. ore_iron_middle, ore_iron_small) are untouched.")
                .translation("onetoys.configuration.worldgen.ore_tweaks.giant_ore_veins_disabler.disable_iron_veins")
                .define("disable_iron_veins", true);

        builder.pop();
    }

    public boolean isEnabled() {
        if (oreTweaks != null && !oreTweaks.isEnabled()) {
            return false;
        }
        return enabledConfig == null || enabledConfig.get();
    }

    public boolean isCopperDisabled() {
        return isEnabled() && (disableCopperVeinsConfig == null || disableCopperVeinsConfig.get());
    }

    public boolean isIronDisabled() {
        return isEnabled() && (disableIronVeinsConfig == null || disableIronVeinsConfig.get());
    }

    public boolean isBothDisabled() {
        return isEnabled()
                && (disableCopperVeinsConfig == null || disableCopperVeinsConfig.get())
                && (disableIronVeinsConfig == null || disableIronVeinsConfig.get());
    }
}
