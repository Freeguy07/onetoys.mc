package com.orehall.module.recipetweaks;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import com.orehall.Orehall;
import com.orehall.mixin.HolderReferenceAccessor;
import com.orehall.mixin.HolderSetNamedAccessor;
import com.orehall.module.ModuleCategory;
import com.orehall.module.ModuleManager;
import com.orehall.module.OrehallModule;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.TagsUpdatedEvent;

public class RecipeTweaksModule extends OrehallModule {
    public static final String ID = "recipe_tweaks";

    private static final ResourceKey<Item> FLINT_KEY =
            ResourceKey.create(Registries.ITEM, ResourceLocation.withDefaultNamespace("flint"));

    private ModConfigSpec.BooleanValue flintInStoneToolMaterialsConfig;
    private final Map<String, ModConfigSpec.BooleanValue> replacementConfigs = new LinkedHashMap<>();
    private final Map<String, ModConfigSpec.BooleanValue> utilityConfigs = new LinkedHashMap<>();

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

    private static RecipeTweaksModule instance;

    public RecipeTweaksModule() {
        super(ID, "Recipe Tweaks", "Various tweaks, replacements, and utility recipes.", ModuleCategory.RECIPE_TWEAKS, true);
        instance = this;
    }

    public static RecipeTweaksModule get() {
        return instance;
    }

    @Override
    public void onConstruct(IEventBus modBus) {
        NeoForge.EVENT_BUS.addListener(this::onAddReloadListener);
        NeoForge.EVENT_BUS.addListener(this::onTagsUpdated);
        modBus.addListener(this::onModConfigEvent);
    }

    private void onAddReloadListener(AddReloadListenerEvent event) {
        event.addListener(new VanillaRecipeDisablerListener(event.getServerResources()));
    }

    private void onTagsUpdated(TagsUpdatedEvent event) {
        if (isEnabled()) {
            updateRuntimeFlintTag(isFlintInStoneToolMaterialsActive());
        }
    }

    private void onModConfigEvent(ModConfigEvent event) {
        if (event.getConfig().getModId().equals(Orehall.MODID)) {
            updateRuntimeFlintTag(isFlintInStoneToolMaterialsActive());
        }
    }

    @Override
    public void onCommonSetup(FMLCommonSetupEvent event) {
        if (isEnabled()) {
            event.enqueueWork(() -> updateRuntimeFlintTag(isFlintInStoneToolMaterialsActive()));
        }
    }

    @Override
    public void buildConfig(ModConfigSpec.Builder builder) {
        flintInStoneToolMaterialsConfig = builder
                .comment("When enabled, adds flint to the stone tool materials tag (#minecraft:stone_tool_materials), allowing stone tools to be crafted with flint.")
                .translation("orehall.configuration.recipe_tweaks.flint_in_stone_tool_materials")
                .define("flint_in_stone_tool_materials", true);

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
    }

    /**
     * Checks whether flint is configured to be added into stone tool materials.
     */
    public boolean isFlintInStoneToolMaterialsEnabled() {
        return flintInStoneToolMaterialsConfig == null || flintInStoneToolMaterialsConfig.get();
    }

    /**
     * Helper to check if both the Recipe Tweaks module and the flint stone tools toggle are active.
     */
    public static boolean isFlintInStoneToolMaterialsActive() {
        return ModuleManager.get().getModule(RecipeTweaksModule.class)
                .map(m -> m.isEnabled() && m.isFlintInStoneToolMaterialsEnabled())
                .orElse(false);
    }

    /**
     * Dynamically updates the in-memory #minecraft:stone_tool_materials tag in BuiltInRegistries.ITEM.
     * Fires tag rebinding invalidation callbacks so recipe ingredient caches update immediately.
     */
    @SuppressWarnings("unchecked")
    public static synchronized void updateRuntimeFlintTag(boolean enabled) {
        try {
            Optional<HolderSet.Named<Item>> tagOpt = BuiltInRegistries.ITEM.getTag(ItemTags.STONE_TOOL_MATERIALS);
            if (tagOpt.isEmpty()) {
                return;
            }

            HolderSet.Named<Item> namedSet = tagOpt.get();
            Holder.Reference<Item> flintHolder = BuiltInRegistries.ITEM.getHolderOrThrow(FLINT_KEY);

            boolean containsFlint = flintHolder.is(ItemTags.STONE_TOOL_MATERIALS);
            if (enabled == containsFlint) {
                return;
            }

            List<Holder<Item>> currentContents = new ArrayList<>(namedSet.stream().toList());

            if (enabled) {
                currentContents.add(flintHolder);
                ((HolderSetNamedAccessor<Item>) (Object) namedSet).orehall$bind(currentContents);

                Set<TagKey<Item>> tags = flintHolder.tags().collect(Collectors.toCollection(HashSet::new));
                tags.add(ItemTags.STONE_TOOL_MATERIALS);
                ((HolderReferenceAccessor<Item>) (Object) flintHolder).orehall$bindTags(tags);
                Orehall.LOGGER.debug("Dynamic Tag: Added flint to stone_tool_materials tag.");
            } else {
                currentContents.remove(flintHolder);
                ((HolderSetNamedAccessor<Item>) (Object) namedSet).orehall$bind(currentContents);

                Set<TagKey<Item>> tags = flintHolder.tags().collect(Collectors.toCollection(HashSet::new));
                tags.remove(ItemTags.STONE_TOOL_MATERIALS);
                ((HolderReferenceAccessor<Item>) (Object) flintHolder).orehall$bindTags(tags);
                Orehall.LOGGER.debug("Dynamic Tag: Removed flint from stone_tool_materials tag.");
            }
        } catch (Exception e) {
            Orehall.LOGGER.debug("Could not update runtime flint tag: {}", e.getMessage());
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
        if ("flint_in_stone_tool_materials".equals(recipePath)) {
            return isFlintInStoneToolMaterialsEnabled();
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
