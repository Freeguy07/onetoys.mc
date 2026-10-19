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
    private ModConfigSpec.IntValue transitionRangeConfig;

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

        transitionRangeConfig = builder
                .comment("Vertical blend range in blocks (+/-) around the transition line for smooth fading between rain and snow.",
                        "Set to 0 for a sharp cutoff without vertical blending.")
                .translation("onetoys.configuration.gameplay_tweaks.height_weather.transition_range")
                .defineInRange("transition_range", 6, 0, 32);

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

    public static int getTransitionRange() {
        return instance != null && instance.transitionRangeConfig != null ? instance.transitionRangeConfig.get() : 6;
    }

    private static double hash2D(int x, int z) {
        long n = (long) x * 374761393L + (long) z * 668265263L;
        n = (n ^ (n >>> 13)) * 1274126177L;
        return (double) ((n ^ (n >>> 16)) & 0x7FFFFFFFL) / (double) 0x7FFFFFFFL;
    }

    /**
     * 2D continuous smooth value noise with quintic interpolation (~25 blocks wavelength).
     * Returns a normalized value in [0.0, 1.0].
     */
    public static double sampleNoise(double x, double z) {
        double nx = x * 0.04;
        double nz = z * 0.04;
        int x0 = (int) Math.floor(nx);
        int z0 = (int) Math.floor(nz);
        int x1 = x0 + 1;
        int z1 = z0 + 1;

        double fx = nx - x0;
        double fz = nz - z0;

        // Smooth quintic curve: 6t^5 - 15t^4 + 10t^3
        double sx = fx * fx * fx * (fx * (fx * 6.0 - 15.0) + 10.0);
        double sz = fz * fz * fz * (fz * (fz * 6.0 - 15.0) + 10.0);

        double v00 = hash2D(x0, z0);
        double v10 = hash2D(x1, z0);
        double v01 = hash2D(x0, z1);
        double v11 = hash2D(x1, z1);

        double top = v00 + sx * (v10 - v00);
        double bot = v01 + sx * (v11 - v01);
        return top + sz * (bot - top);
    }

    /**
     * Calculates the exact snow line Y-coordinate at the given world X and Z coordinates.
     */
    public static double getSnowThresholdAt(double x, double z) {
        double base = getSnowHeight();
        if (!isBorderNoiseEnabled()) {
            return base;
        }
        // Continuous smooth variation in [-4.0, +4.0] blocks
        return base + (sampleNoise(x, z) * 8.0 - 4.0);
    }

    public static boolean shouldSnowAt(BlockPos pos) {
        if (!isFeatureEnabled()) {
            return false;
        }
        return pos.getY() >= getSnowThresholdAt((double) pos.getX() + 0.5, (double) pos.getZ() + 0.5);
    }

    /**
     * Computes the alpha blend factor for rain at height y.
     * Returns 1.0 below (snowThreshold - blend), fades smoothly to 0.0 at (snowThreshold + blend).
     */
    public static float getRainBlendFactor(double y, double snowThreshold, int blend) {
        if (blend <= 0) {
            return y <= snowThreshold ? 1.0F : 0.0F;
        }
        double lower = snowThreshold - blend;
        double upper = snowThreshold + blend;
        if (y <= lower) {
            return 1.0F;
        }
        if (y >= upper) {
            return 0.0F;
        }
        double t = 1.0 - (y - lower) / (upper - lower);
        return (float) (t * t * (3.0 - 2.0 * t));
    }

    /**
     * Computes the alpha blend factor for snow at height y.
     * Returns 0.0 below (snowThreshold - blend), fades smoothly to 1.0 at (snowThreshold + blend).
     */
    public static float getSnowBlendFactor(double y, double snowThreshold, int blend) {
        if (blend <= 0) {
            return y >= snowThreshold ? 1.0F : 0.0F;
        }
        double lower = snowThreshold - blend;
        double upper = snowThreshold + blend;
        if (y <= lower) {
            return 0.0F;
        }
        if (y >= upper) {
            return 1.0F;
        }
        double t = (y - lower) / (upper - lower);
        return (float) (t * t * (3.0 - 2.0 * t));
    }
}
