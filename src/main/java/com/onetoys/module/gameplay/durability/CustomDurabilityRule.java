package com.onetoys.module.gameplay.durability;

import java.util.regex.Pattern;

import com.onetoys.Onetoys;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.component.Unbreakable;
import net.neoforged.neoforge.event.ModifyDefaultComponentsEvent;

/**
 * Immutable custom durability override rule.
 * Supports exact ResourceLocation IDs, wildcard glob patterns, and item tags.
 */
public sealed interface CustomDurabilityRule permits
        CustomDurabilityRule.Exact,
        CustomDurabilityRule.Wildcard,
        CustomDurabilityRule.Tag {

    int durability();

    boolean matches(Item item, ResourceLocation id, String path);

    default void apply(ModifyDefaultComponentsEvent event, Item item, ResourceLocation id, boolean showInTooltip) {
        final int target = durability();
        if (target == -1) {
            event.modify(item, b -> b.set(DataComponents.UNBREAKABLE, new Unbreakable(showInTooltip)).remove(DataComponents.MAX_DAMAGE));
            Onetoys.LOGGER.info("Applied custom unbreakable override [{}] for '{}'", this, id);
            return;
        }
        int current = item.components().getOrDefault(DataComponents.MAX_DAMAGE, 0);
        if (current != target) {
            event.modify(item, b -> b.set(DataComponents.MAX_DAMAGE, target));
            Onetoys.LOGGER.info("Applied custom durability override [{}] for '{}': {} -> {}", this, id, current, target);
        }
    }

    default void apply(ModifyDefaultComponentsEvent event, Item item, ResourceLocation id) {
        apply(event, item, id, true);
    }

    record Exact(ResourceLocation targetId, int durability) implements CustomDurabilityRule {
        @Override
        public boolean matches(Item item, ResourceLocation id, String path) {
            return targetId.equals(id);
        }

        @Override
        public String toString() {
            return targetId.toString();
        }
    }

    record Wildcard(String pattern, Pattern regex, boolean hasColon, int durability) implements CustomDurabilityRule {
        @Override
        public boolean matches(Item item, ResourceLocation id, String path) {
            if (hasColon) {
                return regex.matcher(id.toString()).matches();
            }
            return regex.matcher(path).matches() || regex.matcher(id.toString()).matches();
        }

        @Override
        public String toString() {
            return pattern;
        }
    }

    record Tag(String rawTag, TagKey<Item> tagKey, int durability) implements CustomDurabilityRule {
        @Override
        public boolean matches(Item item, ResourceLocation id, String path) {
            try {
                return BuiltInRegistries.ITEM.wrapAsHolder(item).is(tagKey);
            } catch (Exception ignored) {
                return false;
            }
        }

        @Override
        public String toString() {
            return rawTag;
        }
    }

    static CustomDurabilityRule parse(String entry) {
        if (entry == null) {
            return null;
        }
        String[] parts = entry.split("=", 2);
        if (parts.length != 2) {
            return null;
        }

        String pattern = parts[0].trim();
        int durability;
        try {
            durability = Integer.parseInt(parts[1].trim());
        } catch (NumberFormatException e) {
            Onetoys.LOGGER.warn("Invalid durability value in custom override entry: {}", entry);
            return null;
        }
        if (durability <= 0 && durability != -1) {
            return null;
        }

        // Tag rule: e.g. #c:tools/copper=180
        if (pattern.startsWith("#")) {
            ResourceLocation tagLoc = ResourceLocation.tryParse(pattern.substring(1).trim());
            if (tagLoc != null) {
                TagKey<Item> tagKey = TagKey.create(Registries.ITEM, tagLoc);
                return new Tag(pattern, tagKey, durability);
            }
            Onetoys.LOGGER.warn("Invalid tag location in custom durability override: {}", pattern);
            return null;
        }

        // Exact ID rule: e.g. minecraft:stick=50 or caverns_and_chasms:sanguine_sword=450
        if (!pattern.contains("*") && !pattern.contains("?")) {
            ResourceLocation exactId = ResourceLocation.tryParse(pattern);
            if (exactId != null) {
                return new Exact(exactId, durability);
            }
        }

        // Wildcard glob pattern: e.g. caverns_and_chasms:sanguine_*=450 or *copper_*=180
        try {
            Pattern regex = globToRegex(pattern);
            return new Wildcard(pattern, regex, pattern.contains(":"), durability);
        } catch (Exception e) {
            Onetoys.LOGGER.warn("Failed to compile wildcard pattern in custom durability override '{}': {}", pattern, e.getMessage());
            return null;
        }
    }

    static Pattern globToRegex(String glob) {
        StringBuilder sb = new StringBuilder("^");
        for (int i = 0; i < glob.length(); i++) {
            char c = glob.charAt(i);
            if (c == '*') {
                sb.append(".*");
            } else if (c == '?') {
                sb.append(".");
            } else if ("\\.[]{}()+^$|".indexOf(c) >= 0) {
                sb.append('\\').append(c);
            } else {
                sb.append(c);
            }
        }
        sb.append("$");
        return Pattern.compile(sb.toString(), Pattern.CASE_INSENSITIVE);
    }
}
