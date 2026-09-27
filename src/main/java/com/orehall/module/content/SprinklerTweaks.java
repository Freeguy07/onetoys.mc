package com.orehall.module.content;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Controller and configuration coordinator for the Sprinkler block.
 * Manages the sprinkler subcategory under the content module.
 */
public class SprinklerTweaks {
    private final ContentModule parent;
    private ModConfigSpec.BooleanValue enabledConfig;
    private ModConfigSpec.BooleanValue continuousIrrigationConfig;
    private ModConfigSpec.IntValue irrigationIntervalTicksConfig;
    private ModConfigSpec.BooleanValue requireWaterConfig;
    private ModConfigSpec.DoubleValue cropGrowthChanceConfig;

    public SprinklerTweaks(ContentModule parent) {
        this.parent = parent;
    }

    public void buildConfig(ModConfigSpec.Builder builder) {
        builder.comment("Subcategory description for the redstone-powered Sprinkler block.")
                .translation("orehall.configuration.content.sprinkler")
                .push("sprinkler");

        enabledConfig = builder
                .comment("Master switch to enable or disable the Sprinkler block and its ballistic watering mechanic.")
                .translation("orehall.configuration.content.sprinkler.enabled")
                .define("enabled", true);

        continuousIrrigationConfig = builder
                .comment("When enabled, the sprinkler periodically irrigates surrounding crops while continuously powered by redstone.")
                .translation("orehall.configuration.content.sprinkler.continuous_irrigation")
                .define("continuous_irrigation", true);

        irrigationIntervalTicksConfig = builder
                .comment("Delay in ticks (20 ticks = 1 second) between irrigation cycles when continuously powered by redstone.")
                .translation("orehall.configuration.content.sprinkler.irrigation_interval_ticks")
                .defineInRange("irrigation_interval_ticks", 100, 20, 1200);

        requireWaterConfig = builder
                .comment("When enabled, the sprinkler requires water (waterlogged, adjacent water block, or water cauldron) to spray.")
                .translation("orehall.configuration.content.sprinkler.require_water")
                .define("require_water", true);

        cropGrowthChanceConfig = builder
                .comment("Probability (0.0 to 1.0) that a landing water droplet triggers an accelerated growth tick on crops.")
                .translation("orehall.configuration.content.sprinkler.crop_growth_chance")
                .defineInRange("crop_growth_chance", 0.5, 0.0, 1.0);

        builder.pop();
    }

    public boolean isEnabled() {
        return parent.isEnabled() && (enabledConfig == null || enabledConfig.get());
    }

    public boolean isContinuousIrrigation() {
        return continuousIrrigationConfig == null || continuousIrrigationConfig.get();
    }

    public int getIrrigationIntervalTicks() {
        return irrigationIntervalTicksConfig != null ? irrigationIntervalTicksConfig.get() : 100;
    }

    public boolean isRequireWater() {
        return requireWaterConfig != null && requireWaterConfig.get();
    }

    public double getCropGrowthChance() {
        return cropGrowthChanceConfig != null ? cropGrowthChanceConfig.get() : 0.5;
    }

    public void onConstruct(IEventBus modBus) {
    }

    public void onCommonSetup(FMLCommonSetupEvent event) {
    }

    public void onClientSetup(FMLClientSetupEvent event) {
    }
}
