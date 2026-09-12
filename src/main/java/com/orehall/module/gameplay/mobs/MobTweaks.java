package com.orehall.module.gameplay.mobs;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.animal.MushroomCow;
import net.minecraft.world.entity.animal.goat.Goat;
import net.minecraft.world.item.ItemStack;
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

        // Single consolidated interaction listener for all mob milking and feeding mechanics
        NeoForge.EVENT_BUS.addListener(this::onEntityInteract);
    }

    private void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (!isEnabled()) {
            return;
        }

        ItemStack stack = event.getItemStack();
        if (stack.isEmpty()) {
            return;
        }

        Entity target = event.getTarget();
        boolean isBucket = stack.is(Items.BUCKET);

        if (target instanceof MushroomCow mooshroom) {
            if (isBucket) {
                mooshroomTweaks.handleMilking(event, mooshroom);
            } else if (stack.is(Items.BOWL)) {
                mooshroomTweaks.handleStewMilking(event, mooshroom);
            } else if (mooshroom.isFood(stack)) {
                mooshroomTweaks.handleFeeding(event, mooshroom);
            }
        } else if (target instanceof Cow cow) {
            if (isBucket) {
                cowTweaks.handleMilking(event, cow);
            } else if (cow.isFood(stack)) {
                cowTweaks.handleFeeding(event, cow);
            }
        } else if (target instanceof Goat goat) {
            if (isBucket) {
                goatTweaks.handleMilking(event, goat);
            } else if (goat.isFood(stack)) {
                goatTweaks.handleFeeding(event, goat);
            }
        }
    }
}
