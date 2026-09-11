package com.orehall.module.gameplay.durability;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.event.ModifyDefaultComponentsEvent;

public class DurabilityTweaks {
    private final Map<String, DurabilityGroup> toolGroups = new LinkedHashMap<>();
    private final Map<String, DurabilityGroup> armorGroups = new LinkedHashMap<>();
    private final Map<String, DurabilityGroup> miscGroups = new LinkedHashMap<>();
    private final Map<String, DurabilityGroup> groups = new LinkedHashMap<>();
    private ModConfigSpec.ConfigValue<List<? extends String>> customDurabilityConfig;
    private ModConfigSpec.BooleanValue enabledConfig;

    public DurabilityTweaks() {
        initGroups();
        discoverAndCategorizeItems();
    }

    private void initGroups() {
        // Tool Tiers
        addToolGroup(new DurabilityGroup("wood_tools", "Wood Tools", true, 59));
        addToolGroup(new DurabilityGroup("stone_tools", "Stone Tools", true, 131));
        addToolGroup(new DurabilityGroup("iron_tools", "Iron Tools", true, 250));
        addToolGroup(new DurabilityGroup("golden_tools", "Golden Tools", true, 32));
        addToolGroup(new DurabilityGroup("diamond_tools", "Diamond Tools", true, 1561));
        addToolGroup(new DurabilityGroup("netherite_tools", "Netherite Tools", true, 2031));

        // Armor Tiers
        addArmorGroup(new DurabilityGroup("leather_armor", "Leather Armor", true, 65));
        addArmorGroup(new DurabilityGroup("chainmail_armor", "Chainmail Armor", true, 200));
        addArmorGroup(new DurabilityGroup("iron_armor", "Iron Armor", true, 200));
        addArmorGroup(new DurabilityGroup("golden_armor", "Golden Armor", true, 90));
        addArmorGroup(new DurabilityGroup("diamond_armor", "Diamond Armor", true, 450));
        addArmorGroup(new DurabilityGroup("netherite_armor", "Netherite Armor", true, 550));

        // Miscellaneous
        addMiscGroup(new DurabilityGroup("misc", "Miscellaneous Equipment", false, 0));
    }

    private void addToolGroup(DurabilityGroup group) {
        toolGroups.put(group.getId(), group);
        groups.put(group.getId(), group);
    }

    private void addArmorGroup(DurabilityGroup group) {
        armorGroups.put(group.getId(), group);
        groups.put(group.getId(), group);
    }

    private void addMiscGroup(DurabilityGroup group) {
        miscGroups.put(group.getId(), group);
        groups.put(group.getId(), group);
    }

    private void discoverAndCategorizeItems() {
        for (Item item : BuiltInRegistries.ITEM) {
            int maxDamage = item.components().getOrDefault(DataComponents.MAX_DAMAGE, 0);
            if (maxDamage > 0) {
                String path = BuiltInRegistries.ITEM.getKey(item).getPath();
                String targetGroupId = categorizeItem(path);
                DurabilityGroup group = groups.get(targetGroupId);
                if (group != null) {
                    group.addItem(item, maxDamage);
                }
            }
        }
    }

    private String categorizeItem(String path) {
        boolean isTool = path.endsWith("_sword") || path.endsWith("_pickaxe") || path.endsWith("_axe")
                || path.endsWith("_shovel") || path.endsWith("_hoe");
        boolean isArmor = path.endsWith("_helmet") || path.endsWith("_chestplate") || path.endsWith("_leggings")
                || path.endsWith("_boots");

        if (isTool) {
            if (path.startsWith("wooden_")) return "wood_tools";
            if (path.startsWith("stone_")) return "stone_tools";
            if (path.startsWith("iron_")) return "iron_tools";
            if (path.startsWith("golden_")) return "golden_tools";
            if (path.startsWith("diamond_")) return "diamond_tools";
            if (path.startsWith("netherite_")) return "netherite_tools";
        }

        if (isArmor) {
            if (path.startsWith("leather_")) return "leather_armor";
            if (path.startsWith("chainmail_")) return "chainmail_armor";
            if (path.startsWith("iron_")) return "iron_armor";
            if (path.startsWith("golden_")) return "golden_armor";
            if (path.startsWith("diamond_")) return "diamond_armor";
            if (path.startsWith("netherite_")) return "netherite_armor";
        }

        return "misc";
    }

    public void buildConfig(ModConfigSpec.Builder builder) {
        builder.comment("Sub-category allowing custom durability configuration for items.")
                .translation("orehall.configuration.gameplay_tweaks.durability_tweaks")
                .push("durability_tweaks");

        enabledConfig = builder
                .comment("Enable the durability tweaks feature.")
                .translation("orehall.configuration.gameplay_tweaks.durability_tweaks.enabled")
                .define("enabled", true);

        // Tool tiers subcategory
        builder.comment("Durability settings for tool tiers.")
                .translation("orehall.configuration.gameplay_tweaks.durability_tweaks.tools")
                .push("tools");
        for (DurabilityGroup group : toolGroups.values()) {
            if (group.hasItems()) {
                group.buildConfig(builder, "orehall.configuration.gameplay_tweaks.durability_tweaks.tools." + group.getId());
            }
        }
        builder.pop();

        // Armor tiers subcategory
        builder.comment("Durability settings for armor tiers.")
                .translation("orehall.configuration.gameplay_tweaks.durability_tweaks.armor")
                .push("armor");
        for (DurabilityGroup group : armorGroups.values()) {
            if (group.hasItems()) {
                group.buildConfig(builder, "orehall.configuration.gameplay_tweaks.durability_tweaks.armor." + group.getId());
            }
        }
        builder.pop();

        // Miscellaneous equipment
        for (DurabilityGroup group : miscGroups.values()) {
            if (group.hasItems()) {
                group.buildConfig(builder, "orehall.configuration.gameplay_tweaks.durability_tweaks." + group.getId());
            }
        }

        builder.comment("Custom durability entries for modded or non-vanilla items.",
                        "Format: \"namespace:item_path=value\", e.g. \"minecraft:stick=50\" or \"mymod:custom_sword=1200\"")
                .translation("orehall.configuration.gameplay_tweaks.durability_tweaks.custom_overrides")
                .push("custom_overrides");
        customDurabilityConfig = builder
                .comment("List of custom item durability definitions.")
                .translation("orehall.configuration.gameplay_tweaks.durability_tweaks.custom_overrides.custom_item_durability")
                .defineListAllowEmpty("custom_item_durability", new ArrayList<>(), () -> "", obj -> obj instanceof String str && str.contains("="));
        builder.pop();

        builder.pop();
    }

    public boolean isEnabled() {
        return enabledConfig == null || enabledConfig.get();
    }

    public void apply(ModifyDefaultComponentsEvent event) {
        if (!isEnabled()) {
            return;
        }

        // Apply group and individual settings
        for (DurabilityGroup group : groups.values()) {
            group.apply(event);
        }

        // Apply custom overrides
        if (customDurabilityConfig != null) {
            for (String entry : customDurabilityConfig.get()) {
                String[] parts = entry.split("=");
                if (parts.length == 2) {
                    try {
                        ResourceLocation itemId = ResourceLocation.parse(parts[0].trim());
                        int customDurability = Integer.parseInt(parts[1].trim());

                        if (customDurability > 0) {
                            BuiltInRegistries.ITEM.getOptional(itemId).ifPresent(item -> {
                                event.modify(item, b -> b.set(DataComponents.MAX_DAMAGE, customDurability));
                                com.orehall.Orehall.LOGGER.info("Applied custom durability override for '{}': {}", itemId, customDurability);
                            });
                        }
                    } catch (Exception e) {
                        com.orehall.Orehall.LOGGER.warn("Failed to parse custom durability entry: {}", entry, e);
                    }
                }
            }
        }
    }
}
