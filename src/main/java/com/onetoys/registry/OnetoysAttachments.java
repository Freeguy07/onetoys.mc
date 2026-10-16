package com.onetoys.registry;

import java.util.ArrayList;
import java.util.List;

import com.mojang.serialization.Codec;
import com.onetoys.Onetoys;
import com.onetoys.module.gameplay.items.SavedRecoveryCompass;

import net.minecraft.network.codec.ByteBufCodecs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public class OnetoysAttachments {
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, Onetoys.MODID);

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<Long>> MILK_COOLDOWN =
            ATTACHMENT_TYPES.register("milk_cooldown", () ->
                    AttachmentType.builder(() -> 0L)
                            .serialize(Codec.LONG)
                            .sync(ByteBufCodecs.VAR_LONG)
                            .build()
            );

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<Long>> STEW_COOLDOWN =
            ATTACHMENT_TYPES.register("stew_cooldown", () ->
                    AttachmentType.builder(() -> 0L)
                            .serialize(Codec.LONG)
                            .sync(ByteBufCodecs.VAR_LONG)
                            .build()
            );

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<List<SavedRecoveryCompass>>> SAVED_RECOVERY_COMPASSES =
            ATTACHMENT_TYPES.register("saved_recovery_compasses", () ->
                    AttachmentType.<List<SavedRecoveryCompass>>builder(() -> new ArrayList<>())
                            .serialize(SavedRecoveryCompass.CODEC.listOf().xmap(ArrayList::new, ArrayList::new))
                            .copyOnDeath()
                            .build()
            );

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<Long>> WET_UNTIL =
            ATTACHMENT_TYPES.register("wet_until", () ->
                    AttachmentType.builder(() -> 0L)
                            .serialize(Codec.LONG)
                            .build()
            );

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<Boolean>> IS_WET =
            ATTACHMENT_TYPES.register("is_wet", () ->
                    AttachmentType.builder(() -> false)
                            .serialize(Codec.BOOL)
                            .sync(ByteBufCodecs.BOOL)
                            .build()
            );

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<Boolean>> RECOVERY_COMPASS_OVERLAY =
            ATTACHMENT_TYPES.register("recovery_compass_overlay", () ->
                    AttachmentType.builder(() -> false)
                            .serialize(Codec.BOOL)
                            .sync(ByteBufCodecs.BOOL)
                            .build()
            );

    public static void register(IEventBus modBus) {
        ATTACHMENT_TYPES.register(modBus);
    }
}
