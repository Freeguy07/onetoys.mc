package com.onetoys.mixin;

import com.onetoys.Onetoys;
import com.onetoys.sol_valheim.SOLValheim;
import com.onetoys.sol_valheim.ValheimFoodData;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public class PlayerEntityMixin {

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

