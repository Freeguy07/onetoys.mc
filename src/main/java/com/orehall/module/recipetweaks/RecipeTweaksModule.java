package com.orehall.module.recipetweaks;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import com.orehall.module.ModuleCategory;
import com.orehall.module.OrehallModule;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.common.ModConfigSpec;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddReloadListenerEvent;

public class RecipeTweaksModule extends OrehallModule {
    public static final String ID = "recipe_tweaks";

    private final Map<String, ModConfigSpec.BooleanValue> replacementConfigs = new LinkedHashMap<>();
    private final Map<String, ModConfigSpec.BooleanValue> utilityConfigs = new LinkedHashMap<>();

    private ModConfigSpec.BooleanValue buttonsRequireRedstoneEnabled;
    private ModConfigSpec.IntValue buttonsRequireRedstoneOutputCount;

    private ModConfigSpec.BooleanValue pressurePlatesRequireRedstoneEnabled;
    private ModConfigSpec.IntValue pressurePlatesRequireRedstoneOutputCount;

    // Map of replacement recipe path -> vanilla recipe target path
    private static final Map<String, String> REPLACEMENT_TARGETS = Map.of(
            "reliable_recovery_compass", "recovery_compass",
            "reliable_shears", "shears",
            "reliable_spyglass", "spyglass",
            "reliable_lever", "lever"
    );

    // List of utility recipes
    private static final String[] UTILITY_RECIPES = new String[] {
            "sugar_from_beetroot",
            "bell"
    };

    public RecipeTweaksModule() {
        super(ID, "Recipe Tweaks", "Various tweaks, replacements, and utility recipes.", ModuleCategory.RECIPE_TWEAKS, true);
    }

    @Override
    public void onConstruct(IEventBus modBus) {
        NeoForge.EVENT_BUS.addListener(this::onAddReloadListener);
    }

    private void onAddReloadListener(AddReloadListenerEvent event) {
        event.addListener(new VanillaRecipeDisablerListener(event.getServerResources()));
    }

    @Override
    public void buildConfig(ModConfigSpec.Builder builder) {
        builder.comment("Recipes that replace vanilla crafting recipes when enabled.")
                .translation("orehall.configuration.recipe_tweaks.replacement_recipes")
                .push("replacement_recipes");

        for (Map.Entry<String, String> entry : REPLACEMENT_TARGETS.entrySet()) {
            String replacementName = entry.getKey();
            String vanillaTarget = entry.getValue();
            ModConfigSpec.BooleanValue configValue = builder
                    .comment("When enabled, replaces vanilla 'minecraft:" + vanillaTarget + "' with '" + replacementName + "'.")
                    .translation("orehall.configuration.recipe_tweaks.replacement_recipes." + replacementName)
                    .define(replacementName, true);
            replacementConfigs.put(replacementName, configValue);
        }
        builder.pop();

        builder.comment("Utility and convenience recipes.")
                .translation("orehall.configuration.recipe_tweaks.utility")
                .push("utility");

        for (String utilityName : UTILITY_RECIPES) {
            ModConfigSpec.BooleanValue configValue = builder
                    .comment("Enable the '" + utilityName + "' recipe.")
                    .translation("orehall.configuration.recipe_tweaks.utility." + utilityName)
                    .define(utilityName, true);
            utilityConfigs.put(utilityName, configValue);
        }
        builder.pop();

        builder.comment("Experimental recipes and dynamic crafting mechanics.")
                .translation("orehall.configuration.recipe_tweaks.experimental")
                .push("experimental");

        builder.comment("Automatic recipe replacement system.")
                .translation("orehall.configuration.recipe_tweaks.experimental.automatic_recipe_replacement_system")
                .push("automatic_recipe_replacement_system");

        builder.comment("Buttons require redstone to craft and yield multiple buttons.")
                .translation("orehall.configuration.recipe_tweaks.experimental.automatic_recipe_replacement_system.buttons_require_redstone")
                .push("buttons_require_redstone");

        buttonsRequireRedstoneEnabled = builder
                .comment("When enabled, replaces all 1-ingredient button recipes with a 2-ingredient recipe (block on top, redstone on bottom).")
                .translation("orehall.configuration.recipe_tweaks.experimental.automatic_recipe_replacement_system.buttons_require_redstone.enabled")
                .define("enabled", true);

        buttonsRequireRedstoneOutputCount = builder
                .comment("Number of buttons crafted per recipe.")
                .translation("orehall.configuration.recipe_tweaks.experimental.automatic_recipe_replacement_system.buttons_require_redstone.output_count")
                .defineInRange("output_count", 4, 1, 64);

        builder.pop(); // buttons_require_redstone

        builder.comment("Pressure plates require redstone to craft and yield multiple pressure plates.")
                .translation("orehall.configuration.recipe_tweaks.experimental.automatic_recipe_replacement_system.pressure_plates_require_redstone")
                .push("pressure_plates_require_redstone");

        pressurePlatesRequireRedstoneEnabled = builder
                .comment("When enabled, replaces all 2-ingredient pressure plate recipes with a shaped recipe (3 blocks on top, redstone in middle bottom).")
                .translation("orehall.configuration.recipe_tweaks.experimental.automatic_recipe_replacement_system.pressure_plates_require_redstone.enabled")
                .define("enabled", true);

        pressurePlatesRequireRedstoneOutputCount = builder
                .comment("Number of pressure plates crafted per recipe.")
                .translation("orehall.configuration.recipe_tweaks.experimental.automatic_recipe_replacement_system.pressure_plates_require_redstone.output_count")
                .defineInRange("output_count", 2, 1, 64);

        builder.pop(); // pressure_plates_require_redstone
        builder.pop(); // automatic_recipe_replacement_system
        builder.pop(); // experimental
    }

    public boolean isButtonsRequireRedstoneEnabled() {
        return isEnabled() && buttonsRequireRedstoneEnabled != null && buttonsRequireRedstoneEnabled.get();
    }

    public int getButtonsRequireRedstoneOutputCount() {
        return buttonsRequireRedstoneOutputCount != null ? buttonsRequireRedstoneOutputCount.get() : 4;
    }

    public boolean isPressurePlatesRequireRedstoneEnabled() {
        return isEnabled() && pressurePlatesRequireRedstoneEnabled != null && pressurePlatesRequireRedstoneEnabled.get();
    }

    public int getPressurePlatesRequireRedstoneOutputCount() {
        return pressurePlatesRequireRedstoneOutputCount != null ? pressurePlatesRequireRedstoneOutputCount.get() : 2;
    }

    /**
     * Checks whether an individual recipe is enabled.
     * Checks module enabled state first, then the specific recipe config toggle.
     */
    public boolean isRecipeEnabled(String recipePath) {
        if (!isEnabled()) {
            return false;
        }
        ModConfigSpec.BooleanValue config = replacementConfigs.get(recipePath);
        if (config != null) {
            return config.get();
        }
        config = utilityConfigs.get(recipePath);
        return config != null && config.get();
    }

    /**
     * Returns a map of (vanilla recipe ResourceLocation -> replacement recipe ResourceLocation)
     * for all replacement recipes that are currently enabled.
     */
    public Map<ResourceLocation, ResourceLocation> getActiveReplacements() {
        if (!isEnabled()) {
            return Collections.emptyMap();
        }

        Map<ResourceLocation, ResourceLocation> active = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : REPLACEMENT_TARGETS.entrySet()) {
            String replacementName = entry.getKey();
            String vanillaTarget = entry.getValue();

            ModConfigSpec.BooleanValue config = replacementConfigs.get(replacementName);
            if (config != null && config.get()) {
                active.put(
                        ResourceLocation.withDefaultNamespace(vanillaTarget),
                        ResourceLocation.fromNamespaceAndPath("orehall", replacementName)
                );
            }
        }
        return Collections.unmodifiableMap(active);
    }
}
