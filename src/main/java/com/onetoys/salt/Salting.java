package com.onetoys.salt;

import net.minecraft.core.component.DataComponents;
import net.minecraft.util.Unit;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

public class Salting {

    private static final String SALTED_KEY = "Salted";

    public static boolean isSalted(ItemStack itemStack) {
        if (itemStack == null || itemStack.isEmpty()) {
            return false;
        }
        if (itemStack.has(SaltDataComponents.SALTED.get())) {
            return true;
        }
        CustomData customData = itemStack.get(DataComponents.CUSTOM_DATA);
        return customData != null && customData.contains(SALTED_KEY);
    }

    /**
     * Same ItemStack is returned.
     */
    public static ItemStack setSalted(ItemStack itemStack) {
        if (itemStack == null || itemStack.isEmpty()) {
            return itemStack;
        }
        itemStack.set(SaltDataComponents.SALTED.get(), Unit.INSTANCE);
        return itemStack;
    }

    public static int getBonusHearts(ItemStack stack) {
        if (SaltConfiguration.SALTING_BONUS_HEARTS != null && SaltConfiguration.isLoaded()) {
            return SaltConfiguration.SALTING_BONUS_HEARTS.get();
        }
        return 1;
    }
}

