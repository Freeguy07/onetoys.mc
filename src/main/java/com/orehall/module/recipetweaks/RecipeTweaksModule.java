package com.orehall.module.recipetweaks;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
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

    private ModConfigSpec.BooleanValue automaticSerializedRecipeReplacementSystemEnabled;
    private ModConfigSpec.ConfigValue<List<? extends String>> automaticSerializedRecipeReplacementRules;

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

        builder.comment("Automatic serialized recipe replacement system.")
                .translation("orehall.configuration.recipe_tweaks.automatic_serialized_recipe_replacement_system")
                .push("automatic_serialized_recipe_replacement_system");

        automaticSerializedRecipeReplacementSystemEnabled = builder
                .comment("Master switch to enable or disable the entire automatic serialized recipe replacement system.")
                .translation("orehall.configuration.recipe_tweaks.automatic_serialized_recipe_replacement_system.enabled")
                .define("enabled", true);

        automaticSerializedRecipeReplacementRules = builder
                .comment("List of automatic recipe replacement rules using the key-value DSL.",
                        "Default entries configure button, pressure plate, door, trapdoor, fence, fence gate, sign, hanging sign, and chain recipes.",
                        "Syntax: \"id=<name>; enabled=<true|false>; target=<#tag|item|*wildcard>; input_count=<inputs>; pattern=<rows_with_slash>; keys=<key:item>; output=<count|2x>; exclude=<item|tag>\"")
                .translation("orehall.configuration.recipe_tweaks.automatic_serialized_recipe_replacement_system.rules")
                .defineListAllowEmpty("rules",
                        List.of(
                                "id=buttons_require_redstone; enabled=true; target=*_button; input_count=1; pattern=#/R; keys=R:minecraft:redstone; output=1; exclude=minecraft:redstone",
                                "id=pressure_plates_require_redstone; enabled=true; target=*_pressure_plate; input_count=2; pattern=###/.R.; keys=R:minecraft:redstone; output=3; exclude=minecraft:redstone",
                                "id=cheap_doors; enabled=true; target=*_door; input_count=6; pattern=##/##/##; output=4",
                                "id=cheap_trapdoors; enabled=true; target=*_trapdoor; input_count=6; pattern=###/###; output=4",
                                "id=cheap_fences; enabled=true; target=*_fence; input_count=6; pattern=#S#/#S#; keys=S:minecraft:stick; output=4; exclude=minecraft:nether_brick",
                                "id=cheap_fence_gates; enabled=true; target=*_fence_gate; input_count=6; pattern=S#S/S#S; keys=S:minecraft:stick; output=2",
                                "id=cheap_signs; enabled=true; target=*_sign; input_count=7; pattern=#/S; keys=S:minecraft:stick; output=2; exclude=minecraft:chain",
                                "id=cheap_hanging_signs; enabled=true; target=*_hanging_sign; input_count=8; pattern=C/#; keys=C:minecraft:chain; output=2",
                                "id=cheap_chains; enabled=true; target=*chain; input_count=3; pattern=#/#/#; output=2"
                        ),
                        () -> "",
                        obj -> obj instanceof String str && str.contains("target=")
                );

        builder.pop(); // automatic_serialized_recipe_replacement_system
    }

    public boolean isAutomaticSerializedRecipeReplacementSystemEnabled() {
        return isEnabled() && automaticSerializedRecipeReplacementSystemEnabled != null && automaticSerializedRecipeReplacementSystemEnabled.get();
    }

    public List<String> getAutomaticSerializedRecipeReplacementRules() {
        if (automaticSerializedRecipeReplacementRules == null) {
            return List.of();
        }
        List<? extends String> raw = automaticSerializedRecipeReplacementRules.get();
        return raw.stream()
                .filter(s -> s != null && !s.isBlank())
                .map(String::trim)
                .toList();
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
