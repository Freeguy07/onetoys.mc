package com.onetoys.item;

import java.util.List;

import com.onetoys.module.gameplay.GameplayTweaksModule;
import com.onetoys.module.gameplay.items.SwordBlockingTweaks;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.Tool;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.common.ItemAbility;

public class MacheteItem extends PrimitiveWeaponItem {

    public MacheteItem(Item.Properties properties) {
        super(properties);
    }

    public static ItemAttributeModifiers createAttributes() {
        return ItemAttributeModifiers.builder()
                .add(
                        Attributes.ATTACK_DAMAGE,
                        new AttributeModifier(
                                BASE_ATTACK_DAMAGE_ID,
                                4.0D, // Base player attack damage is 1.0; 1.0 + 4.0 = 5.0 total attack damage
                                AttributeModifier.Operation.ADD_VALUE
                        ),
                        EquipmentSlotGroup.MAINHAND
                )
                .build();
    }

    public static Tool createToolProperties() {
        return new Tool(
                List.of(
                        Tool.Rule.minesAndDrops(List.of(Blocks.COBWEB), 15.0F),
                        Tool.Rule.overrideSpeed(BlockTags.LEAVES, 15.0F),
                        Tool.Rule.overrideSpeed(BlockTags.SWORD_EFFICIENT, 1.5F)
                ),
                1.0F,
                2
        );
    }

    @Override
    public boolean canAttackBlock(BlockState state, Level level, BlockPos pos, Player player) {
        if (!GameplayTweaksModule.isMacheteEnabled()) {
            return true;
        }
        return !player.isCreative();
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        return GameplayTweaksModule.isMacheteEnabled();
    }

    @Override
    public void postHurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (GameplayTweaksModule.isMacheteEnabled()) {
            stack.hurtAndBreak(1, attacker, EquipmentSlot.MAINHAND);
        }
    }

    @Override
    public boolean canPerformAction(ItemStack stack, ItemAbility itemAbility) {
        if (!GameplayTweaksModule.isMacheteEnabled()) {
            return false;
        }
        // Exclude SWORD_SWEEP so sweeping edge and sweep attacks are not performed
        return itemAbility == ItemAbilities.SWORD_DIG;
    }

    @Override
    public boolean isValidRepairItem(ItemStack stack, ItemStack repairCandidate) {
        if (!GameplayTweaksModule.isMacheteEnabled()) {
            return false;
        }
        return super.isValidRepairItem(stack, repairCandidate);
    }

    @Override
    public int getEnchantmentValue(ItemStack stack) {
        return 14;
    }

    @Override
    @SuppressWarnings("deprecation")
    public int getEnchantmentValue() {
        return 14;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        if (GameplayTweaksModule.isMacheteEnabled() && SwordBlockingTweaks.canBlock(stack)) {
            return UseAnim.BLOCK;
        }
        return super.getUseAnimation(stack);
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        if (GameplayTweaksModule.isMacheteEnabled() && SwordBlockingTweaks.canBlock(stack)) {
            return 72000;
        }
        return super.getUseDuration(stack, entity);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand usedHand) {
        ItemStack stack = player.getItemInHand(usedHand);
        if (GameplayTweaksModule.isMacheteEnabled() && SwordBlockingTweaks.canBlock(stack)) {
            if (player.getCooldowns().isOnCooldown(this)) {
                return InteractionResultHolder.pass(stack);
            }
            if (usedHand == InteractionHand.MAIN_HAND) {
                ItemStack offhand = player.getOffhandItem();
                if (!offhand.isEmpty()) {
                    if (offhand.canPerformAction(ItemAbilities.SHIELD_BLOCK)
                            && !player.getCooldowns().isOnCooldown(offhand.getItem())) {
                        return InteractionResultHolder.pass(stack);
                    }
                    FoodProperties food = offhand.getFoodProperties(player);
                    if (food != null && player.canEat(food.canAlwaysEat())) {
                        return InteractionResultHolder.pass(stack);
                    }
                }
            }
            player.startUsingItem(usedHand);
            return InteractionResultHolder.consume(stack);
        }
        return super.use(level, player, usedHand);
    }
}
