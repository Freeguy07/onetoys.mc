package com.onetoys.item;

import java.util.List;

import com.onetoys.module.gameplay.GameplayTweaksModule;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.Tool;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.common.ItemAbility;

public class ClubItem extends PrimitiveWeaponItem {

    public ClubItem(Item.Properties properties) {
        super(properties);
    }

    public static ItemAttributeModifiers createAttributes() {
        return ItemAttributeModifiers.builder()
                .add(
                        Attributes.ATTACK_DAMAGE,
                        new AttributeModifier(
                                BASE_ATTACK_DAMAGE_ID,
                                7.0D, // Base player attack damage is 1.0; 1.0 + 7.0 = 8.0 total attack damage (less than stone axe's 9.0)
                                AttributeModifier.Operation.ADD_VALUE
                        ),
                        EquipmentSlotGroup.MAINHAND
                )
                .add(
                        Attributes.ATTACK_SPEED,
                        new AttributeModifier(
                                BASE_ATTACK_SPEED_ID,
                                -3.4D, // Base player attack speed is 4.0; 4.0 - 3.4 = 0.6 attack speed (slower cooldown than stone axe's 0.8)
                                AttributeModifier.Operation.ADD_VALUE
                        ),
                        EquipmentSlotGroup.MAINHAND
                )
                .build();
    }

    public static Tool createToolProperties() {
        return new Tool(
                List.of(
                        Tool.Rule.deniesDrops(BlockTags.INCORRECT_FOR_STONE_TOOL),
                        Tool.Rule.minesAndDrops(BlockTags.MINEABLE_WITH_PICKAXE, 4.0F),
                        Tool.Rule.minesAndDrops(BlockTags.MINEABLE_WITH_AXE, 4.0F),
                        Tool.Rule.minesAndDrops(BlockTags.MINEABLE_WITH_SHOVEL, 4.0F)
                ),
                1.0F,
                1
        );
    }

    @Override
    public boolean canAttackBlock(BlockState state, Level level, BlockPos pos, Player player) {
        if (!GameplayTweaksModule.isClubEnabled()) {
            return true;
        }
        return !player.isCreative();
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        return GameplayTweaksModule.isClubEnabled();
    }

    @Override
    public void postHurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (GameplayTweaksModule.isClubEnabled()) {
            stack.hurtAndBreak(1, attacker, EquipmentSlot.MAINHAND);
        }
    }

    @Override
    public boolean canDisableShield(ItemStack stack, ItemStack shield, LivingEntity entity, LivingEntity attacker) {
        return GameplayTweaksModule.isClubEnabled();
    }

    @Override
    public boolean canPerformAction(ItemStack stack, ItemAbility itemAbility) {
        if (!GameplayTweaksModule.isClubEnabled()) {
            return false;
        }
        return itemAbility == ItemAbilities.PICKAXE_DIG
                || itemAbility == ItemAbilities.AXE_DIG
                || itemAbility == ItemAbilities.SHOVEL_DIG;
    }

    @Override
    public boolean isValidRepairItem(ItemStack stack, ItemStack repairCandidate) {
        if (!GameplayTweaksModule.isClubEnabled()) {
            return false;
        }
        return super.isValidRepairItem(stack, repairCandidate);
    }

    @Override
    public int getEnchantmentValue(ItemStack stack) {
        return 5; // Stone tier enchantment value
    }

    @Override
    @SuppressWarnings("deprecation")
    public int getEnchantmentValue() {
        return 5;
    }
}
