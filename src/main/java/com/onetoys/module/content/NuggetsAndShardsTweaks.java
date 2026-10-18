package com.onetoys.module.content;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Controller and configuration coordinator for Nuggets and Shards.
 * Manages the nuggets_and_shards subcategory under the content module.
 */
public class NuggetsAndShardsTweaks {
    private final ContentModule parent;
    private ModConfigSpec.BooleanValue enabledConfig;

    public NuggetsAndShardsTweaks(ContentModule parent) {
        this.parent = parent;
    }

    public void buildConfig(ModConfigSpec.Builder builder) {
        builder.comment("Additions of nuggets, shards, and lumps (copper nugget, diamond/emerald shard, coal lump).")
                .translation("onetoys.configuration.content.nuggets_and_shards")
                .push("nuggets_and_shards");

        enabledConfig = builder
                .comment("Master switch to enable or disable all nuggets and shard items and their recipes.")
                .translation("onetoys.configuration.content.nuggets_and_shards.enabled")
                .define("enabled", true);

        builder.pop();
    }

    public boolean isEnabled() {
        return parent.isEnabled() && (enabledConfig == null || enabledConfig.get());
    }
}
