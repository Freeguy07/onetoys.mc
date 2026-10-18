package com.onetoys.mixin;

import com.onetoys.Onetoys;
import com.onetoys.module.gameplay.items.SwordBlockingTweaks;
import com.onetoys.sol_valheim.SOLValheim;
import com.onetoys.sol_valheim.ValheimFoodData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.event.EventHooks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public abstract class PlayerEntityMixin extends LivingEntity {

    protected PlayerEntityMixin(EntityType<? extends LivingEntity> entityType, Level level) {
        super(entityType, level);
    }

    @Inject(method = "hurtCurrentlyUsedShield(F)V", at = @At("HEAD"), cancellable = true)
    private void onetoys$hurtCurrentlyUsedShield(float damage, CallbackInfo ci) {
        if (!SwordBlockingTweaks.isTweakEnabled()) {
            return;
        }
        ci.cancel();
        if (!this.useItem.canPerformAction(ItemAbilities.SHIELD_BLOCK) || SwordBlockingTweaks.isSword(this.useItem)) {
            return;
        }
        Player player = (Player) (Object) this;
        if (!this.level().isClientSide) {
            player.awardStat(Stats.ITEM_USED.get(this.useItem.getItem()));
        }
        int durabilityDamage = SwordBlockingTweaks.calculateBlockDurabilityDamage(damage);
        if (durabilityDamage > 0) {
            InteractionHand interactionHand = this.getUsedItemHand();
            if (this.level() instanceof ServerLevel serverLevel && !this.hasInfiniteMaterials()) {
                this.useItem.hurtAndBreak(durabilityDamage, serverLevel, this, item -> {
                    this.onEquippedItemBroken(item, getSlotForHand(interactionHand));
                    EventHooks.onPlayerDestroyItem(player, this.useItem, interactionHand);
                    this.stopUsingItem();
                });
            }
            if (this.useItem.isEmpty()) {
                if (interactionHand == InteractionHand.MAIN_HAND) {
                    this.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
                } else {
                    this.setItemSlot(EquipmentSlot.OFFHAND, ItemStack.EMPTY);
                }
                this.useItem = ItemStack.EMPTY;
                this.playSound(SoundEvents.SHIELD_BREAK, 0.8F, 0.8F + this.level().random.nextFloat() * 0.4F);
            }
        }
    }

    @Inject(at = @At("HEAD"), method = "causeFoodExhaustion(F)V", cancellable = true)
    private void onAddExhaustion(float exhaustion, CallbackInfo info) {
        if (!com.onetoys.module.gameplay.FoodOverhaulModule.isFoodOverhaulEnabled()) return;
        info.cancel();
    }

    @Inject(at = @At("HEAD"), method = "jumpFromGround")
    private void onJumpFromGround(CallbackInfo info) {
        if (!com.onetoys.module.gameplay.FoodOverhaulModule.isFoodOverhaulEnabled()) return;
        Player player = (Player) (Object) this;
        if (player.isCreative()) return;
        ValheimFoodData foodData = player.getData(SOLValheim.FOOD_DATA);
        if (foodData != null && !foodData.hasActiveFoods()) {
            foodData.addExhaustion(player, player.isSprinting() ? 0.05F : 0.02F);
        }
    }

    @Inject(at = @At("HEAD"), method = "canEat(Z)Z", cancellable = true)
    private void onCanConsume(boolean ignorehunger, CallbackInfoReturnable<Boolean> info) {
        if (!com.onetoys.module.gameplay.FoodOverhaulModule.isFoodOverhaulEnabled()) return;
        info.setReturnValue(true);
    }

    @Inject(at = @At("HEAD"), method = "canSprint", cancellable = true)
    private void onCanSprint(CallbackInfoReturnable<Boolean> info) {
        if (!com.onetoys.module.gameplay.FoodOverhaulModule.isFoodOverhaulEnabled()) return;
        Player player = (Player) (Object) this;
        if (player.isCreative()) return;
        if (player.hasEffect(Onetoys.HUNGER_2)) {
            info.setReturnValue(false);
        }
    }
}
