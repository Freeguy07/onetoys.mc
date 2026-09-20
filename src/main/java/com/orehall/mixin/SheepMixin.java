package com.orehall.mixin;

import com.orehall.module.gameplay.wetness.WetnessTweaks;
import com.orehall.registry.OrehallItems;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.entity.item.ItemEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Sheep.class)
public abstract class SheepMixin {

    @Shadow
    public abstract void setSheared(boolean sheared);

    @Inject(method = "shear", at = @At("HEAD"), cancellable = true)
    private void orehall$shearWetSheep(SoundSource soundSource, CallbackInfo ci) {
        if (WetnessTweaks.isMattedWoolFromWetSheep()) {
            Sheep sheep = (Sheep) (Object) this;
            if (WetnessTweaks.isWet(sheep)) {
                ci.cancel();
                sheep.level().playSound(null, sheep, SoundEvents.SHEEP_SHEAR, soundSource, 1.0F, 1.0F);
                this.setSheared(true);
                if (!sheep.level().isClientSide()) {
                    int count = 1 + sheep.getRandom().nextInt(3);
                    for (int i = 0; i < count; i++) {
                        ItemEntity itemEntity = sheep.spawnAtLocation(OrehallItems.MATTED_WOOL.get(), 1);
                        if (itemEntity != null) {
                            itemEntity.setDeltaMovement(
                                    itemEntity.getDeltaMovement().add(
                                            (sheep.getRandom().nextFloat() - sheep.getRandom().nextFloat()) * 0.1F,
                                            sheep.getRandom().nextFloat() * 0.05F,
                                            (sheep.getRandom().nextFloat() - sheep.getRandom().nextFloat()) * 0.1F
                                    )
                            );
                        }
                    }
                }
            }
        }
    }
}
