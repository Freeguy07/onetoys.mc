package com.orehall.module.gameplay.mobs;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.animal.MushroomCow;
import net.minecraft.world.entity.animal.goat.Goat;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

public class MobTweaks {
    private final GoatTweaks goatTweaks = new GoatTweaks();
    private final CowTweaks cowTweaks = new CowTweaks();
    private final MooshroomTweaks mooshroomTweaks = new MooshroomTweaks();
    private ModConfigSpec.BooleanValue enabledConfig;

    public void buildConfig(ModConfigSpec.Builder builder) {
        builder.comment("Tweaks and adjustments to vanilla mobs and creatures.")
                .translation("orehall.configuration.gameplay_tweaks.mob_tweaks")
                .push("mob_tweaks");

        enabledConfig = builder
                .comment("Enable the mob tweaks feature.")
                .translation("orehall.configuration.gameplay_tweaks.mob_tweaks.enabled")
                .define("enabled", true);

        goatTweaks.buildConfig(builder);
        cowTweaks.buildConfig(builder);
        mooshroomTweaks.buildConfig(builder);

        builder.pop();
    }

    public boolean isEnabled() {
        return enabledConfig == null || enabledConfig.get();
    }

    public GoatTweaks getGoatTweaks() {
        return goatTweaks;
    }

    public CowTweaks getCowTweaks() {
        return cowTweaks;
    }

    public MooshroomTweaks getMooshroomTweaks() {
        return mooshroomTweaks;
    }

    public void onConstruct(IEventBus modBus) {
        goatTweaks.onConstruct(modBus);
        cowTweaks.onConstruct(modBus);
        mooshroomTweaks.onConstruct(modBus);

        // Single consolidated interaction listener for all mob milking mechanics
        NeoForge.EVENT_BUS.addListener(this::onEntityInteract);
    }

    private void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (!isEnabled()) {
            return;
        }

        // Fast path: ignore interactions with any item other than a bucket immediately
        if (!event.getItemStack().is(Items.BUCKET)) {
            return;
        }

        Entity target = event.getTarget();
        if (target instanceof MushroomCow mooshroom) {
            mooshroomTweaks.handleMilking(event, mooshroom);
        } else if (target instanceof Cow cow) {
            cowTweaks.handleMilking(event, cow);
        } else if (target instanceof Goat goat) {
            goatTweaks.handleMilking(event, goat);
        }
    }
}
