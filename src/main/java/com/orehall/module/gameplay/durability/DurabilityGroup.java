package com.orehall.module.gameplay.durability;

import java.util.LinkedHashMap;
import java.util.Map;

import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.event.ModifyDefaultComponentsEvent;

public class DurabilityGroup {
    private final String id;
    private final String displayName;
    private final boolean isTier;
    private final int defaultGlobalDurability;
    private final Map<Item, Integer> items = new LinkedHashMap<>();

    private ModConfigSpec.BooleanValue enableGlobal;
    private ModConfigSpec.IntValue globalDurability;
    private final Map<Item, ModConfigSpec.IntValue> itemConfigs = new LinkedHashMap<>();

    public DurabilityGroup(String id, String displayName, boolean isTier, int defaultGlobalDurability) {
        this.id = id;
        this.displayName = displayName;
        this.isTier = isTier;
        this.defaultGlobalDurability = defaultGlobalDurability;
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void addItem(Item item, int defaultDurability) {
        items.put(item, defaultDurability);
    }

    public boolean hasItems() {
        return !items.isEmpty();
    }

    public void buildConfig(ModConfigSpec.Builder builder, String translationKey) {
        builder.comment(displayName)
                .translation(translationKey)
                .push(id);

        if (isTier) {
            enableGlobal = builder
                    .comment("If true, all items in this group will use the 'global_durability' value below instead of their individual values.")
                    .translation("orehall.configuration.durability_tweaks.enable_global")
                    .define("enable_global", false);

            globalDurability = builder
                    .comment("Global durability applied to all items in this group when 'enable_global' is true.")
                    .translation("orehall.configuration.durability_tweaks.global_durability")
                    .defineInRange("global_durability", defaultGlobalDurability, 1, 1_000_000);
        }

        for (Map.Entry<Item, Integer> entry : items.entrySet()) {
            Item item = entry.getKey();
            int defaultVal = entry.getValue();
            String itemPath = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(item).getPath();

            ModConfigSpec.IntValue config = builder
                    .comment("Durability for " + itemPath + " (Default: " + defaultVal + ")")
                    .translation(item.getDescriptionId())
                    .defineInRange(itemPath, defaultVal, 1, 1_000_000);
            itemConfigs.put(item, config);
        }

        builder.pop();
    }

    public void buildConfig(ModConfigSpec.Builder builder) {
        buildConfig(builder, "orehall.configuration.gameplay_tweaks.durability_tweaks." + id);
    }

    public void apply(ModifyDefaultComponentsEvent event) {
        boolean useGlobal = isTier && enableGlobal != null && enableGlobal.get();
        int globalVal = (useGlobal && globalDurability != null) ? globalDurability.get() : -1;

        for (Map.Entry<Item, ModConfigSpec.IntValue> entry : itemConfigs.entrySet()) {
            Item item = entry.getKey();
            int defaultVal = items.getOrDefault(item, 0);
            int targetVal = useGlobal ? globalVal : entry.getValue().get();

            if (targetVal > 0 && targetVal != defaultVal) {
                ResourceLocation key = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(item);
                com.orehall.Orehall.LOGGER.info("Adjusting durability for '{}': {} -> {}", key, defaultVal, targetVal);
                event.modify(item, patchBuilder -> patchBuilder.set(DataComponents.MAX_DAMAGE, targetVal));
            }
        }
    }
}
