package com.orehall.module.gameplay.mobs;

import net.minecraft.world.entity.animal.MushroomCow;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

public class MooshroomTweaks {
    private ModConfigSpec.BooleanValue enabledConfig;
    private ModConfigSpec.BooleanValue milkingCooldownEnabledConfig;
    private ModConfigSpec.IntValue milkingCooldownTicksConfig;
    private ModConfigSpec.BooleanValue stewMilkingCooldownEnabledConfig;
    private ModConfigSpec.IntValue stewMilkingCooldownTicksConfig;
    private ModConfigSpec.BooleanValue feedingResetsCooldownConfig;

    public void buildConfig(ModConfigSpec.Builder builder) {
        builder.comment("Tweaks and adjustments to mooshrooms.")
                .translation("orehall.configuration.gameplay_tweaks.mob_tweaks.mooshroom")
                .push("mooshroom");

        enabledConfig = builder
                .comment("Enable mooshroom tweaks.")
                .translation("orehall.configuration.gameplay_tweaks.mob_tweaks.mooshroom.enabled")
                .define("enabled", true);

        milkingCooldownEnabledConfig = builder
                .comment("When enabled, milking mooshrooms with a bucket triggers a cooldown before they can be milked again.")
                .translation("orehall.configuration.gameplay_tweaks.mob_tweaks.mooshroom.milking_cooldown_enabled")
                .define("milking_cooldown_enabled", true);

        milkingCooldownTicksConfig = builder
                .comment("Milking cooldown duration in ticks (20 ticks = 1 second, 1200 ticks = 1 minute). Set to 0 to disable cooldown.")
                .translation("orehall.configuration.gameplay_tweaks.mob_tweaks.mooshroom.milking_cooldown_ticks")
                .defineInRange("milking_cooldown_ticks", 1200, 0, 72000);

        stewMilkingCooldownEnabledConfig = builder
                .comment("When enabled, harvesting stew from mooshrooms with a bowl triggers a cooldown before they can be milked for stew again.")
                .translation("orehall.configuration.gameplay_tweaks.mob_tweaks.mooshroom.stew_milking_cooldown_enabled")
                .define("stew_milking_cooldown_enabled", true);

        stewMilkingCooldownTicksConfig = builder
                .comment("Stew milking cooldown duration in ticks (20 ticks = 1 second, 1200 ticks = 1 minute). Set to 0 to disable cooldown.")
                .translation("orehall.configuration.gameplay_tweaks.mob_tweaks.mooshroom.stew_milking_cooldown_ticks")
                .defineInRange("stew_milking_cooldown_ticks", 1200, 0, 72000);

        feedingResetsCooldownConfig = builder
                .comment("When enabled, feeding mooshrooms wheat clears their milking and stew cooldowns.")
                .translation("orehall.configuration.gameplay_tweaks.mob_tweaks.mooshroom.feeding_resets_cooldown")
                .define("feeding_resets_cooldown", true);

        builder.pop();
    }

    public boolean isEnabled() {
        return enabledConfig == null || enabledConfig.get();
    }

    public boolean isMilkingCooldownEnabled() {
        return isEnabled() && (milkingCooldownEnabledConfig == null || milkingCooldownEnabledConfig.get());
    }

    public boolean isStewMilkingCooldownEnabled() {
        return isEnabled() && (stewMilkingCooldownEnabledConfig == null || stewMilkingCooldownEnabledConfig.get());
    }

    public boolean isFeedingResetEnabled() {
        return (isMilkingCooldownEnabled() || isStewMilkingCooldownEnabled()) && (feedingResetsCooldownConfig == null || feedingResetsCooldownConfig.get());
    }

    public int getMilkingCooldownTicks() {
        return milkingCooldownTicksConfig != null ? milkingCooldownTicksConfig.get() : 1200;
    }

    public int getStewMilkingCooldownTicks() {
        return stewMilkingCooldownTicksConfig != null ? stewMilkingCooldownTicksConfig.get() : 1200;
    }

    public void onConstruct(IEventBus modBus) {
        // Reserved for future mooshroom-specific event bus listeners
    }

    public void handleMilking(PlayerInteractEvent.EntityInteract event, MushroomCow mooshroom) {
        MobMilkingHelper.handle(event, mooshroom, isMilkingCooldownEnabled(), getMilkingCooldownTicks());
    }

    public void handleStewMilking(PlayerInteractEvent.EntityInteract event, MushroomCow mooshroom) {
        MobMilkingHelper.handleStew(event, mooshroom, isStewMilkingCooldownEnabled(), getStewMilkingCooldownTicks());
    }

    public void handleFeeding(PlayerInteractEvent.EntityInteract event, MushroomCow mooshroom) {
        MobMilkingHelper.handleFeeding(event, mooshroom, isFeedingResetEnabled());
    }
}
