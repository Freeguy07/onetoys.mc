package com.onetoys.module.content;

import com.onetoys.salt.SaltConfiguration;
import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Controller and configuration coordinator for Salt mechanics.
 * Manages the salt subcategory under the content module.
 */
public class SaltTweaks {
    private final ContentModule parent;
    private ModConfigSpec.BooleanValue enabledConfig;

    public SaltTweaks(ContentModule parent) {
        this.parent = parent;
    }

    public void buildConfig(ModConfigSpec.Builder builder) {
        builder.comment("Salt mineral deposits, evaporation, food seasoning, and dissolving mechanics.")
                .translation("onetoys.configuration.content.salt")
                .push("salt");

        enabledConfig = builder
                .comment("Master switch to enable or disable all features in the Salt category.")
                .translation("onetoys.configuration.content.salt.enabled")
                .define("enabled", true);

        SaltConfiguration.buildConfig(builder);

        builder.pop();
    }

    public boolean isEnabled() {
        return parent.isEnabled() && (enabledConfig == null || enabledConfig.get());
    }
}
