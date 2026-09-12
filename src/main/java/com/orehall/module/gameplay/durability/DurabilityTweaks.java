package com.orehall.module.gameplay.durability;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.electronwill.nightconfig.core.UnmodifiableConfig;
import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.electronwill.nightconfig.core.io.WritingMode;
import com.orehall.Orehall;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.event.ModifyDefaultComponentsEvent;

public class DurabilityTweaks {

    public record ItemDefault(String path, int defaultDurability) implements Comparable<ItemDefault> {
        @Override
        public int compareTo(ItemDefault o) {
            return this.path.compareTo(o.path);
        }
    }

    private ModConfigSpec.BooleanValue enabledConfig;
    private ModConfigSpec.BooleanValue autoSyncConfig;

    public void buildConfig(ModConfigSpec.Builder builder) {
        builder.comment("Sub-category allowing custom durability configuration for items.",
                        "Item durability values are automatically cataloged and configured in 'config/orehall/durability.toml'.")
                .translation("orehall.configuration.gameplay_tweaks.durability_tweaks")
                .push("durability_tweaks");

        enabledConfig = builder
                .comment("Enable custom durability adjustments defined in 'config/orehall/durability.toml'.")
                .translation("orehall.configuration.gameplay_tweaks.durability_tweaks.enabled")
                .define("enabled", true);

        autoSyncConfig = builder
                .comment("Automatically discover and append newly installed mod items to 'config/orehall/durability.toml'.")
                .translation("orehall.configuration.gameplay_tweaks.durability_tweaks.auto_sync")
                .define("auto_sync", true);

        builder.pop();
    }

    public boolean isEnabled() {
        return enabledConfig == null || enabledConfig.get();
    }

    public boolean isAutoSyncEnabled() {
        return autoSyncConfig == null || autoSyncConfig.get();
    }

    public void apply(ModifyDefaultComponentsEvent event) {
        if (!isEnabled()) {
            return;
        }

        Path configPath = FMLPaths.CONFIGDIR.get().resolve("orehall").resolve("durability.toml");

        try {
            Files.createDirectories(configPath.getParent());
        } catch (IOException e) {
            Orehall.LOGGER.error("Failed to create directory for durability configuration: {}", configPath.getParent(), e);
            return;
        }

        // 1. Discover all damageable items in BuiltInRegistries.ITEM grouped by mod namespace
        Map<String, List<ItemDefault>> itemsByMod = discoverDamageableItems();

        // 2. If the file does not exist, generate initial formatted TOML file
        if (!Files.exists(configPath)) {
            generateInitialConfigFile(configPath, itemsByMod);
            Orehall.LOGGER.info("Generated initial durability configuration at '{}' with {} items across {} mods",
                    configPath, countTotalItems(itemsByMod), itemsByMod.size());
            return;
        }

        // 3. File exists: read user values, auto-sync new items if enabled, and apply
        loadAndApplyDurabilities(event, configPath, itemsByMod);
    }

    private Map<String, List<ItemDefault>> discoverDamageableItems() {
        Map<String, List<ItemDefault>> rawMap = new HashMap<>();

        for (Item item : BuiltInRegistries.ITEM) {
            int maxDamage = item.components().getOrDefault(DataComponents.MAX_DAMAGE, 0);
            if (maxDamage > 0) {
                ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
                rawMap.computeIfAbsent(id.getNamespace(), k -> new ArrayList<>())
                        .add(new ItemDefault(id.getPath(), maxDamage));
            }
        }

        for (List<ItemDefault> list : rawMap.values()) {
            list.sort(null);
        }

        Map<String, List<ItemDefault>> sortedMap = new LinkedHashMap<>();
        if (rawMap.containsKey("minecraft")) {
            sortedMap.put("minecraft", rawMap.get("minecraft"));
        }
        List<String> otherMods = new ArrayList<>(rawMap.keySet());
        otherMods.remove("minecraft");
        otherMods.sort(null);
        for (String mod : otherMods) {
            sortedMap.put(mod, rawMap.get(mod));
        }

        return sortedMap;
    }

    private int countTotalItems(Map<String, List<ItemDefault>> itemsByMod) {
        int count = 0;
        for (List<ItemDefault> list : itemsByMod.values()) {
            count += list.size();
        }
        return count;
    }

    private void generateInitialConfigFile(Path path, Map<String, List<ItemDefault>> itemsByMod) {
        StringBuilder sb = new StringBuilder();
        sb.append("# ==============================================================================\n");
        sb.append("# Orehall - Durability Configuration\n");
        sb.append("# Automatically generated and synchronized with all installed mods.\n");
        sb.append("# Any item whose value differs from default will have its durability adjusted.\n");
        sb.append("# Changes take effect upon restarting the game / server.\n");
        sb.append("# ==============================================================================\n\n");

        sb.append("# Optional wildcard / pattern / tag overrides (highest priority)\n");
        sb.append("# Format: \"target_pattern\" = durability\n");
        sb.append("# Examples:\n");
        sb.append("# \"caverns_and_chasms:sanguine_*\" = 450\n");
        sb.append("# \"*copper_*\" = 200\n");
        sb.append("# \"#c:tools/silver\" = 160\n");
        sb.append("[overrides]\n\n");

        for (Map.Entry<String, List<ItemDefault>> entry : itemsByMod.entrySet()) {
            String modid = entry.getKey();
            sb.append("[").append(modid).append("]\n");
            for (ItemDefault item : entry.getValue()) {
                sb.append("    ").append(item.path()).append(" = ").append(item.defaultDurability()).append("\n");
            }
            sb.append("\n");
        }

        try {
            Files.writeString(path, sb.toString(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            Orehall.LOGGER.error("Failed to write initial durability configuration to '{}'", path, e);
        }
    }

    private void loadAndApplyDurabilities(ModifyDefaultComponentsEvent event, Path configPath, Map<String, List<ItemDefault>> itemsByMod) {
        try (CommentedFileConfig config = CommentedFileConfig.builder(configPath)
                .sync()
                .writingMode(WritingMode.REPLACE)
                .build()) {

            config.load();

            // Read [overrides] table
            List<CustomDurabilityRule> overrideRules = new ArrayList<>();
            Object overridesObj = config.get("overrides");
            if (overridesObj instanceof UnmodifiableConfig overridesConfig) {
                for (UnmodifiableConfig.Entry entry : overridesConfig.entrySet()) {
                    String pattern = entry.getKey();
                    Object val = entry.getValue();
                    if (val instanceof Number num && num.intValue() > 0) {
                        CustomDurabilityRule rule = CustomDurabilityRule.parse(pattern + "=" + num.intValue());
                        if (rule != null) {
                            overrideRules.add(rule);
                        }
                    }
                }
            }

            // Read all configured per-item durabilities
            Map<ResourceLocation, Integer> configuredDurabilities = new HashMap<>();
            for (UnmodifiableConfig.Entry modEntry : config.entrySet()) {
                String modid = modEntry.getKey();
                if ("overrides".equals(modid)) {
                    continue;
                }
                if (modEntry.getValue() instanceof UnmodifiableConfig modTable) {
                    for (UnmodifiableConfig.Entry itemEntry : modTable.entrySet()) {
                        String itemPath = itemEntry.getKey();
                        if (itemEntry.getValue() instanceof Number num) {
                            configuredDurabilities.put(ResourceLocation.fromNamespaceAndPath(modid, itemPath), num.intValue());
                        }
                    }
                }
            }

            // Auto-sync newly installed mod items if enabled
            if (isAutoSyncEnabled()) {
                boolean modified = false;
                int newCount = 0;
                for (Map.Entry<String, List<ItemDefault>> entry : itemsByMod.entrySet()) {
                    String modid = entry.getKey();
                    for (ItemDefault item : entry.getValue()) {
                        List<String> keyPath = List.of(modid, item.path());
                        if (!config.contains(keyPath)) {
                            config.set(keyPath, item.defaultDurability());
                            modified = true;
                            newCount++;
                        }
                    }
                }
                if (modified) {
                    config.save();
                    Orehall.LOGGER.info("Synchronized {} new mod items into '{}'", newCount, configPath.getFileName());
                }
            }

            // Single pass over BuiltInRegistries.ITEM to apply modified durabilities
            for (Item item : BuiltInRegistries.ITEM) {
                int maxDamage = item.components().getOrDefault(DataComponents.MAX_DAMAGE, 0);
                if (maxDamage <= 0) {
                    continue;
                }

                ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
                String path = id.getPath();

                // 1. Check [overrides] rules first (highest priority)
                CustomDurabilityRule matchedRule = null;
                for (CustomDurabilityRule rule : overrideRules) {
                    if (rule.matches(item, id, path)) {
                        matchedRule = rule;
                    }
                }

                if (matchedRule != null) {
                    matchedRule.apply(event, item, id);
                } else {
                    // 2. Apply configured value from durability.toml if changed from current max damage
                    Integer configuredVal = configuredDurabilities.get(id);
                    if (configuredVal != null && configuredVal > 0 && configuredVal != maxDamage) {
                        final int targetVal = configuredVal;
                        event.modify(item, b -> b.set(DataComponents.MAX_DAMAGE, targetVal));
                        Orehall.LOGGER.info("Adjusting durability for '{}': {} -> {}", id, maxDamage, targetVal);
                    }
                }
            }

            // 3. Handle exact [overrides] targeting non-damageable items (e.g. stick = 50)
            for (CustomDurabilityRule rule : overrideRules) {
                if (rule instanceof CustomDurabilityRule.Exact exact) {
                    BuiltInRegistries.ITEM.getHolder(exact.targetId()).ifPresent(holder -> {
                        Item item = holder.value();
                        int max = item.components().getOrDefault(DataComponents.MAX_DAMAGE, 0);
                        if (max <= 0) {
                            event.modify(item, b -> b.set(DataComponents.MAX_DAMAGE, exact.durability()));
                            Orehall.LOGGER.info("Applied exact custom durability override to non-damageable item '{}': {}", exact.targetId(), exact.durability());
                        }
                    });
                }
            }

        } catch (Exception e) {
            Orehall.LOGGER.error("Failed to load or apply durability configuration from '{}'", configPath, e);
        }
    }
}
