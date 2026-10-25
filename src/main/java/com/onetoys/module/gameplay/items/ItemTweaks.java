package com.onetoys.module.gameplay.items;

import com.onetoys.module.gameplay.GameplayTweaksModule;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.ModConfigSpec;

public final class ItemTweaks {
    private final GameplayTweaksModule parent;
    private final RecoveryCompassTweaks recoveryCompassTweaks = new RecoveryCompassTweaks(this);
    private final StopwatchTweaks stopwatchTweaks = new StopwatchTweaks(this);
    private final SwordBlockingTweaks swordBlockingTweaks = new SwordBlockingTweaks(this);
    private final SpearTweaks spearTweaks = new SpearTweaks(this);
    private final MacheteTweaks macheteTweaks = new MacheteTweaks(this);
    private final ClubTweaks clubTweaks = new ClubTweaks(this);
    private ModConfigSpec.BooleanValue enabledConfig;
    private ModConfigSpec.BooleanValue disableCharcoalConfig;

    public ItemTweaks(GameplayTweaksModule parent) {
        this.parent = parent;
    }

    public ItemTweaks() {
        this(null);
    }

    public void buildConfig(ModConfigSpec.Builder builder) {
        builder.comment("Tweaks and adjustments to items and equipment.")
                .translation("onetoys.configuration.gameplay_tweaks.item_tweaks")
                .push("item_tweaks");

        enabledConfig = builder
                .comment("Enable the item tweaks feature.")
                .translation("onetoys.configuration.gameplay_tweaks.item_tweaks.enabled")
                .define("enabled", true);

        disableCharcoalConfig = builder
                .comment("When enabled, charcoal is disabled. Recipes producing charcoal (such as smelting logs) are removed, and charcoal cannot be burned as furnace fuel.")
                .translation("onetoys.configuration.gameplay_tweaks.item_tweaks.disable_charcoal")
                .define("disable_charcoal", true);

        recoveryCompassTweaks.buildConfig(builder);
        stopwatchTweaks.buildConfig(builder);
        swordBlockingTweaks.buildConfig(builder);
        spearTweaks.buildConfig(builder);
        macheteTweaks.buildConfig(builder);
        clubTweaks.buildConfig(builder);

        builder.pop();
    }

    public boolean isEnabled() {
        return (parent == null || parent.isEnabled()) && (enabledConfig == null || enabledConfig.get());
    }

    public boolean isDisableCharcoal() {
        return isEnabled() && (disableCharcoalConfig == null || disableCharcoalConfig.get());
    }

    public RecoveryCompassTweaks getRecoveryCompassTweaks() {
        return recoveryCompassTweaks;
    }

    public StopwatchTweaks getStopwatchTweaks() {
        return stopwatchTweaks;
    }

    public SwordBlockingTweaks getSwordBlockingTweaks() {
        return swordBlockingTweaks;
    }

    public SpearTweaks getSpearTweaks() {
        return spearTweaks;
    }

    public MacheteTweaks getMacheteTweaks() {
        return macheteTweaks;
    }

    public ClubTweaks getClubTweaks() {
        return clubTweaks;
    }

    public boolean isStopwatchEnabled() {
        return isEnabled() && stopwatchTweaks.isEnabled();
    }

    public boolean isStopwatchHotbarDisplayEnabled() {
        return isStopwatchEnabled() && stopwatchTweaks.isHotbarDisplayEnabled();
    }

    public boolean isSwordBlockingEnabled() {
        return isEnabled() && swordBlockingTweaks.isEnabled();
    }

    public boolean isSpearEnabled() {
        return isEnabled() && spearTweaks.isEnabled();
    }

    public boolean isMacheteEnabled() {
        return isEnabled() && macheteTweaks.isEnabled();
    }

    public boolean isClubEnabled() {
        return isEnabled() && clubTweaks.isEnabled();
    }

    public void onConstruct(IEventBus modBus) {
        recoveryCompassTweaks.onConstruct(modBus);
        swordBlockingTweaks.onConstruct(modBus);
        spearTweaks.onConstruct(modBus);
        macheteTweaks.onConstruct(modBus);
        clubTweaks.onConstruct(modBus);
    }
}
