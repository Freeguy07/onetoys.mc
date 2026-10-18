package com.onetoys.mixin;

import com.onetoys.module.gameplay.mobs.SpecialMobTweaks;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Slime.class)
public abstract class SlimeMixin {

    @Inject(method = "jumpFromGround", at = @At("HEAD"), cancellable = true)
    private void Onetoys$slipperySlimeSlidePulse(CallbackInfo ci) {
        Slime slime = (Slime) (Object) this;
        if (SpecialMobTweaks.isSlipperySlimeEnabled()
                && SpecialMobTweaks.isSpecialMob(slime, SpecialMobTweaks.TYPE_SLIPPERY_SLIME)) {
            float yawRad = slime.getYRot() * Mth.DEG_TO_RAD;
            double pulseSpeed = 0.34D + 0.04D * Math.min(4, slime.getSize());
            double dx = -Mth.sin(yawRad) * pulseSpeed;
            double dz = Mth.cos(yawRad) * pulseSpeed;
            Vec3 current = slime.getDeltaMovement();
            double dy = current.y > 0.0D ? 0.0D : current.y;

            slime.setDeltaMovement(dx, dy, dz);
            slime.hasImpulse = true;
            slime.targetSquish = 0.45F;
            slime.playSound(SoundEvents.SLIME_SQUISH_SMALL, 0.8F, 0.9F + slime.getRandom().nextFloat() * 0.2F);
            ci.cancel();
        }
    }
}
