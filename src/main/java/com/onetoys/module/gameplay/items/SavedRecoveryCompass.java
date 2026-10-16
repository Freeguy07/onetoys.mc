package com.onetoys.module.gameplay.items;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.world.item.ItemStack;

public record SavedRecoveryCompass(int slot, ItemStack stack) {
    public static final Codec<SavedRecoveryCompass> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.INT.fieldOf("slot").forGetter(SavedRecoveryCompass::slot),
                    ItemStack.CODEC.fieldOf("stack").forGetter(SavedRecoveryCompass::stack)
            ).apply(instance, SavedRecoveryCompass::new)
    );
}
