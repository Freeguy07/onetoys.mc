package com.onetoys.salt;

import net.minecraft.core.component.DataComponents;
import net.minecraft.util.Unit;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

public class Sweetening {
    private static final String SWEETENED_KEY = "Sweetened";

    public static boolean isSweetened(ItemStack itemStack) {
        if (itemStack == null || itemStack.isEmpty()) {
            return false;
        }
        if (itemStack.has(SaltDataComponents.SWEETENED.get())) {
            return true;
        }
        CustomData customData = itemStack.get(DataComponents.CUSTOM_DATA);
        return customData != null && customData.contains(SWEETENED_KEY);
    }

    public static ItemStack setSweetened(ItemStack itemStack) {
        if (itemStack == null || itemStack.isEmpty()) {
            return itemStack;
        }
        itemStack.set(SaltDataComponents.SWEETENED.get(), Unit.INSTANCE);
        return itemStack;
    }

    public static float getHealthRegenBonus(ItemStack stack) {
        if (SaltConfiguration.SWEETENING_REGEN_BONUS != null && SaltConfiguration.isLoaded()) {
            return SaltConfiguration.SWEETENING_REGEN_BONUS.get().floatValue();
        }
        return 0.25f;
    }
}

