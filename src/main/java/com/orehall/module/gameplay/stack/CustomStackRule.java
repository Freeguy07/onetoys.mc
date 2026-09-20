package com.orehall.module.gameplay.stack;

import java.util.regex.Pattern;

import com.orehall.Orehall;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.event.ModifyDefaultComponentsEvent;

/**
 * Immutable custom stack size override rule.
 * Supports exact ResourceLocation IDs, wildcard glob patterns, and item tags.
 */
public sealed interface CustomStackRule permits
        CustomStackRule.Exact,
        CustomStackRule.Wildcard,
        CustomStackRule.Tag {

    int maxStackSize();

    boolean matches(Item item, ResourceLocation id, String path);

    default void apply(ModifyDefaultComponentsEvent event, Item item, ResourceLocation id) {
        final int target = maxStackSize();
        int current = item.components().getOrDefault(DataComponents.MAX_STACK_SIZE, 64);
        if (current != target) {
            event.modify(item, b -> b.set(DataComponents.MAX_STACK_SIZE, target));
            Orehall.LOGGER.info("Applied custom stack size [{}] for '{}': {} -> {}", this, id, current, target);
        }
    }

    record Exact(ResourceLocation targetId, int maxStackSize) implements CustomStackRule {
        @Override
        public boolean matches(Item item, ResourceLocation id, String path) {
            return targetId.equals(id);
        }

        @Override
        public String toString() {
            return targetId + "=" + maxStackSize;
        }
    }

    record Wildcard(String pattern, Pattern regex, boolean hasColon, int maxStackSize) implements CustomStackRule {
        @Override
        public boolean matches(Item item, ResourceLocation id, String path) {
            if (hasColon) {
                return regex.matcher(id.toString()).matches();
            }
            return regex.matcher(path).matches() || regex.matcher(id.toString()).matches();
        }

        @Override
        public String toString() {
            return pattern + "=" + maxStackSize;
        }
    }

    record Tag(String rawTag, TagKey<Item> tagKey, int maxStackSize) implements CustomStackRule {
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
            return rawTag + "=" + maxStackSize;
        }
    }

    static CustomStackRule parse(String entry) {
        if (entry == null) {
            return null;
        }
        String[] parts = entry.split("=", 2);
        if (parts.length != 2) {
            return null;
        }

        String pattern = parts[0].trim();
        int size;
        try {
            size = Integer.parseInt(parts[1].trim());
            if (size < 1 || size > 99) {
                Orehall.LOGGER.warn("Invalid stack size '{}' in rule '{}' (must be between 1 and 99). Skipping.", parts[1].trim(), entry);
                return null;
            }
        } catch (NumberFormatException e) {
            Orehall.LOGGER.warn("Failed to parse stack size integer in rule '{}'. Skipping.", entry);
            return null;
        }

        if (pattern.startsWith("#")) {
            String tagString = pattern.substring(1);
            ResourceLocation tagLoc = ResourceLocation.tryParse(tagString);
            if (tagLoc != null) {
                TagKey<Item> tagKey = TagKey.create(Registries.ITEM, tagLoc);
                return new Tag(pattern, tagKey, size);
            }
            Orehall.LOGGER.warn("Invalid tag syntax in rule: '{}'", entry);
            return null;
        }

        if (pattern.contains("*") || pattern.contains("?")) {
            boolean hasColon = pattern.contains(":");
            String regexStr = "^" + Pattern.quote(pattern)
                    .replace("*", "\\E.*\\Q")
                    .replace("?", "\\E.\\Q") + "$";
            regexStr = regexStr.replace("\\Q\\E", "");
            Pattern compiled = Pattern.compile(regexStr);
            return new Wildcard(pattern, compiled, hasColon, size);
        }

        ResourceLocation loc = ResourceLocation.tryParse(pattern);
        if (loc != null) {
            return new Exact(loc, size);
        }

        Orehall.LOGGER.warn("Invalid item ID or pattern in rule: '{}'", entry);
        return null;
    }
}
