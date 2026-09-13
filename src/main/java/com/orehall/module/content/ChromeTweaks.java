package com.orehall.module.content;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Controller and configuration coordinator for Chrome Tweaks.
 * Manages the chrome_tweaks subcategory under the content module.
 */
public class ChromeTweaks {
    private final ContentModule parent;
    private ModConfigSpec.BooleanValue enabledConfig;

    public ChromeTweaks(ContentModule parent) {
        this.parent = parent;
    }

    public void buildConfig(ModConfigSpec.Builder builder) {
        builder.comment("Tweaks and additions for mod content and enhancements.")
                .translation("orehall.configuration.content.chrome_tweaks")
                .push("chrome_tweaks");

        enabledConfig = builder
                .comment("Master switch to enable or disable all features in the Chrome Tweaks category.")
                .translation("orehall.configuration.content.chrome_tweaks.enabled")
                .define("enabled", true);

        oreGenerationConfig = builder
                .comment("When enabled, chrome ore generates in the Nether replacing blackstone.")
                .translation("orehall.configuration.content.chrome_tweaks.ore_generation")
                .define("ore_generation", true);

        builder.pop();
    }

    private ModConfigSpec.BooleanValue oreGenerationConfig;

    public boolean isEnabled() {
        return parent.isEnabled() && (enabledConfig == null || enabledConfig.get());
    }

    public boolean isOreGenerationEnabled() {
        return isEnabled() && (oreGenerationConfig == null || oreGenerationConfig.get());
    }

    public void onConstruct(IEventBus modBus) {
    }

    public void onCommonSetup(FMLCommonSetupEvent event) {
    }

    public void onClientSetup(FMLClientSetupEvent event) {
    }
}
