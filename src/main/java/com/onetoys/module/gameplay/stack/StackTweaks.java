package com.onetoys.module.gameplay.stack;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.electronwill.nightconfig.core.UnmodifiableConfig;
import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.onetoys.Onetoys;
import com.onetoys.mixin.ItemAccessor;

import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BedItem;
import net.minecraft.world.item.BoatItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MinecartItem;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.event.ModifyDefaultComponentsEvent;

public class StackTweaks {

    public static final String DEFAULT_CONFIG_TEMPLATE = """
            # ==============================================================================
            # Onetoys - Custom Stack Sizes Configuration
            # ==============================================================================
            # Configure custom maximum stack sizes for items, wildcard patterns, and tags.
            # Max stack size values must be integers between 1 and 99.
            # Changes take effect upon restarting Minecraft / launching the server.
            # ==============================================================================

            # High-priority pattern, wildcard, and tag overrides
            # Format: "target_pattern" = max_stack_size
            # Examples:
            # "minecraft:totem_of_undying" = 4
            # "*_horse_armor" = 16
            # "#c:potions" = 16
            # "minecraft:cake" = 16
            [overrides]

            # Optional per-item definitions grouped by mod namespace
            # [minecraft]
            # totem_of_undying = 4
            # cake = 16
            """;

    private ModConfigSpec.BooleanValue enabledConfig;
    private ModConfigSpec.BooleanValue saddlesStackTo16Config;
    private ModConfigSpec.BooleanValue boatsStackTo16Config;
    private ModConfigSpec.BooleanValue minecartsStackTo16Config;
    private ModConfigSpec.BooleanValue bedsStackTo16Config;
    private ModConfigSpec.BooleanValue potionsStackTo16Config;
    private ModConfigSpec.BooleanValue stewsAndSoupsStackTo16Config;
    private ModConfigSpec.BooleanValue enderPearlsStackTo64Config;
    private ModConfigSpec.BooleanValue snowballsStackTo64Config;
    private ModConfigSpec.BooleanValue eggsStackTo64Config;

    private final List<CustomStackRule.Tag> cachedTagRules = new ArrayList<>();
    private final Map<Item, Integer> originalStackSizes = new HashMap<>();

    public void buildConfig(ModConfigSpec.Builder builder) {
        builder.comment("Tweaks and adjustments to item maximum stack sizes.",
                        "Custom stack sizes are configured in 'config/Onetoys/stacks.toml'.")
                .translation("onetoys.configuration.gameplay_tweaks.stack_tweaks")
                .push("stack_tweaks");

        enabledConfig = builder
                .comment("Master switch to enable or disable all item stack size tweaks.")
                .translation("onetoys.configuration.gameplay_tweaks.stack_tweaks.enabled")
                .define("enabled", true);

        saddlesStackTo16Config = builder
                .comment("When enabled, saddles can stack up to 16.")
                .translation("onetoys.configuration.gameplay_tweaks.stack_tweaks.saddles_stack_to_16")
                .define("saddles_stack_to_16", true);

        boatsStackTo16Config = builder
                .comment("When enabled, boats, chest boats, and rafts can stack up to 16.")
                .translation("onetoys.configuration.gameplay_tweaks.stack_tweaks.boats_stack_to_16")
                .define("boats_stack_to_16", true);

        minecartsStackTo16Config = builder
                .comment("When enabled, all minecart variants can stack up to 16.")
                .translation("onetoys.configuration.gameplay_tweaks.stack_tweaks.minecarts_stack_to_16")
                .define("minecarts_stack_to_16", true);

        bedsStackTo16Config = builder
                .comment("When enabled, beds can stack up to 16.")
                .translation("onetoys.configuration.gameplay_tweaks.stack_tweaks.beds_stack_to_16")
                .define("beds_stack_to_16", true);

        potionsStackTo16Config = builder
                .comment("When enabled, regular, splash, and lingering potions can stack up to 16.")
                .translation("onetoys.configuration.gameplay_tweaks.stack_tweaks.potions_stack_to_16")
                .define("potions_stack_to_16", true);

        stewsAndSoupsStackTo16Config = builder
                .comment("When enabled, mushroom stew, beetroot soup, rabbit stew, and suspicious stew can stack up to 16.")
                .translation("onetoys.configuration.gameplay_tweaks.stack_tweaks.stews_and_soups_stack_to_16")
                .define("stews_and_soups_stack_to_16", true);

        enderPearlsStackTo64Config = builder
                .comment("When enabled, ender pearls can stack up to 64 instead of 16.")
                .translation("onetoys.configuration.gameplay_tweaks.stack_tweaks.ender_pearls_stack_to_64")
                .define("ender_pearls_stack_to_64", true);

        snowballsStackTo64Config = builder
                .comment("When enabled, snowballs can stack up to 64 instead of 16.")
                .translation("onetoys.configuration.gameplay_tweaks.stack_tweaks.snowballs_stack_to_64")
                .define("snowballs_stack_to_64", true);

        eggsStackTo64Config = builder
                .comment("When enabled, eggs can stack up to 64 instead of 16.")
                .translation("onetoys.configuration.gameplay_tweaks.stack_tweaks.eggs_stack_to_64")
                .define("eggs_stack_to_64", true);

        builder.pop();
    }

    public boolean isEnabled() {
        return enabledConfig == null || enabledConfig.get();
    }

    public boolean isSaddlesStackTo16() {
        return isEnabled() && (saddlesStackTo16Config == null || saddlesStackTo16Config.get());
    }

    public boolean isBoatsStackTo16() {
        return isEnabled() && (boatsStackTo16Config == null || boatsStackTo16Config.get());
    }

    public boolean isMinecartsStackTo16() {
        return isEnabled() && (minecartsStackTo16Config == null || minecartsStackTo16Config.get());
    }

    public boolean isBedsStackTo16() {
        return isEnabled() && (bedsStackTo16Config == null || bedsStackTo16Config.get());
    }

    public boolean isPotionsStackTo16() {
        return isEnabled() && (potionsStackTo16Config == null || potionsStackTo16Config.get());
    }

    public boolean isStewsAndSoupsStackTo16() {
        return isEnabled() && (stewsAndSoupsStackTo16Config == null || stewsAndSoupsStackTo16Config.get());
    }

    public boolean isEnderPearlsStackTo64() {
        return isEnabled() && (enderPearlsStackTo64Config == null || enderPearlsStackTo64Config.get());
    }

    public boolean isSnowballsStackTo64() {
        return isEnabled() && (snowballsStackTo64Config == null || snowballsStackTo64Config.get());
    }

    public boolean isEggsStackTo64() {
        return isEnabled() && (eggsStackTo64Config == null || eggsStackTo64Config.get());
    }

    public void ensureConfigFile() {
        Path configPath = FMLPaths.CONFIGDIR.get().resolve("onetoys").resolve("stacks.toml");
        if (!Files.exists(configPath)) {
            try {
                Files.createDirectories(configPath.getParent());
                Files.writeString(configPath, DEFAULT_CONFIG_TEMPLATE, StandardCharsets.UTF_8);
                Onetoys.LOGGER.info("Generated initial stack configuration at '{}'", configPath);
            } catch (IOException e) {
                Onetoys.LOGGER.error("Failed to write initial stack configuration to '{}'", configPath, e);
            }
        }
    }

    public void apply(ModifyDefaultComponentsEvent event) {
        if (!isEnabled()) {
            return;
        }

        Path configPath = FMLPaths.CONFIGDIR.get().resolve("onetoys").resolve("stacks.toml");
        ensureConfigFile();

        loadAndApplyStackSizes(event, configPath);
    }

    public void applyTagOverrides() {
        for (Map.Entry<Item, Integer> entry : originalStackSizes.entrySet()) {
            Item item = entry.getKey();
            int original = entry.getValue();
            var builder = DataComponentMap.builder().addAll(item.components());
            builder.set(DataComponents.MAX_STACK_SIZE, original);
            ((ItemAccessor) item).onetoys$setComponents(builder.build());
        }

        if (!isEnabled() || cachedTagRules.isEmpty()) {
            return;
        }

        for (Item item : BuiltInRegistries.ITEM) {
            ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
            String path = id.getPath();
            for (CustomStackRule.Tag rule : cachedTagRules) {
                if (rule.matches(item, id, path)) {
                    originalStackSizes.computeIfAbsent(item, i -> i.components().getOrDefault(DataComponents.MAX_STACK_SIZE, 64));
                    rule.applyDirect(item, id);
                }
            }
        }
    }

    private void loadAndApplyStackSizes(ModifyDefaultComponentsEvent event, Path configPath) {
        List<CustomStackRule> overrideRules = new ArrayList<>();
        Map<ResourceLocation, Integer> configuredStackSizes = new HashMap<>();

        try (CommentedFileConfig config = CommentedFileConfig.builder(configPath)
                .sync()
                .build()) {
            config.load();

            cachedTagRules.clear();

            // 1. Read [overrides] table
            Object overridesObj = config.get("overrides");
            if (overridesObj instanceof UnmodifiableConfig overridesConfig) {
                for (UnmodifiableConfig.Entry entry : overridesConfig.entrySet()) {
                    String pattern = entry.getKey();
                    Object val = entry.getValue();
                    if (val instanceof Number num) {
                        int size = num.intValue();
                        if (size >= 1 && size <= 99) {
                            CustomStackRule rule = CustomStackRule.parse(pattern + "=" + size);
                            if (rule != null) {
                                overrideRules.add(rule);
                                if (rule instanceof CustomStackRule.Tag tagRule) {
                                    cachedTagRules.add(tagRule);
                                }
                            }
                        } else {
                            Onetoys.LOGGER.warn("Invalid stack size '{}' for override '{}' in '{}' (must be 1-99)", size, pattern, configPath.getFileName());
                        }
                    }
                }
            }

            // 2. Read 'rules' list if present
            Object rulesObj = config.get("rules");
            if (rulesObj instanceof List<?> rulesList) {
                for (Object ruleEntry : rulesList) {
                    if (ruleEntry instanceof String entryStr) {
                        CustomStackRule rule = CustomStackRule.parse(entryStr);
                        if (rule != null) {
                            overrideRules.add(rule);
                            if (rule instanceof CustomStackRule.Tag tagRule) {
                                cachedTagRules.add(tagRule);
                            }
                        }
                    }
                }
            }

            // 3. Read per-mod tables (e.g. [minecraft], [modid])
            for (UnmodifiableConfig.Entry modEntry : config.entrySet()) {
                String modid = modEntry.getKey();
                if ("overrides".equals(modid) || "rules".equals(modid)) {
                    continue;
                }
                if (modEntry.getValue() instanceof UnmodifiableConfig modTable) {
                    for (UnmodifiableConfig.Entry itemEntry : modTable.entrySet()) {
                        String itemPath = itemEntry.getKey();
                        if (itemEntry.getValue() instanceof Number num) {
                            int size = num.intValue();
                            if (size >= 1 && size <= 99) {
                                configuredStackSizes.put(ResourceLocation.fromNamespaceAndPath(modid, itemPath), size);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            Onetoys.LOGGER.error("Failed to load stack configuration from '{}'", configPath, e);
        }

        boolean saddles = isSaddlesStackTo16();
        boolean boats = isBoatsStackTo16();
        boolean minecarts = isMinecartsStackTo16();
        boolean beds = isBedsStackTo16();
        boolean potions = isPotionsStackTo16();
        boolean stews = isStewsAndSoupsStackTo16();
        boolean pearls = isEnderPearlsStackTo64();
        boolean snowballs = isSnowballsStackTo64();
        boolean eggs = isEggsStackTo64();

        for (Item item : BuiltInRegistries.ITEM) {
            ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
            String path = id.getPath();

            // 1. Check [overrides] rules (highest priority)
            CustomStackRule matchedRule = null;
            for (CustomStackRule rule : overrideRules) {
                if (rule.matches(item, id, path)) {
                    matchedRule = rule;
                }
            }

            if (matchedRule != null) {
                matchedRule.apply(event, item, id);
                continue;
            }

            // 2. Check per-mod exact table from stacks.toml
            Integer configuredSize = configuredStackSizes.get(id);
            if (configuredSize != null) {
                modifyStackSize(event, item, id, configuredSize);
                continue;
            }

            // 3. Preset toggles
            if (saddles && item == Items.SADDLE) {
                modifyStackSize(event, item, id, 16);
            } else if (boats && (item instanceof BoatItem || path.endsWith("_boat") || path.endsWith("_raft"))) {
                modifyStackSize(event, item, id, 16);
            } else if (minecarts && (item instanceof MinecartItem || path.endsWith("minecart"))) {
                modifyStackSize(event, item, id, 16);
            } else if (beds && (item instanceof BedItem || path.endsWith("_bed"))) {
                modifyStackSize(event, item, id, 16);
            } else if (potions && (item == Items.POTION || item == Items.SPLASH_POTION || item == Items.LINGERING_POTION)) {
                modifyStackSize(event, item, id, 16);
            } else if (stews && (item == Items.MUSHROOM_STEW || item == Items.BEETROOT_SOUP || item == Items.RABBIT_STEW || item == Items.SUSPICIOUS_STEW)) {
                modifyStackSize(event, item, id, 16);
            } else if (pearls && item == Items.ENDER_PEARL) {
                modifyStackSize(event, item, id, 64);
            } else if (snowballs && item == Items.SNOWBALL) {
                modifyStackSize(event, item, id, 64);
            } else if (eggs && item == Items.EGG) {
                modifyStackSize(event, item, id, 64);
            }
        }
    }

    private void modifyStackSize(ModifyDefaultComponentsEvent event, Item item, ResourceLocation id, int target) {
        int current = item.components().getOrDefault(DataComponents.MAX_STACK_SIZE, 64);
        if (current != target) {
            event.modify(item, b -> b.set(DataComponents.MAX_STACK_SIZE, target));
            Onetoys.LOGGER.info("Applied preset stack size for '{}': {} -> {}", id, current, target);
        }
    }
}
