package com.onetoys.module.gameplay.mobs;

import net.minecraft.world.entity.animal.goat.Goat;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

public class GoatTweaks {
    private ModConfigSpec.BooleanValue enabledConfig;
    private ModConfigSpec.BooleanValue dropMuttonConfig;
    private ModConfigSpec.BooleanValue milkingCooldownEnabledConfig;
    private ModConfigSpec.IntValue milkingCooldownTicksConfig;
    private ModConfigSpec.BooleanValue feedingResetsCooldownConfig;

    public void buildConfig(ModConfigSpec.Builder builder) {
        builder.comment("Tweaks and adjustments to goats.")
                .translation("onetoys.configuration.gameplay_tweaks.mob_tweaks.goat")
                .push("goat");

        enabledConfig = builder
                .comment("Enable goat tweaks.")
                .translation("onetoys.configuration.gameplay_tweaks.mob_tweaks.goat.enabled")
                .define("enabled", true);

        dropMuttonConfig = builder
                .comment("When enabled, goats drop mutton when killed, just like sheep.")
                .translation("onetoys.configuration.gameplay_tweaks.mob_tweaks.goat.drop_mutton")
                .define("drop_mutton", true);

        milkingCooldownEnabledConfig = builder
                .comment("When enabled, milking goats triggers a cooldown before they can be milked again.")
                .translation("onetoys.configuration.gameplay_tweaks.mob_tweaks.goat.milking_cooldown_enabled")
                .define("milking_cooldown_enabled", true);

        milkingCooldownTicksConfig = builder
                .comment("Milking cooldown duration in ticks (20 ticks = 1 second, 6000 ticks = 5 minutes). Set to 0 to disable cooldown.")
                .translation("onetoys.configuration.gameplay_tweaks.mob_tweaks.goat.milking_cooldown_ticks")
                .defineInRange("milking_cooldown_ticks", 6000, 0, 72000);

        feedingResetsCooldownConfig = builder
                .comment("When enabled, feeding goats wheat clears their milking cooldown.")
                .translation("onetoys.configuration.gameplay_tweaks.mob_tweaks.goat.feeding_resets_cooldown")
                .define("feeding_resets_cooldown", true);

        builder.pop();
    }

    public boolean isEnabled() {
        return enabledConfig == null || enabledConfig.get();
    }

    public boolean isMuttonDropEnabled() {
        return isEnabled() && (dropMuttonConfig == null || dropMuttonConfig.get());
    }

    public boolean isMilkingCooldownEnabled() {
        return isEnabled() && (milkingCooldownEnabledConfig == null || milkingCooldownEnabledConfig.get());
    }

    public boolean isFeedingResetEnabled() {
        return isMilkingCooldownEnabled() && (feedingResetsCooldownConfig == null || feedingResetsCooldownConfig.get());
    }

    public int getMilkingCooldownTicks() {
        return milkingCooldownTicksConfig != null ? milkingCooldownTicksConfig.get() : 6000;
    }

    public void onConstruct(IEventBus modBus) {
        // Reserved for future goat-specific event bus listeners
    }

    public void handleMilking(PlayerInteractEvent.EntityInteract event, Goat goat) {
        MobMilkingHelper.handle(event, goat, isMilkingCooldownEnabled(), getMilkingCooldownTicks());
    }

    public void handleFeeding(PlayerInteractEvent.EntityInteract event, Goat goat) {
        MobMilkingHelper.handleFeeding(event, goat, isFeedingResetEnabled());
    }
}
