package com.onetoys.item;

import com.onetoys.Onetoys;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.level.block.Block;

public class DumbbellItem extends BlockItem {
    public static final ResourceLocation ATTACK_DAMAGE_ID = ResourceLocation.fromNamespaceAndPath(Onetoys.MODID, "dumbbell_attack_damage");
    public static final ResourceLocation GRAVITY_MAINHAND_ID = ResourceLocation.fromNamespaceAndPath(Onetoys.MODID, "dumbbell_gravity_mainhand");
    public static final ResourceLocation GRAVITY_OFFHAND_ID = ResourceLocation.fromNamespaceAndPath(Onetoys.MODID, "dumbbell_gravity_offhand");

    public DumbbellItem(Block block, Item.Properties properties) {
        super(block, properties);
    }

    @Override
    public void appendHoverText(net.minecraft.world.item.ItemStack stack, Item.TooltipContext context, java.util.List<net.minecraft.network.chat.Component> tooltipComponents, net.minecraft.world.item.TooltipFlag tooltipFlag) {
        // Tooltips explicitly removed per design specification
    }

    public static ItemAttributeModifiers createAttributes(double gravityMultiplier) {
        return ItemAttributeModifiers.builder()
                .add(
                        Attributes.ATTACK_DAMAGE,
                        new AttributeModifier(
                                ATTACK_DAMAGE_ID,
                                1.0, // Base player attack damage is 1.0; +1.0 modifier results in 2.0 total attack damage
                                AttributeModifier.Operation.ADD_VALUE
                        ),
                        EquipmentSlotGroup.MAINHAND
                )
                .add(
                        Attributes.GRAVITY,
                        new AttributeModifier(
                                GRAVITY_MAINHAND_ID,
                                gravityMultiplier,
                                AttributeModifier.Operation.ADD_MULTIPLIED_BASE
                        ),
                        EquipmentSlotGroup.MAINHAND
                )
                .add(
                        Attributes.GRAVITY,
                        new AttributeModifier(
                                GRAVITY_OFFHAND_ID,
                                gravityMultiplier,
                                AttributeModifier.Operation.ADD_MULTIPLIED_BASE
                        ),
                        EquipmentSlotGroup.OFFHAND
                )
                .build()
                .withTooltip(false);
    }
}
