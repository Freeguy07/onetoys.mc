package com.onetoys.module.gameplay.durability;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.electronwill.nightconfig.core.UnmodifiableConfig;
import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.electronwill.nightconfig.core.io.WritingMode;
import com.onetoys.Onetoys;
import com.onetoys.mixin.ItemAccessor;
import com.onetoys.registry.OnetoysTags;

import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TieredItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.component.Unbreakable;
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

    private record OriginalDurability(Integer maxDamage, Unbreakable unbreakable) {}

    private ModConfigSpec.BooleanValue enabledConfig;
    private ModConfigSpec.BooleanValue autoSyncConfig;
    private ModConfigSpec.ConfigValue<List<? extends String>> unbreakableItemsConfig;
    private ModConfigSpec.BooleanValue showUnbreakableTooltipConfig;

    private final List<CustomDurabilityRule> cachedOverrideRules = new ArrayList<>();
    private final List<CustomDurabilityRule.Tag> cachedTagRules = new ArrayList<>();
    private final Map<Item, OriginalDurability> originalDurabilities = new HashMap<>();

    public void buildConfig(ModConfigSpec.Builder builder) {
        builder.comment("Sub-category allowing custom durability configuration for items.",
                        "Item durability values are automatically cataloged and configured in 'config/Onetoys/durability.toml'.")
                .translation("onetoys.configuration.gameplay_tweaks.durability_tweaks")
                .push("durability_tweaks");

        enabledConfig = builder
                .comment("Enable custom durability adjustments defined in 'config/Onetoys/durability.toml'.")
                .translation("onetoys.configuration.gameplay_tweaks.durability_tweaks.enabled")
                .define("enabled", true);

        autoSyncConfig = builder
                .comment("Automatically discover and append newly installed mod items to 'config/Onetoys/durability.toml'.")
                .translation("onetoys.configuration.gameplay_tweaks.durability_tweaks.auto_sync")
                .define("auto_sync", true);

        unbreakableItemsConfig = builder
                .comment("List of item IDs, tags (starting with '#'), or wildcard patterns that will be made completely unbreakable.",
                        "Examples: ['minecraft:elytra', '#c:tools/netherite', '*golden_*']")
                .translation("onetoys.configuration.gameplay_tweaks.durability_tweaks.unbreakable_items")
                .defineListAllowEmpty("unbreakable_items", List.of(
                        "#onetoys:unbreakable",
                        "minecraft:wooden_sword",
                        "minecraft:wooden_shovel",
                        "minecraft:wooden_pickaxe",
                        "minecraft:wooden_axe",
                        "minecraft:wooden_hoe"
                ), () -> "", obj -> obj instanceof String);

        showUnbreakableTooltipConfig = builder
                .comment("When enabled, items made unbreakable display the 'Unbreakable' line in their tooltip.")
                .translation("onetoys.configuration.gameplay_tweaks.durability_tweaks.show_unbreakable_tooltip")
                .define("show_unbreakable_tooltip", true);

        builder.pop();
    }

    public boolean isEnabled() {
        return enabledConfig == null || enabledConfig.get();
    }

    public boolean isAutoSyncEnabled() {
        return autoSyncConfig == null || autoSyncConfig.get();
    }

    public List<? extends String> getUnbreakableItems() {
        return unbreakableItemsConfig != null ? unbreakableItemsConfig.get() : List.of();
    }

    public boolean shouldShowUnbreakableTooltip() {
        return showUnbreakableTooltipConfig == null || showUnbreakableTooltipConfig.get();
    }

    public void apply(ModifyDefaultComponentsEvent event) {
        if (!isEnabled()) {
            return;
        }

        Path configPath = FMLPaths.CONFIGDIR.get().resolve("onetoys").resolve("durability.toml");

        try {
            Files.createDirectories(configPath.getParent());
        } catch (IOException e) {
            Onetoys.LOGGER.error("Failed to create directory for durability configuration: {}", configPath.getParent(), e);
            return;
        }

        // 1. Discover all damageable items in BuiltInRegistries.ITEM grouped by mod namespace
        Map<String, List<ItemDefault>> itemsByMod = discoverDamageableItems();

        // 2. If the file does not exist, generate initial formatted TOML file
        if (!Files.exists(configPath)) {
            generateInitialConfigFile(configPath, itemsByMod);
            Onetoys.LOGGER.info("Generated initial durability configuration at '{}' with {} items across {} mods",
                    configPath, countTotalItems(itemsByMod), itemsByMod.size());
        }

        // 3. Read user values, auto-sync new items if enabled, and apply
        loadAndApplyDurabilities(event, configPath, itemsByMod);
    }

    public void applyTagOverrides() {
        // Restore original values for all items previously modified by tag overrides
        for (Map.Entry<Item, OriginalDurability> entry : originalDurabilities.entrySet()) {
            Item item = entry.getKey();
            OriginalDurability original = entry.getValue();
            var builder = DataComponentMap.builder().addAll(item.components());
            builder.set(DataComponents.MAX_DAMAGE, original.maxDamage());
            builder.set(DataComponents.UNBREAKABLE, original.unbreakable());
            ((ItemAccessor) item).onetoys$setComponents(builder.build());
        }
        originalDurabilities.clear();

        if (!isEnabled() || cachedTagRules.isEmpty()) {
            return;
        }

        boolean showTooltip = shouldShowUnbreakableTooltip();
        for (Item item : BuiltInRegistries.ITEM) {
            ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
            String path = id.getPath();
            for (CustomDurabilityRule rule : cachedOverrideRules) {
                if (rule.matches(item, id, path)) {
                    if (rule instanceof CustomDurabilityRule.Tag tagRule) {
                        originalDurabilities.computeIfAbsent(item, i -> new OriginalDurability(
                                i.components().get(DataComponents.MAX_DAMAGE),
                                i.components().get(DataComponents.UNBREAKABLE)
                        ));
                        tagRule.applyDirect(item, id, showTooltip);
                    }
                    break;
                }
            }
        }
    }

    private Map<String, List<ItemDefault>> discoverDamageableItems() {
        Map<String, List<ItemDefault>> rawMap = new HashMap<>();

        for (Item item : BuiltInRegistries.ITEM) {
            int maxDamage = item.components().getOrDefault(DataComponents.MAX_DAMAGE, 0);
            if (maxDamage > 0) {
                ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
                int defaultDurability = calculateDefaultDurability(item, id, id.getPath(), maxDamage);
                rawMap.computeIfAbsent(id.getNamespace(), k -> new ArrayList<>())
                        .add(new ItemDefault(id.getPath(), defaultDurability));
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

    private void generateInitialConfigFile(Path path, Map<String, List<ItemDefault>> itemsByMod) {
        StringBuilder toml = new StringBuilder();
        toml.append("# ====================================================================\n");
        toml.append("# Onetoys - Durability Tweaks Configuration\n");
        toml.append("#\n");
        toml.append("# Defines custom max durability values for damageable items across all mods.\n");
        toml.append("# Format:\n");
        toml.append("#   item_path = durability_integer\n");
        toml.append("#\n");
        toml.append("# Special value:\n");
        toml.append("#   -1 = Makes the item completely unbreakable (like the 'unbreakable_items' setting)\n");
        toml.append("#\n");
        toml.append("# Notes:\n");
        toml.append("#   - Items are organized into tables by their mod namespace, e.g. [minecraft].\n");
        toml.append("#   - You can add custom overrides under the special [overrides] table using:\n");
        toml.append("#       exact IDs:         \"minecraft:diamond_sword\" = 2500\n");
        toml.append("#       wildcard patterns: \"*golden_*\" = 500\n");
        toml.append("#       item tags:         \"#c:tools/netherite\" = -1\n");
        toml.append("#   - When auto_sync is enabled, newly discovered mod items will be automatically\n");
        toml.append("#     appended here with their default values on next startup.\n");
        toml.append("# ====================================================================\n\n");

        toml.append("# --------------------------------------------------------------------\n");
        toml.append("# Custom Overrides (Exact IDs, Wildcards, and Item Tags)\n");
        toml.append("# --------------------------------------------------------------------\n");
        toml.append("[overrides]\n");
        toml.append("# \"minecraft:elytra\" = -1\n");
        toml.append("# \"*wooden_*\" = 120\n\n");

        for (Map.Entry<String, List<ItemDefault>> modEntry : itemsByMod.entrySet()) {
            String modId = modEntry.getKey();
            List<ItemDefault> items = modEntry.getValue();

            toml.append("# --------------------------------------------------------------------\n");
            toml.append("# Mod: ").append(modId).append("\n");
            toml.append("# --------------------------------------------------------------------\n");
            toml.append("[").append(modId).append("]\n");

            for (ItemDefault item : items) {
                toml.append(item.path()).append(" = ").append(item.defaultDurability()).append("\n");
            }
            toml.append("\n");
        }

        try {
            Files.writeString(path, toml.toString(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            Onetoys.LOGGER.error("Failed to write initial durability configuration to '{}'", path, e);
        }
    }

    private void loadAndApplyDurabilities(ModifyDefaultComponentsEvent event, Path configPath, Map<String, List<ItemDefault>> itemsByMod) {
        List<CustomDurabilityRule> overrideRules = new ArrayList<>();
        Map<ResourceLocation, Integer> configuredDurabilities = new HashMap<>();

        try (CommentedFileConfig config = CommentedFileConfig.builder(configPath)
                .sync()
                .writingMode(WritingMode.REPLACE)
                .build()) {
            config.load();

            cachedOverrideRules.clear();
            cachedTagRules.clear();

            // 1. Read [overrides] table
            Object overridesObj = config.get("overrides");
            if (overridesObj instanceof UnmodifiableConfig overridesConfig) {
                for (UnmodifiableConfig.Entry entry : overridesConfig.entrySet()) {
                    String pattern = entry.getKey();
                    Object val = entry.getValue();
                    if (val instanceof Number num) {
                        int dur = num.intValue();
                        CustomDurabilityRule rule = CustomDurabilityRule.parse(pattern + "=" + dur);
                        if (rule != null) {
                            overrideRules.add(rule);
                            if (rule instanceof CustomDurabilityRule.Tag tagRule) {
                                cachedTagRules.add(tagRule);
                            }
                        }
                    }
                }
            }

            // 2. Read mod tables
            for (String modId : itemsByMod.keySet()) {
                Object modTableObj = config.get(modId);
                if (modTableObj instanceof UnmodifiableConfig modTable) {
                    for (UnmodifiableConfig.Entry entry : modTable.entrySet()) {
                        String itemPath = entry.getKey();
                        Object val = entry.getValue();
                        if (val instanceof Number num) {
                            configuredDurabilities.put(ResourceLocation.fromNamespaceAndPath(modId, itemPath), num.intValue());
                        }
                    }
                }
            }

            // 3. Auto-sync newly discovered items if enabled
            if (isAutoSyncEnabled()) {
                boolean modified = false;
                for (Map.Entry<String, List<ItemDefault>> modEntry : itemsByMod.entrySet()) {
                    String modId = modEntry.getKey();
                    for (ItemDefault item : modEntry.getValue()) {
                        ResourceLocation key = ResourceLocation.fromNamespaceAndPath(modId, item.path());
                        if (!configuredDurabilities.containsKey(key)) {
                            config.set(List.of(modId, item.path()), item.defaultDurability());
                            configuredDurabilities.put(key, item.defaultDurability());
                            modified = true;
                        }
                    }
                }
                if (modified) {
                    config.save();
                    Onetoys.LOGGER.info("Auto-synced new damageable items into '{}'", configPath);
                }
            }

        } catch (Exception e) {
            Onetoys.LOGGER.error("Failed to load durability configuration from '{}'", configPath, e);
            return;
        }

        // 4. Also register unbreakable items from main forge config into override rules
        for (String pattern : getUnbreakableItems()) {
            CustomDurabilityRule rule = CustomDurabilityRule.parse(pattern + "=-1");
            if (rule != null) {
                overrideRules.add(rule);
                if (rule instanceof CustomDurabilityRule.Tag tagRule) {
                    cachedTagRules.add(tagRule);
                }
            }
        }

        // 5. Always include built-in unbreakable tags as rules
        CustomDurabilityRule unbreakableTag = new CustomDurabilityRule.Tag("#onetoys:unbreakable", OnetoysTags.Items.UNBREAKABLE, -1);
        overrideRules.add(unbreakableTag);
        cachedTagRules.add((CustomDurabilityRule.Tag) unbreakableTag);

        CustomDurabilityRule unbreakableItemsTag = new CustomDurabilityRule.Tag("#onetoys:unbreakable_items", OnetoysTags.Items.UNBREAKABLE_ITEMS, -1);
        overrideRules.add(unbreakableItemsTag);
        cachedTagRules.add((CustomDurabilityRule.Tag) unbreakableItemsTag);

        CustomDurabilityRule commonUnbreakableTag = new CustomDurabilityRule.Tag("#c:unbreakable", OnetoysTags.Items.COMMON_UNBREAKABLE, -1);
        overrideRules.add(commonUnbreakableTag);
        cachedTagRules.add((CustomDurabilityRule.Tag) commonUnbreakableTag);

        cachedOverrideRules.addAll(overrideRules);

        // 6. Apply modifiers to registered items
        boolean showTooltip = shouldShowUnbreakableTooltip();
        for (Item item : BuiltInRegistries.ITEM) {
            ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
            String path = id.getPath();

            // First check override rules (exact > wildcard)
            boolean overridden = false;
            for (CustomDurabilityRule rule : overrideRules) {
                if (!(rule instanceof CustomDurabilityRule.Tag) && rule.matches(item, id, path)) {
                    rule.apply(event, item, id, showTooltip);
                    overridden = true;
                    break;
                }
            }

            if (!overridden) {
                Integer customDur = configuredDurabilities.get(id);
                if (customDur != null) {
                    new CustomDurabilityRule.Exact(id, customDur).apply(event, item, id, showTooltip);
                }
            }
        }
    }

    private int calculateDefaultDurability(Item item, ResourceLocation id, String path, int vanillaMaxDamage) {
        if (item instanceof TieredItem tiered) {
            Tier tier = tiered.getTier();
            if (tier == Tiers.WOOD || tier == Tiers.GOLD) return 60;
            if (tier == Tiers.STONE) return 132;
            if (tier == Tiers.IRON) return 250;
            if (tier == Tiers.DIAMOND) return 1561;
            if (tier == Tiers.NETHERITE) return 2031;
        }

        if (item instanceof ArmorItem armor) {
            ArmorItem.Type type = armor.getType();
            if (type == ArmorItem.Type.BOOTS) return 430;
            if (type == ArmorItem.Type.LEGGINGS) return 496;
            if (type == ArmorItem.Type.CHESTPLATE) return 529;
            if (type == ArmorItem.Type.HELMET) return 364;
        }

        return vanillaMaxDamage;
    }

    private int countTotalItems(Map<String, List<ItemDefault>> itemsByMod) {
        int total = 0;
        for (List<ItemDefault> list : itemsByMod.values()) {
            total += list.size();
        }
        return total;
    }
}
