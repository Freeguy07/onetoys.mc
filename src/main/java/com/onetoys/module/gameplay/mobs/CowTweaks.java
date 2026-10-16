package com.onetoys.module.gameplay.mobs;

import net.minecraft.world.entity.animal.Cow;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

public class CowTweaks {
    private ModConfigSpec.BooleanValue enabledConfig;
    private ModConfigSpec.BooleanValue milkingCooldownEnabledConfig;
    private ModConfigSpec.IntValue milkingCooldownTicksConfig;
    private ModConfigSpec.BooleanValue feedingResetsCooldownConfig;

    public void buildConfig(ModConfigSpec.Builder builder) {
        builder.comment("Tweaks and adjustments to cows.")
                .translation("onetoys.configuration.gameplay_tweaks.mob_tweaks.cow")
                .push("cow");

        enabledConfig = builder
                .comment("Enable cow tweaks.")
                .translation("onetoys.configuration.gameplay_tweaks.mob_tweaks.cow.enabled")
                .define("enabled", true);

        milkingCooldownEnabledConfig = builder
                .comment("When enabled, milking cows triggers a cooldown before they can be milked again.")
                .translation("onetoys.configuration.gameplay_tweaks.mob_tweaks.cow.milking_cooldown_enabled")
                .define("milking_cooldown_enabled", true);

        milkingCooldownTicksConfig = builder
                .comment("Milking cooldown duration in ticks (20 ticks = 1 second, 1200 ticks = 1 minute). Set to 0 to disable cooldown.")
                .translation("onetoys.configuration.gameplay_tweaks.mob_tweaks.cow.milking_cooldown_ticks")
                .defineInRange("milking_cooldown_ticks", 1200, 0, 72000);

        feedingResetsCooldownConfig = builder
                .comment("When enabled, feeding cows wheat clears their milking cooldown.")
                .translation("onetoys.configuration.gameplay_tweaks.mob_tweaks.cow.feeding_resets_cooldown")
                .define("feeding_resets_cooldown", true);

        builder.pop();
    }

    public boolean isEnabled() {
        return enabledConfig == null || enabledConfig.get();
    }

    public boolean isMilkingCooldownEnabled() {
        return isEnabled() && (milkingCooldownEnabledConfig == null || milkingCooldownEnabledConfig.get());
    }

    public boolean isFeedingResetEnabled() {
        return isMilkingCooldownEnabled() && (feedingResetsCooldownConfig == null || feedingResetsCooldownConfig.get());
    }

    public int getMilkingCooldownTicks() {
        return milkingCooldownTicksConfig != null ? milkingCooldownTicksConfig.get() : 1200;
    }

    public void onConstruct(IEventBus modBus) {
        // Reserved for future cow-specific event bus listeners
    }

    public void handleMilking(PlayerInteractEvent.EntityInteract event, Cow cow) {
        MobMilkingHelper.handle(event, cow, isMilkingCooldownEnabled(), getMilkingCooldownTicks());
    }

    public void handleFeeding(PlayerInteractEvent.EntityInteract event, Cow cow) {
        MobMilkingHelper.handleFeeding(event, cow, isFeedingResetEnabled());
    }
}
