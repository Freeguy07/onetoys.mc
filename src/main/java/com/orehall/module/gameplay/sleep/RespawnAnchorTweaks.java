package com.orehall.module.gameplay.sleep;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import com.orehall.module.ModuleManager;
import com.orehall.module.gameplay.GameplayTweaksModule;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Controller for the Respawn Anchor Required sub-category under Sleeping Tweaks.
 * Manages dynamic replacement fuel (items or tags), multi-dimension functionality
 * (preventing explosions in Overworld/End), and dimension blacklisting.
 * (The reliable crafting recipe is managed under Recipe Tweaks).
 */
public class RespawnAnchorTweaks {
    private static RespawnAnchorTweaks instance;

    private ModConfigSpec.BooleanValue enabledConfig;
    private ModConfigSpec.ConfigValue<String> fuelItemConfig;
    private ModConfigSpec.ConfigValue<String> dimensionBlacklistConfig;

    private volatile Set<ResourceLocation> cachedBlacklist = null;
    private volatile String lastBlacklistRaw = null;

    public RespawnAnchorTweaks() {
        instance = this;
    }

    public static RespawnAnchorTweaks get() {
        return instance;
    }

    public void buildConfig(ModConfigSpec.Builder builder) {
        builder.comment("Settings and mechanics for the respawn anchor.")
                .translation("orehall.configuration.gameplay_tweaks.sleeping_tweaks.respawn_anchor_required")
                .push("respawn_anchor_required");

        enabledConfig = builder
                .comment("Master switch to enable or disable respawn anchor tweaks.")
                .translation("orehall.configuration.gameplay_tweaks.sleeping_tweaks.respawn_anchor_required.enabled")
                .define("enabled", true);

        fuelItemConfig = builder
                .comment("Item or tag used as replacement fuel for the respawn anchor.",
                        "Default: 'minecraft:echo_shard'. Supports item IDs (e.g. 'minecraft:echo_shard') or tags (e.g. '#c:gems/amethyst').",
                        "If left empty or set to an invalid ID, defaults back to 'minecraft:glowstone'.",
                        "When set to a valid non-glowstone item, glowstone is no longer accepted as fuel.")
                .translation("orehall.configuration.gameplay_tweaks.sleeping_tweaks.respawn_anchor_required.fuel_item")
                .define("fuel_item", "minecraft:echo_shard");

        dimensionBlacklistConfig = builder
                .comment("A comma-separated list of dimension IDs where the respawn anchor will not work and will explode upon interaction.",
                        "Default: '' (empty string, meaning respawn anchor works in all dimensions including Overworld and End).",
                        "Example: 'minecraft:the_end' or 'minecraft:the_end, othermod:dimension'")
                .translation("orehall.configuration.gameplay_tweaks.sleeping_tweaks.respawn_anchor_required.dimension_blacklist")
                .define("dimension_blacklist", "");

        builder.pop();
    }

    public boolean isEnabled() {
        return enabledConfig == null || enabledConfig.get();
    }

    public static boolean isTweakEnabled() {
        if (!ModuleManager.get().isModuleEnabled(GameplayTweaksModule.ID)) {
            return false;
        }
        RespawnAnchorTweaks tweaks = instance;
        if (tweaks == null) {
            return false;
        }
        GameplayTweaksModule module = ModuleManager.get().getModule(GameplayTweaksModule.class).orElse(null);
        if (module == null || !module.getSleepingTweaks().isEnabled()) {
            return false;
        }
        return tweaks.isEnabled();
    }

    public String getFuelItemRaw() {
        return fuelItemConfig != null ? fuelItemConfig.get() : "minecraft:echo_shard";
    }

    public static boolean isValidFuel(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        RespawnAnchorTweaks tweaks = instance;
        String raw = tweaks != null ? tweaks.getFuelItemRaw() : "minecraft:echo_shard";
        if (raw == null || raw.isBlank()) {
            return stack.is(Items.GLOWSTONE);
        }
        raw = raw.trim();

        // 1. Tag specification (starts with '#')
        if (raw.startsWith("#")) {
            ResourceLocation tagLoc = ResourceLocation.tryParse(raw.substring(1));
            if (tagLoc != null) {
                TagKey<Item> tagKey = TagKey.create(Registries.ITEM, tagLoc);
                return stack.is(tagKey);
            }
            // Invalid tag ID syntax -> fallback to glowstone
            return stack.is(Items.GLOWSTONE);
        }

        // 2. Direct item ID specification
        ResourceLocation itemLoc = ResourceLocation.tryParse(raw);
        if (itemLoc == null) {
            return stack.is(Items.GLOWSTONE);
        }

        if (!BuiltInRegistries.ITEM.containsKey(itemLoc)) {
            // Unknown or invalid item ID -> fallback to glowstone
            return stack.is(Items.GLOWSTONE);
        }

        Item configuredItem = BuiltInRegistries.ITEM.get(itemLoc);
        if (configuredItem == Items.AIR && !itemLoc.equals(BuiltInRegistries.ITEM.getKey(Items.AIR))) {
            return stack.is(Items.GLOWSTONE);
        }

        return stack.is(configuredItem);
    }

    public static boolean isDimensionBlacklisted(Level level) {
        RespawnAnchorTweaks tweaks = instance;
        if (tweaks == null) {
            return false;
        }
        Set<ResourceLocation> blacklist = tweaks.getBlacklistedDimensions();
        if (blacklist.isEmpty()) {
            return false;
        }
        return blacklist.contains(level.dimension().location());
    }

    public Set<ResourceLocation> getBlacklistedDimensions() {
        String raw = dimensionBlacklistConfig != null ? dimensionBlacklistConfig.get() : "";
        if (raw == null || raw.isBlank()) {
            return Collections.emptySet();
        }
        if (raw.equals(lastBlacklistRaw) && cachedBlacklist != null) {
            return cachedBlacklist;
        }

        Set<ResourceLocation> set = new HashSet<>();
        String[] split = raw.split(",");
        for (String seg : split) {
            String trimmed = seg.trim();
            if (!trimmed.isEmpty()) {
                ResourceLocation loc = ResourceLocation.tryParse(trimmed);
                if (loc != null) {
                    set.add(loc);
                }
            }
        }
        cachedBlacklist = Collections.unmodifiableSet(set);
        lastBlacklistRaw = raw;
        return cachedBlacklist;
    }

    public void onCommonSetup(FMLCommonSetupEvent event) {
        // Dynamic dispenser charging is handled dynamically via DispenserBlockMixin
    }
}
