package com.onetoys.item;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Base class for the family of primitive weapons: spear, club, and machete.
 * All primitive weapons share common characteristics including repairability with flint.
 */
public abstract class PrimitiveWeaponItem extends Item {

    public PrimitiveWeaponItem(Item.Properties properties) {
        super(properties);
    }

    @Override
    public boolean isValidRepairItem(ItemStack stack, ItemStack repairCandidate) {
        return repairCandidate.is(Items.FLINT) || super.isValidRepairItem(stack, repairCandidate);
    }
}
