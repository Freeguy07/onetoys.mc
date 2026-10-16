package com.onetoys.salt;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.level.block.SoundType;
import net.neoforged.neoforge.common.util.DeferredSoundType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class SaltSounds {
    public static final DeferredRegister<SoundEvent> SOUNDS =
            DeferredRegister.create(BuiltInRegistries.SOUND_EVENT, Salt.ID);

    public static final DeferredHolder<SoundEvent, SoundEvent> SALT_DISSOLVE = registerBlockSound("salt.dissolve");
    public static final DeferredHolder<SoundEvent, SoundEvent> MELT = registerBlockSound("melt");
    public static final DeferredHolder<SoundEvent, SoundEvent> CAULDRON_EVAPORATE = registerBlockSound("cauldron.evaporate");
    public static final DeferredHolder<SoundEvent, SoundEvent> BUBBLE_POP = registerBlockSound("bubble_pop");
    public static final DeferredHolder<SoundEvent, SoundEvent> SALT_CAULDRON_REMOVE_SALT = registerBlockSound("salt_cauldron.remove_salt");

    public static final DeferredHolder<SoundEvent, SoundEvent> SALT_BREAK = registerBlockSound("salt.break");
    public static final DeferredHolder<SoundEvent, SoundEvent> SALT_STEP = registerBlockSound("salt.step");
    public static final DeferredHolder<SoundEvent, SoundEvent> SALT_PLACE = registerBlockSound("salt.place");
    public static final DeferredHolder<SoundEvent, SoundEvent> SALT_HIT = registerBlockSound("salt.hit");
    public static final DeferredHolder<SoundEvent, SoundEvent> SALT_FALL = registerBlockSound("salt.fall");

    public static final DeferredHolder<SoundEvent, SoundEvent> SALT_CLUSTER_BREAK = registerBlockSound("salt_cluster.break");
    public static final DeferredHolder<SoundEvent, SoundEvent> SALT_CLUSTER_STEP = registerBlockSound("salt_cluster.step");
    public static final DeferredHolder<SoundEvent, SoundEvent> SALT_CLUSTER_PLACE = registerBlockSound("salt_cluster.place");
    public static final DeferredHolder<SoundEvent, SoundEvent> SALT_CLUSTER_HIT = registerBlockSound("salt_cluster.hit");
    public static final DeferredHolder<SoundEvent, SoundEvent> SALT_CLUSTER_FALL = registerBlockSound("salt_cluster.fall");

    public static final DeferredHolder<SoundEvent, SoundEvent> LARGE_SALT_BUD_BREAK = registerBlockSound("large_salt_bud.break");
    public static final DeferredHolder<SoundEvent, SoundEvent> LARGE_SALT_BUD_STEP = registerBlockSound("large_salt_bud.step");
    public static final DeferredHolder<SoundEvent, SoundEvent> LARGE_SALT_BUD_PLACE = registerBlockSound("large_salt_bud.place");
    public static final DeferredHolder<SoundEvent, SoundEvent> LARGE_SALT_BUD_HIT = registerBlockSound("large_salt_bud.hit");
    public static final DeferredHolder<SoundEvent, SoundEvent> LARGE_SALT_BUD_FALL = registerBlockSound("large_salt_bud.fall");

    public static final DeferredHolder<SoundEvent, SoundEvent> MEDIUM_SALT_BUD_BREAK = registerBlockSound("medium_salt_bud.break");
    public static final DeferredHolder<SoundEvent, SoundEvent> MEDIUM_SALT_BUD_STEP = registerBlockSound("medium_salt_bud.step");
    public static final DeferredHolder<SoundEvent, SoundEvent> MEDIUM_SALT_BUD_PLACE = registerBlockSound("medium_salt_bud.place");
    public static final DeferredHolder<SoundEvent, SoundEvent> MEDIUM_SALT_BUD_HIT = registerBlockSound("medium_salt_bud.hit");
    public static final DeferredHolder<SoundEvent, SoundEvent> MEDIUM_SALT_BUD_FALL = registerBlockSound("medium_salt_bud.fall");

    public static final DeferredHolder<SoundEvent, SoundEvent> SMALL_SALT_BUD_BREAK = registerBlockSound("small_salt_bud.break");
    public static final DeferredHolder<SoundEvent, SoundEvent> SMALL_SALT_BUD_STEP = registerBlockSound("small_salt_bud.step");
    public static final DeferredHolder<SoundEvent, SoundEvent> SMALL_SALT_BUD_PLACE = registerBlockSound("small_salt_bud.place");
    public static final DeferredHolder<SoundEvent, SoundEvent> SMALL_SALT_BUD_HIT = registerBlockSound("small_salt_bud.hit");
    public static final DeferredHolder<SoundEvent, SoundEvent> SMALL_SALT_BUD_FALL = registerBlockSound("small_salt_bud.fall");

    private static DeferredHolder<SoundEvent, SoundEvent> registerBlockSound(String name) {
        return SOUNDS.register("block.salt." + name, () -> SoundEvent.createVariableRangeEvent(
                ResourceLocation.fromNamespaceAndPath(Salt.ID, "block.salt." + name)));
    }

    public static class Types {
        public static final SoundType SALT = new DeferredSoundType(1.0f, 1.0f,
                SALT_BREAK,
                SALT_STEP,
                SALT_PLACE,
                SALT_HIT,
                SALT_FALL);

        public static final SoundType SALT_CLUSTER = new DeferredSoundType(1.0f, 1.0f,
                SALT_CLUSTER_BREAK,
                SALT_CLUSTER_STEP,
                SALT_CLUSTER_PLACE,
                SALT_CLUSTER_HIT,
                SALT_CLUSTER_FALL);

        public static final SoundType LARGE_SALT_BUD = new DeferredSoundType(1.0f, 1.0f,
                LARGE_SALT_BUD_BREAK,
                LARGE_SALT_BUD_STEP,
                LARGE_SALT_BUD_PLACE,
                LARGE_SALT_BUD_HIT,
                LARGE_SALT_BUD_FALL);

        public static final SoundType MEDIUM_SALT_BUD = new DeferredSoundType(1.0f, 1.0f,
                MEDIUM_SALT_BUD_BREAK,
                MEDIUM_SALT_BUD_STEP,
                MEDIUM_SALT_BUD_PLACE,
                MEDIUM_SALT_BUD_HIT,
                MEDIUM_SALT_BUD_FALL);

        public static final SoundType SMALL_SALT_BUD = new DeferredSoundType(1.0f, 1.0f,
                SMALL_SALT_BUD_BREAK,
                SMALL_SALT_BUD_STEP,
                SMALL_SALT_BUD_PLACE,
                SMALL_SALT_BUD_HIT,
                SMALL_SALT_BUD_FALL);
    }
}

