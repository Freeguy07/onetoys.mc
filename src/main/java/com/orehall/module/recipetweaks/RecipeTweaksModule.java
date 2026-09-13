package com.orehall.module.recipetweaks;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.orehall.Orehall;
import com.orehall.module.ModuleCategory;
import com.orehall.module.OrehallModule;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddReloadListenerEvent;

public class RecipeTweaksModule extends OrehallModule {
    public static final String ID = "recipe_tweaks";

    private final Map<String, ModConfigSpec.BooleanValue> replacementConfigs = new LinkedHashMap<>();
    private final Map<String, ModConfigSpec.BooleanValue> utilityConfigs = new LinkedHashMap<>();

    private ModConfigSpec.BooleanValue automaticSerializedRecipeReplacementSystemEnabled;

    // Map of replacement recipe path -> vanilla recipe target path
    private static final Map<String, String> REPLACEMENT_TARGETS = Map.of(
            "reliable_recovery_compass", "recovery_compass",
            "reliable_shears", "shears",
            "reliable_spyglass", "spyglass",
            "reliable_lever", "lever",
            "reliable_respawn_anchor", "respawn_anchor"
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

        builder.comment("Dynamic recipe replacement system.",
                        "Enables in-place ingredient substitutions ('swap'), multi-ingredient reconstructions, and output tweaks.",
                        "Replacement rules are configured in 'config/orehall/recipes.toml'.",
                        "WARNING: Be aware that the replacement system is experimental and can make mistakes.")
                .translation("orehall.configuration.recipe_tweaks.automatic_serialized_recipe_replacement_system")
                .push("automatic_serialized_recipe_replacement_system");

        automaticSerializedRecipeReplacementSystemEnabled = builder
                .comment("Master switch to enable or disable the entire dynamic recipe replacement system.",
                        "Detailed replacement rules and developer documentation are located in 'config/orehall/recipes.toml'.",
                        "WARNING: Be aware that the replacement system is experimental and can make mistakes.")
                .translation("orehall.configuration.recipe_tweaks.automatic_serialized_recipe_replacement_system.enabled")
                .define("enabled", true);

        builder.pop(); // automatic_serialized_recipe_replacement_system
    }

    public boolean isAutomaticSerializedRecipeReplacementSystemEnabled() {
        return isEnabled() && automaticSerializedRecipeReplacementSystemEnabled != null && automaticSerializedRecipeReplacementSystemEnabled.get();
    }

    public List<String> getAutomaticSerializedRecipeReplacementRules() {
        Path configPath = FMLPaths.CONFIGDIR.get().resolve("orehall").resolve("recipes.toml");
        ensureRecipesConfigFile(configPath);

        List<String> rules = new ArrayList<>();
        try (CommentedFileConfig fileConfig = CommentedFileConfig.builder(configPath).sync().build()) {
            fileConfig.load();
            Object rawObj = fileConfig.get("rules");
            if (rawObj instanceof List<?> list) {
                for (Object item : list) {
                    if (item instanceof String s && !s.isBlank()) {
                        rules.add(s.trim());
                    }
                }
            }
        } catch (Exception e) {
            Orehall.LOGGER.error("Failed to load recipe replacement rules from '{}': {}", configPath, e.getMessage());
        }
        return rules;
    }

    private void ensureRecipesConfigFile(Path configPath) {
        if (Files.exists(configPath)) {
            return;
        }
        try {
            Files.createDirectories(configPath.getParent());
            String template = """
# ================================================================================
# Orehall - Dynamic Recipe Replacement Configuration
# ================================================================================
# Scans all server recipes during datapack reload, identifies matching targets,
# and safely replaces them in-place with zero file collisions.
#
# NOTE: Be aware that the replacement system is experimental and can make mistakes.
#
# --- SELECTORS (At least ONE is required) ---
#   * target=<item|#tag|*wildcard>      : Match recipes producing this output item/tag/wildcard.
#                                         Supports comma-separated lists (e.g. "target=#c:ingots,minecraft:iron_sword").
#   * has_ingredient=<item|#tag|*wild>  : Match recipes that consume this ingredient anywhere in their grid.
#   * recipe_id=<wildcard|regex>        : Match recipe by its ResourceLocation ID or path (e.g. "minecraft:*_door").
#   * input_count=<number>              : Optional constraint: only match recipes with exactly N non-empty slots.
#   * exclude=<item|#tag|*wildcard>     : Optional exclusion: skips recipe if it consumes or outputs this item/tag.
#
# --- ACTION MODES (Choose ONE) ---
#   [MODE 1: IN-PLACE INGREDIENT SUBSTITUTION ('swap')]
#     Surgically swaps an ingredient while preserving the original shape, air slots, and other ingredients.
#     Syntax: swap=<source_ingredient> -> <replacement_ingredient>
#     Example: swap=minecraft:slime_ball -> minecraft:honey_bottle
#
#   [MODE 2: SHAPED PATTERN RECONSTRUCTION ('pattern')]
#     Rebuilds the recipe into a custom shaped grid (1-3 rows separated by '/').
#     Air slots can be '.' or '_' (e.g. "###/.R.").
#     Dynamic Ingredient Captures (extracted automatically from source recipe):
#       '#' or '1' = First distinct material from source recipe (e.g. primary metal/wood).
#       '2'        = Second distinct material from source recipe (e.g. handle/secondary metal).
#       '3', '4'   = Third / Fourth distinct materials from source recipe.
#     Static Key Mappings:
#       keys=<char:item|#tag>,... (e.g. "keys=S:minecraft:stick,R:minecraft:redstone").
#
#   [MODE 3: SHAPELESS RECONSTRUCTION ('ingredients')]
#     Rebuilds the recipe into a shapeless craft.
#     Syntax: ingredients=<token1>,<token2>,...
#     Tokens can be dynamic ('#', '1', '2', '3') or static ('minecraft:redstone', '#c:rods').
#
#   [MODE 4: OUTPUT-ONLY TWEAK ('output')]
#     Leaves recipe shape and ingredients completely unchanged, adjusting only the output count.
#
# --- MODIFIERS & FLAGS ---
#   * id=<name>                         : Unique rule identifier for logs and recipe IDs (default: auto).
#   * enabled=<true|false>              : Enable or disable this rule independently (default: true).
#   * output=<count|multiplier>         : Fixed yield (e.g. "output=4") or multiplier (e.g. "output=2x").
#   * group=<string>                    : Optional recipe book group identifier.
#
# --- PRACTICAL EXAMPLES ---
#   1. In-place single ingredient swap (Sticky Pistons accept Honey):
#      "id=honey_sticky_piston; target=minecraft:sticky_piston; swap=minecraft:slime_ball -> minecraft:honey_bottle"
#   2. In-place material tier swap with output scaling (Copper Hoppers yielding 2):
#      "id=copper_hoppers; target=minecraft:hopper; swap=#c:ingots/iron -> #c:ingots/copper; output=2"
#   3. Input-triggered global swap (Replace slimeballs in ALL crafting recipes with honeycomb):
#      "id=honeycomb_substitute; has_ingredient=minecraft:slime_ball; swap=minecraft:slime_ball -> minecraft:honeycomb"
#   4. Dynamic multi-ingredient tool pattern (Cheaper Swords using 1 blade + 1 stick across ALL materials):
#      "id=compact_swords; target=*_sword; pattern=1/2; output=1"
#   5. Shaped pattern with static key (Cheaper pickaxes: 2 ingots/planks over 2 sticks):
#      "id=cheap_pickaxes; target=*_pickaxe; pattern=##/S./S.; keys=S:#c:rods/wooden; output=1"
#   6. Recipe ID targeted output boost (4 doors for all door recipes):
#      "id=cheap_doors; recipe_id=*_door; output=4"
#   7. Exclusion constraint (Cheaper fences except nether brick):
#      "id=cheap_fences; target=*_fence; pattern=#S#/#S#; keys=S:minecraft:stick; output=4; exclude=minecraft:nether_brick"
# ================================================================================

rules = [
    # Add your dynamic recipe replacement rules here
]
""";
            Files.writeString(configPath, template, StandardCharsets.UTF_8);
            Orehall.LOGGER.info("Generated dynamic recipe replacement configuration at '{}'", configPath);
        } catch (IOException e) {
            Orehall.LOGGER.error("Failed to create recipe replacement configuration at '{}'", configPath, e);
        }
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
