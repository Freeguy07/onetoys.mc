package com.onetoys.mixin;

import com.onetoys.Onetoys;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LocalPlayer.class)
public class LocalPlayerMixin
{
    /**
     * Prevents normal vanilla or Valheim hunger/food mechanics from blocking or cancelling sprinting/running,
     * but enforces sprint disabling if afflicted with Hunger 2.
     */
    @Inject(at = @At("HEAD"), method = "hasEnoughFoodToStartSprinting", cancellable = true)
    public void canStartSprinting(CallbackInfoReturnable<Boolean> cir)
    {
        if (!com.onetoys.module.gameplay.FoodOverhaulModule.isFoodOverhaulEnabled()) return;
        LocalPlayer player = (LocalPlayer) (Object) this;
        if (player.isCreative())
        {
            cir.setReturnValue(true);
            return;
        }
        if (player.hasEffect(Onetoys.HUNGER_2))
        {
            cir.setReturnValue(false);
            return;
        }

        cir.setReturnValue(true);
    }
}

