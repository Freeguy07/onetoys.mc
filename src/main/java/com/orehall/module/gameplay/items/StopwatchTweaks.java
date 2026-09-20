package com.orehall.module.gameplay.items;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class StopwatchTweaks {
    private static StopwatchTweaks instance;

    private final ItemTweaks parent;
    private ModConfigSpec.BooleanValue enabledConfig;
    private ModConfigSpec.BooleanValue hotbarDisplayConfig;

    public StopwatchTweaks(ItemTweaks parent) {
        this.parent = parent;
        instance = this;
    }

    public static StopwatchTweaks get() {
        return instance;
    }

    public void buildConfig(ModConfigSpec.Builder builder) {
        builder.comment("Features and adjustments for the Stopwatch item.")
                .translation("orehall.configuration.gameplay_tweaks.item_tweaks.stopwatch")
                .push("stopwatch");

        enabledConfig = builder
                .comment("Enable the Stopwatch item and its recipe.")
                .translation("orehall.configuration.gameplay_tweaks.item_tweaks.stopwatch.enabled")
                .define("enabled", true);

        hotbarDisplayConfig = builder
                .comment("When enabled, holding an active or paused stopwatch displays its live dynamic timer above the hotbar.")
                .translation("orehall.configuration.gameplay_tweaks.item_tweaks.stopwatch.hotbar_display")
                .define("hotbar_display", true);

        builder.pop();
    }

    public boolean isEnabled() {
        return enabledConfig == null || enabledConfig.get();
    }

    public boolean isHotbarDisplayEnabled() {
        return isEnabled() && (hotbarDisplayConfig == null || hotbarDisplayConfig.get());
    }
}
