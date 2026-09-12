package com.orehall.module.gameplay.items;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.ModConfigSpec;

public final class ItemTweaks {
    private final RecoveryCompassTweaks recoveryCompassTweaks = new RecoveryCompassTweaks(this);
    private ModConfigSpec.BooleanValue enabledConfig;

    public void buildConfig(ModConfigSpec.Builder builder) {
        builder.comment("Tweaks and adjustments to items and equipment.")
                .translation("orehall.configuration.gameplay_tweaks.item_tweaks")
                .push("item_tweaks");

        enabledConfig = builder
                .comment("Enable the item tweaks feature.")
                .translation("orehall.configuration.gameplay_tweaks.item_tweaks.enabled")
                .define("enabled", true);

        recoveryCompassTweaks.buildConfig(builder);

        builder.pop();
    }

    public boolean isEnabled() {
        return enabledConfig == null || enabledConfig.get();
    }

    public RecoveryCompassTweaks getRecoveryCompassTweaks() {
        return recoveryCompassTweaks;
    }

    public void onConstruct(IEventBus modBus) {
        recoveryCompassTweaks.onConstruct(modBus);
    }

    public void onClientSetup(net.neoforged.fml.event.lifecycle.FMLClientSetupEvent event) {
        recoveryCompassTweaks.onClientSetup(event);
    }
}
