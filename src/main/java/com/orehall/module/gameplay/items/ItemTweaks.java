package com.orehall.module.gameplay.items;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.ModConfigSpec;

public final class ItemTweaks {
    private final RecoveryCompassTweaks recoveryCompassTweaks = new RecoveryCompassTweaks(this);
    private ModConfigSpec.BooleanValue enabledConfig;
    private ModConfigSpec.BooleanValue disableCharcoalConfig;

    public void buildConfig(ModConfigSpec.Builder builder) {
        builder.comment("Tweaks and adjustments to items and equipment.")
                .translation("orehall.configuration.gameplay_tweaks.item_tweaks")
                .push("item_tweaks");

        enabledConfig = builder
                .comment("Enable the item tweaks feature.")
                .translation("orehall.configuration.gameplay_tweaks.item_tweaks.enabled")
                .define("enabled", true);

        disableCharcoalConfig = builder
                .comment("When enabled, charcoal is disabled. Recipes producing charcoal (such as smelting logs) are removed, and charcoal cannot be burned as furnace fuel.")
                .translation("orehall.configuration.gameplay_tweaks.item_tweaks.disable_charcoal")
                .define("disable_charcoal", true);

        recoveryCompassTweaks.buildConfig(builder);

        builder.pop();
    }

    public boolean isEnabled() {
        return enabledConfig == null || enabledConfig.get();
    }

    public boolean isDisableCharcoal() {
        return isEnabled() && (disableCharcoalConfig == null || disableCharcoalConfig.get());
    }

    public RecoveryCompassTweaks getRecoveryCompassTweaks() {
        return recoveryCompassTweaks;
    }

    public void onConstruct(IEventBus modBus) {
        recoveryCompassTweaks.onConstruct(modBus);
    }

    public void onClientSetup(net.neoforged.fml.event.lifecycle.FMLClientSetupEvent event) {
        recoveryCompassTweaks.onClientSetup(event);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(com.orehall.client.CharcoalTooltipHandler::onItemTooltip);
    }
}
