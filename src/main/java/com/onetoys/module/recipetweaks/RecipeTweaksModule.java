package com.onetoys.module.recipetweaks;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import com.onetoys.Onetoys;
import com.onetoys.mixin.HolderReferenceAccessor;
import com.onetoys.mixin.HolderSetNamedAccessor;
import com.onetoys.module.ModuleCategory;
import com.onetoys.module.ModuleManager;
import com.onetoys.module.OnetoysModule;

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
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.TagsUpdatedEvent;

public class RecipeTweaksModule extends OnetoysModule {
    public static final String ID = "recipe_tweaks";

    private static final ResourceKey<Item> FLINT_KEY =
            ResourceKey.create(Registries.ITEM, ResourceLocation.withDefaultNamespace("flint"));

    private static final Set<ResourceLocation> VANILLA_STONE_TOOL_MATERIALS = Set.of(
            ResourceLocation.withDefaultNamespace("cobblestone"),
            ResourceLocation.withDefaultNamespace("blackstone"),
            ResourceLocation.withDefaultNamespace("cobbled_deepslate")
    );

    private ModConfigSpec.BooleanValue handyStoneMaterialsConfig;
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
            updateRuntimeHandyStoneMaterialsTag(isHandyStoneMaterialsActive());
        }
    }

    private void onModConfigEvent(ModConfigEvent event) {
        if (event.getConfig().getModId().equals(Onetoys.MODID)) {
            updateRuntimeHandyStoneMaterialsTag(isHandyStoneMaterialsActive());
        }
    }

    @Override
    public void onCommonSetup(FMLCommonSetupEvent event) {
        if (isEnabled()) {
            event.enqueueWork(() -> updateRuntimeHandyStoneMaterialsTag(isHandyStoneMaterialsActive()));
        }
    }

    @Override
    public void buildConfig(ModConfigSpec.Builder builder) {
        handyStoneMaterialsConfig = builder
                .comment("When enabled, adds flint and #c:stones to the stone tool materials tag (#minecraft:stone_tool_materials), allowing stone tools to be crafted with flint and common stones.")
                .translation("onetoys.configuration.recipe_tweaks.handy_stone_materials")
                .define("handy_stone_materials", true);

        builder.comment("Recipes that replace vanilla crafting recipes when enabled.")
                .translation("onetoys.configuration.recipe_tweaks.replacement_recipes")
                .push("replacement_recipes");

        for (Map.Entry<String, String> entry : REPLACEMENT_TARGETS.entrySet()) {
            String replacementName = entry.getKey();
            String vanillaTarget = entry.getValue();
            ModConfigSpec.BooleanValue configValue = builder
                    .comment("When enabled, replaces vanilla 'minecraft:" + vanillaTarget + "' with '" + replacementName + "'.")
                    .translation("onetoys.configuration.recipe_tweaks.replacement_recipes." + replacementName)
                    .define(replacementName, true);
            replacementConfigs.put(replacementName, configValue);
        }
        builder.pop();

        builder.comment("Utility and convenience recipes.")
                .translation("onetoys.configuration.recipe_tweaks.utility")
                .push("utility");

        for (String utilityName : UTILITY_RECIPES) {
            ModConfigSpec.BooleanValue configValue = builder
                    .comment("Enable the '" + utilityName + "' recipe.")
                    .translation("onetoys.configuration.recipe_tweaks.utility." + utilityName)
                    .define(utilityName, true);
            utilityConfigs.put(utilityName, configValue);
        }
        builder.pop();
    }

    /**
     * Checks whether handy stone materials (flint and #c:stones) are configured to be added into stone tool materials.
     */
    public boolean isHandyStoneMaterialsEnabled() {
        return handyStoneMaterialsConfig == null || handyStoneMaterialsConfig.get();
    }

    @Deprecated
    public boolean isFlintInStoneToolMaterialsEnabled() {
        return isHandyStoneMaterialsEnabled();
    }

    /**
     * Helper to check if both the Recipe Tweaks module and the handy stone materials toggle are active.
     */
    public static boolean isHandyStoneMaterialsActive() {
        return ModuleManager.get().getModule(RecipeTweaksModule.class)
                .map(m -> m.isEnabled() && m.isHandyStoneMaterialsEnabled())
                .orElse(false);
    }

    @Deprecated
    public static boolean isFlintInStoneToolMaterialsActive() {
        return isHandyStoneMaterialsActive();
    }

    /**
     * Dynamically updates the in-memory #minecraft:stone_tool_materials tag in BuiltInRegistries.ITEM.
     * Adds flint and all items in #c:stones when enabled, and removes them when disabled.
     * Fires tag rebinding invalidation callbacks so recipe ingredient caches update immediately.
     */
    @SuppressWarnings("unchecked")
    public static synchronized void updateRuntimeHandyStoneMaterialsTag(boolean enabled) {
        try {
            Optional<HolderSet.Named<Item>> tagOpt = BuiltInRegistries.ITEM.getTag(ItemTags.STONE_TOOL_MATERIALS);
            if (tagOpt.isEmpty()) {
                return;
            }

            HolderSet.Named<Item> namedSet = tagOpt.get();
            Holder.Reference<Item> flintHolder = BuiltInRegistries.ITEM.getHolderOrThrow(FLINT_KEY);

            List<Holder.Reference<Item>> stonesHolders = new ArrayList<>();
            Optional<HolderSet.Named<Item>> cStonesOpt = BuiltInRegistries.ITEM.getTag(Tags.Items.STONES);
            if (cStonesOpt.isPresent()) {
                for (Holder<Item> holder : cStonesOpt.get()) {
                    if (holder instanceof Holder.Reference<Item> ref) {
                        stonesHolders.add(ref);
                    }
                }
            }

            List<Holder<Item>> currentContents = new ArrayList<>(namedSet.stream().toList());
            boolean modified = false;

            if (enabled) {
                if (!currentContents.contains(flintHolder)) {
                    currentContents.add(flintHolder);
                    modified = true;
                }
                bindTagToHolder(flintHolder, ItemTags.STONE_TOOL_MATERIALS, true);

                for (Holder.Reference<Item> stoneRef : stonesHolders) {
                    if (!currentContents.contains(stoneRef)) {
                        currentContents.add(stoneRef);
                        modified = true;
                    }
                    bindTagToHolder(stoneRef, ItemTags.STONE_TOOL_MATERIALS, true);
                }

                if (modified) {
                    ((HolderSetNamedAccessor<Item>) (Object) namedSet).Onetoys$bind(currentContents);
                    Onetoys.LOGGER.debug("Dynamic Tag: Added flint and c:stones to stone_tool_materials tag.");
                }
            } else {
                if (currentContents.remove(flintHolder)) {
                    modified = true;
                }
                bindTagToHolder(flintHolder, ItemTags.STONE_TOOL_MATERIALS, false);

                for (Holder.Reference<Item> stoneRef : stonesHolders) {
                    if (!VANILLA_STONE_TOOL_MATERIALS.contains(stoneRef.key().location())) {
                        if (currentContents.remove(stoneRef)) {
                            modified = true;
                        }
                        bindTagToHolder(stoneRef, ItemTags.STONE_TOOL_MATERIALS, false);
                    }
                }

                if (modified) {
                    ((HolderSetNamedAccessor<Item>) (Object) namedSet).Onetoys$bind(currentContents);
                    Onetoys.LOGGER.debug("Dynamic Tag: Removed flint and c:stones from stone_tool_materials tag.");
                }
            }
        } catch (Exception e) {
            Onetoys.LOGGER.debug("Could not update runtime stone tool materials tag: {}", e.getMessage());
        }
    }

    @Deprecated
    public static void updateRuntimeFlintTag(boolean enabled) {
        updateRuntimeHandyStoneMaterialsTag(enabled);
    }

    @SuppressWarnings("unchecked")
    private static void bindTagToHolder(Holder.Reference<Item> holder, TagKey<Item> tag, boolean add) {
        Set<TagKey<Item>> tags = holder.tags().collect(Collectors.toCollection(HashSet::new));
        if (add) {
            if (tags.add(tag)) {
                ((HolderReferenceAccessor<Item>) (Object) holder).Onetoys$bindTags(tags);
            }
        } else {
            if (tags.remove(tag)) {
                ((HolderReferenceAccessor<Item>) (Object) holder).Onetoys$bindTags(tags);
            }
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
        if ("handy_stone_materials".equals(recipePath) || "flint_in_stone_tool_materials".equals(recipePath)) {
            return isHandyStoneMaterialsEnabled();
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
                        ResourceLocation.fromNamespaceAndPath("onetoys", replacementName)
                );
            }
        }
        return Collections.unmodifiableMap(active);
    }
}
