package com.onetoys.module.gameplay.weather;

import com.onetoys.module.gameplay.GameplayTweaksModule;
import net.minecraft.core.BlockPos;
import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Height-based weather tweaks.
 * Causes precipitation in biomes with downfall to transition to snow at high elevations,
 * copying Minecraft Bedrock's quirk where snow falls instead of rain if you're very high.
 */
public class HeightWeatherTweaks {
    private static HeightWeatherTweaks instance;

    private final GameplayTweaksModule parent;
    private ModConfigSpec.BooleanValue enabledConfig;
    private ModConfigSpec.IntValue snowHeightConfig;
    private ModConfigSpec.BooleanValue borderNoiseConfig;

    public HeightWeatherTweaks(GameplayTweaksModule parent) {
        this.parent = parent;
        instance = this;
    }

    public HeightWeatherTweaks() {
        this(null);
    }

    public void buildConfig(ModConfigSpec.Builder builder) {
        builder.comment("Tweaks for height-based weather transitions (e.g. Bedrock-style snow at high elevations).")
                .translation("onetoys.configuration.gameplay_tweaks.height_weather")
                .push("height_weather");

        enabledConfig = builder
                .comment("Master switch to enable snow falling instead of rain at high altitudes.")
                .translation("onetoys.configuration.gameplay_tweaks.height_weather.enabled")
                .define("enabled", true);

        snowHeightConfig = builder
                .comment("Y-level threshold where precipitation transitions from rain to snow (cloud level is 192).")
                .translation("onetoys.configuration.gameplay_tweaks.height_weather.snow_height")
                .defineInRange("snow_height", 192, -64, 320);

        borderNoiseConfig = builder
                .comment("Whether to apply subtle pseudo-random noise to the transition line (+/- 4 blocks)",
                        "so the boundary isn't a razor-flat plane across chunk boundaries.")
                .translation("onetoys.configuration.gameplay_tweaks.height_weather.border_noise")
                .define("border_noise", true);

        builder.pop();
    }

    public boolean isEnabled() {
        if (parent != null && !parent.isEnabled()) {
            return false;
        }
        return enabledConfig == null || enabledConfig.get();
    }

    public static boolean isFeatureEnabled() {
        return instance != null && instance.isEnabled();
    }

    public static int getSnowHeight() {
        return instance != null && instance.snowHeightConfig != null ? instance.snowHeightConfig.get() : 192;
    }

    public static boolean isBorderNoiseEnabled() {
        return instance != null && instance.borderNoiseConfig != null ? instance.borderNoiseConfig.get() : true;
    }

    public static boolean shouldSnowAt(BlockPos pos) {
        if (!isFeatureEnabled()) {
            return false;
        }
        int baseHeight = getSnowHeight();
        if (!isBorderNoiseEnabled()) {
            return pos.getY() >= baseHeight;
        }

        // Deterministic pseudo-random variation based on X and Z coordinates (-4 to +4)
        long hash = (long) pos.getX() * 3121L ^ (long) pos.getZ() * 418711L;
        hash = (hash ^ (hash >>> 16)) * 0x45d9f3bL;
        hash = hash ^ (hash >>> 16);
        int offset = (int) Math.floorMod(hash, 9L) - 4; // range [-4, +4]
        return pos.getY() >= (baseHeight + offset);
    }
}
