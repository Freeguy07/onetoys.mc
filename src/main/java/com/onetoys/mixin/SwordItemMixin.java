package com.onetoys.mixin;

import com.onetoys.module.gameplay.items.SwordBlockingTweaks;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TieredItem;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.ItemAbilities;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(SwordItem.class)
public abstract class SwordItemMixin extends TieredItem {

    public SwordItemMixin(Tier tier, Properties properties) {
        super(tier, properties);
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        if (SwordBlockingTweaks.isTweakEnabled()) {
            return UseAnim.BLOCK;
        }
        return super.getUseAnimation(stack);
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        if (SwordBlockingTweaks.isTweakEnabled()) {
            return 72000;
        }
        return super.getUseDuration(stack, entity);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand usedHand) {
        if (SwordBlockingTweaks.isTweakEnabled()) {
            ItemStack stack = player.getItemInHand(usedHand);
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
