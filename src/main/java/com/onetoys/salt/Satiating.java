package com.onetoys.salt;

import net.minecraft.core.component.DataComponents;
import net.minecraft.util.Unit;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

public class Satiating {
    private static final String SATIATING_KEY = "Satiating";

    public static boolean isSatiating(ItemStack itemStack) {
        if (itemStack == null || itemStack.isEmpty()) {
            return false;
        }
        if (itemStack.has(SaltDataComponents.SATIATING.get())) {
            return true;
        }
        CustomData customData = itemStack.get(DataComponents.CUSTOM_DATA);
        return customData != null && customData.contains(SATIATING_KEY);
    }

    public static ItemStack setSatiating(ItemStack itemStack) {
        if (itemStack == null || itemStack.isEmpty()) {
            return itemStack;
        }
        itemStack.set(SaltDataComponents.SATIATING.get(), Unit.INSTANCE);
        return itemStack;
    }

    public static float getDurationMultiplier(ItemStack stack) {
        if (SaltConfiguration.SATIATING_DURATION_MULTIPLIER != null && SaltConfiguration.isLoaded()) {
            return SaltConfiguration.SATIATING_DURATION_MULTIPLIER.get().floatValue();
        }
        return 2.0f;
    }
}

