package com.orehall.module.gameplay.durability;

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
import com.orehall.Orehall;

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

    private ModConfigSpec.BooleanValue enabledConfig;
    private ModConfigSpec.BooleanValue autoSyncConfig;
    private ModConfigSpec.ConfigValue<List<? extends String>> unbreakableItemsConfig;
    private ModConfigSpec.BooleanValue showUnbreakableTooltipConfig;

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

        unbreakableItemsConfig = builder
                .comment("List of item IDs, tags (starting with '#'), or wildcard patterns that will be made completely unbreakable.",
                        "Examples: ['minecraft:elytra', '#c:tools/netherite', '*golden_*']")
                .translation("orehall.configuration.gameplay_tweaks.durability_tweaks.unbreakable_items")
                .defineListAllowEmpty("unbreakable_items", List.of(), () -> "", obj -> obj instanceof String);

        showUnbreakableTooltipConfig = builder
                .comment("When enabled, items made unbreakable display the 'Unbreakable' line in their tooltip.")
                .translation("orehall.configuration.gameplay_tweaks.durability_tweaks.show_unbreakable_tooltip")
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
        }

        // 3. Read user values, auto-sync new items if enabled, and apply
        loadAndApplyDurabilities(event, configPath, itemsByMod);
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
        sb.append("# Values default to Orehall's balanced stack-based tiers (multiples of 64):\n");
        sb.append("#   Tools: Wood (64), Stone (128), Gold (288), Iron (576), Diamond (2304), Netherite (3456)\n");
        sb.append("#   Armor: Leather (64), Gold (128), Chain (192), Iron (256), Diamond (512), Netherite (768)\n");
        sb.append("# Any item whose value differs from default will have its durability adjusted.\n");
        sb.append("# Set any item's durability to -1 to make it completely unbreakable.\n");
        sb.append("# Changes take effect upon restarting the game / server.\n");
        sb.append("# ==============================================================================\n\n");

        sb.append("# Optional wildcard / pattern / tag overrides (highest priority)\n");
        sb.append("# Format: \"target_pattern\" = durability\n");
        sb.append("# Set durability to -1 to make matching items completely unbreakable.\n");
        sb.append("# Examples:\n");
        sb.append("# \"caverns_and_chasms:sanguine_*\" = 450\n");
        sb.append("# \"*copper_*\" = 200\n");
        sb.append("# \"#c:tools/silver\" = 160\n");
        sb.append("# \"minecraft:elytra\" = -1\n");
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

            // Read unbreakable items whitelist from orehall.toml
            List<CustomDurabilityRule> whitelistRules = new ArrayList<>();
            for (String entry : getUnbreakableItems()) {
                if (entry != null && !entry.isBlank()) {
                    CustomDurabilityRule rule = CustomDurabilityRule.parse(entry.trim() + "=-1");
                    if (rule != null) {
                        whitelistRules.add(rule);
                    }
                }
            }

            // Read [overrides] table
            List<CustomDurabilityRule> overrideRules = new ArrayList<>();
            Object overridesObj = config.get("overrides");
            if (overridesObj instanceof UnmodifiableConfig overridesConfig) {
                for (UnmodifiableConfig.Entry entry : overridesConfig.entrySet()) {
                    String pattern = entry.getKey();
                    Object val = entry.getValue();
                    if (val instanceof Number num && (num.intValue() > 0 || num.intValue() == -1)) {
                        CustomDurabilityRule rule = CustomDurabilityRule.parse(pattern + "=" + num.intValue());
                        if (rule != null) {
                            overrideRules.add(rule);
                        }
                    }
                }
            }
            overrideRules.addAll(whitelistRules);

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
            boolean showTooltip = shouldShowUnbreakableTooltip();
            for (Item item : BuiltInRegistries.ITEM) {
                ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
                String path = id.getPath();
                int maxDamage = item.components().getOrDefault(DataComponents.MAX_DAMAGE, 0);

                // 1. Check [overrides] and whitelist rules first (highest priority)
                CustomDurabilityRule matchedRule = null;
                for (CustomDurabilityRule rule : overrideRules) {
                    if (rule.matches(item, id, path)) {
                        matchedRule = rule;
                    }
                }

                if (matchedRule != null) {
                    if (matchedRule.durability() == -1 || maxDamage > 0) {
                        matchedRule.apply(event, item, id, showTooltip);
                    }
                    continue;
                }

                if (maxDamage <= 0) {
                    continue;
                }

                // 2. Apply configured value from durability.toml
                Integer configuredVal = configuredDurabilities.get(id);
                if (configuredVal != null) {
                    if (configuredVal == -1) {
                        event.modify(item, b -> b.set(DataComponents.UNBREAKABLE, new Unbreakable(showTooltip)));
                        Orehall.LOGGER.info("Adjusting durability for '{}': marking UNBREAKABLE (-1)", id);
                    } else if (configuredVal > 0 && configuredVal != maxDamage) {
                        final int targetVal = configuredVal;
                        event.modify(item, b -> b.set(DataComponents.MAX_DAMAGE, targetVal));
                        Orehall.LOGGER.info("Adjusting durability for '{}': {} -> {}", id, maxDamage, targetVal);
                    }
                }
            }

            // 3. Handle exact [overrides] targeting non-damageable items (e.g. stick = 50 or stick = -1)
            for (CustomDurabilityRule rule : overrideRules) {
                if (rule instanceof CustomDurabilityRule.Exact exact) {
                    BuiltInRegistries.ITEM.getHolder(exact.targetId()).ifPresent(holder -> {
                        Item item = holder.value();
                        int max = item.components().getOrDefault(DataComponents.MAX_DAMAGE, 0);
                        if (max <= 0) {
                            if (exact.durability() == -1) {
                                event.modify(item, b -> b.set(DataComponents.UNBREAKABLE, new Unbreakable(showTooltip)));
                                Orehall.LOGGER.info("Applied exact unbreakable override to non-damageable item '{}'", exact.targetId());
                            } else {
                                event.modify(item, b -> b.set(DataComponents.MAX_DAMAGE, exact.durability()));
                                Orehall.LOGGER.info("Applied exact custom durability override to non-damageable item '{}': {}", exact.targetId(), exact.durability());
                            }
                        }
                    });
                }
            }

        } catch (Exception e) {
            Orehall.LOGGER.error("Failed to load or apply durability configuration from '{}'", configPath, e);
        }
    }

    /**
     * Calculates default balanced stack-based durability for an item:
     * Tools & Weapons:
     *   Wood: 64 (1 stack), Stone: 128 (2 stacks), Gold: 288 (4.5 stacks),
     *   Iron: 576 (9 stacks), Diamond: 2304 (36 stacks), Netherite: 3456 (54 stacks).
     * Armor (balanced stack tiers to avoid god-tier immortality):
     *   Leather / Wolf: 64 (1 stack), Gold: 128 (2 stacks), Chainmail: 192 (3 stacks),
     *   Iron / Turtle: 256 (4 stacks), Diamond: 512 (8 stacks), Netherite: 768 (12 stacks).
     * Extended to other materials, weapons, utility tools, and fallback rounding for modded items.
     */
    public int calculateDefaultDurability(Item item, ResourceLocation id, String path, int currentMaxDamage) {
        if (currentMaxDamage <= 0) {
            return 0;
        }

        String lowerPath = path.toLowerCase();
        boolean isArmor = item instanceof ArmorItem
                || lowerPath.contains("helmet") || lowerPath.contains("chestplate")
                || lowerPath.contains("leggings") || lowerPath.contains("boots")
                || lowerPath.contains("armor");

        // 1. Check Armor Items (balanced stack tiers)
        if (isArmor) {
            if (item instanceof ArmorItem armor) {
                String mat = armor.getMaterial().unwrapKey().map(k -> k.location().getPath().toLowerCase()).orElse("");
                if (mat.equals("leather") || mat.equals("armadillo")) return 64;
                if (mat.equals("gold") || mat.equals("golden")) return 128;
                if (mat.equals("chainmail") || mat.equals("chain") || mat.equals("silver")) return 192;
                if (mat.equals("iron") || mat.equals("copper") || mat.equals("bronze") || mat.equals("turtle")) return 256;
                if (mat.equals("diamond") || mat.equals("steel") || mat.equals("platinum")) return 512;
                if (mat.equals("netherite")) return 768;
            }

            // Path heuristics for modded or non-ArmorItem armors
            if (lowerPath.contains("netherite")) return 768;
            if (lowerPath.contains("diamond") || lowerPath.contains("platinum") || lowerPath.contains("steel")) return 512;
            if (lowerPath.contains("iron") || lowerPath.contains("bronze") || lowerPath.contains("copper") || lowerPath.contains("turtle")) return 256;
            if (lowerPath.contains("chainmail") || lowerPath.contains("chain_") || lowerPath.contains("silver")) return 192;
            if (lowerPath.contains("golden") || lowerPath.contains("gold_") || lowerPath.contains("_gold")) return 128;
            if (lowerPath.contains("leather") || lowerPath.contains("wolf_armor") || lowerPath.contains("armadillo")) return 64;
        }

        // 2. Check TieredItem (swords, pickaxes, axes, shovels, hoes)
        if (item instanceof TieredItem tiered) {
            Tier tier = tiered.getTier();
            if (tier == Tiers.WOOD) return 64;
            if (tier == Tiers.STONE) return 128;
            if (tier == Tiers.GOLD) return 288;
            if (tier == Tiers.IRON) return 576;
            if (tier == Tiers.DIAMOND) return 2304;
            if (tier == Tiers.NETHERITE) return 3456;
        }

        // 3. Specific weapons and utility tools (checked before material keywords so "flint_and_steel" is not matched as "steel")
        if (lowerPath.contains("flint_and_steel")) return 64;
        if (lowerPath.contains("fishing_rod")) return 288;
        if (lowerPath.contains("trident") || lowerPath.contains("mace")) return 2304;
        if (lowerPath.contains("crossbow") || lowerPath.contains("shield") || lowerPath.contains("elytra")) return 576;
        if (lowerPath.endsWith("bow") || lowerPath.contains("bow_")) return 384;
        if (lowerPath.contains("shears")) return 256;
        if (lowerPath.contains("brush")
                || lowerPath.contains("carrot_on_a_stick") || lowerPath.contains("warped_fungus_on_a_stick")) return 64;

        // 4. Check path for tool/weapon material keywords (modded tools or non-standard class hierarchies)
        if (lowerPath.contains("netherite")) return 3456;
        if (lowerPath.contains("diamond") || lowerPath.contains("platinum")) return 2304;
        if (lowerPath.contains("steel")) return 1152;
        if (lowerPath.contains("iron") || lowerPath.contains("bronze")) return 576;
        if (lowerPath.contains("silver") || lowerPath.contains("golden") || lowerPath.contains("gold_") || lowerPath.contains("_gold") || lowerPath.contains("chainmail") || lowerPath.contains("chain_")) return 288;
        if (lowerPath.contains("copper")) return 256;
        if (lowerPath.contains("stone")) return 128;
        if (lowerPath.contains("wooden") || lowerPath.contains("wood_") || lowerPath.contains("_wood")) return 64;

        // 5. Fallback for any other damageable items: round to nearest stack multiple (min 1 stack = 64)
        int stacks = Math.max(1, Math.round(currentMaxDamage / 64.0f));
        return stacks * 64;
    }
}
