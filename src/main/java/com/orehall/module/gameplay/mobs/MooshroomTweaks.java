package com.orehall.module.gameplay.mobs;

import net.minecraft.world.entity.animal.MushroomCow;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

public class MooshroomTweaks {
    private ModConfigSpec.BooleanValue enabledConfig;
    private ModConfigSpec.BooleanValue milkingCooldownEnabledConfig;
    private ModConfigSpec.IntValue milkingCooldownTicksConfig;

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

        builder.pop();
    }

    public boolean isEnabled() {
        return enabledConfig == null || enabledConfig.get();
    }

    public boolean isMilkingCooldownEnabled() {
        return isEnabled() && (milkingCooldownEnabledConfig == null || milkingCooldownEnabledConfig.get());
    }

    public int getMilkingCooldownTicks() {
        return milkingCooldownTicksConfig != null ? milkingCooldownTicksConfig.get() : 1200;
    }

    public void onConstruct(IEventBus modBus) {
        // Reserved for future mooshroom-specific event bus listeners
    }

    public void handleMilking(PlayerInteractEvent.EntityInteract event, MushroomCow mooshroom) {
        MobMilkingHelper.handle(event, mooshroom, isMilkingCooldownEnabled(), getMilkingCooldownTicks());
    }
}
