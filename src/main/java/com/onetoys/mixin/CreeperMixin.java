package com.onetoys.mixin;

import com.onetoys.module.gameplay.wetness.WetnessTweaks;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.monster.Creeper;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Creeper.class)
public abstract class CreeperMixin {

    @Shadow
    @Final
    private static EntityDataAccessor<Boolean> DATA_IS_IGNITED;

    @Shadow
    private int swell;

    @Shadow
    public abstract void setSwellDir(int dir);

    @Shadow
    public abstract int getSwellDir();

    @Shadow
    public abstract boolean isIgnited();

    @Unique
    private boolean Onetoys$isDeflatingFromFizzle = false;

    @Unique
    private long Onetoys$fizzleCooldownUntil = 0L;

    @Inject(method = "explodeCreeper", at = @At("HEAD"), cancellable = true)
    private void Onetoys$cancelWetExplosion(CallbackInfo ci) {
        if (WetnessTweaks.isWetCreepersCannotExplode()) {
            Creeper creeper = (Creeper) (Object) this;
            if (WetnessTweaks.isWet(creeper)) {
                ci.cancel();

                // Clear ignited state from flint & steel
                if (this.isIgnited()) {
                    creeper.getEntityData().set(DATA_IS_IGNITED, false);
                }

                // Trigger smooth deflation back to normal size
                this.Onetoys$isDeflatingFromFizzle = true;
                this.Onetoys$fizzleCooldownUntil = creeper.level().getGameTime() + 60L;
                this.setSwellDir(-1);

                // Play steam burst and extinguisher audio
                if (creeper.level() instanceof ServerLevel serverLevel) {
                    serverLevel.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, creeper.getX(), creeper.getY() + creeper.getBbHeight() * 0.7, creeper.getZ(), 8, 0.2, 0.2, 0.2, 0.03);
                    serverLevel.sendParticles(ParticleTypes.SMOKE, creeper.getX(), creeper.getY() + creeper.getBbHeight() * 0.7, creeper.getZ(), 10, 0.2, 0.2, 0.2, 0.05);
                    creeper.playSound(SoundEvents.FIRE_EXTINGUISH, 0.8F, 1.2F);
                }
            }
        }
    }

    @Inject(method = "setSwellDir", at = @At("HEAD"), cancellable = true)
    private void Onetoys$enforceDeflation(int dir, CallbackInfo ci) {
        if (dir > 0 && (this.Onetoys$isDeflatingFromFizzle || this.Onetoys$fizzleCooldownUntil > 0L)) {
            Creeper creeper = (Creeper) (Object) this;
            long gameTime = creeper.level().getGameTime();
            if (this.Onetoys$isDeflatingFromFizzle || gameTime < this.Onetoys$fizzleCooldownUntil) {
                ci.cancel();
            } else {
                this.Onetoys$fizzleCooldownUntil = 0L;
            }
        }
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void Onetoys$tickFizzleDeflation(CallbackInfo ci) {
        if (this.Onetoys$isDeflatingFromFizzle) {
            Creeper creeper = (Creeper) (Object) this;
            if (this.swell <= 0) {
                this.Onetoys$isDeflatingFromFizzle = false;
            } else if (creeper.tickCount % 5 == 0 && creeper.level() instanceof ServerLevel serverLevel) {
                // Occasional steam wisp while visibly shrinking back to normal
                serverLevel.sendParticles(ParticleTypes.SMOKE, creeper.getX(), creeper.getY() + creeper.getBbHeight() * 0.6, creeper.getZ(), 1, 0.1, 0.1, 0.1, 0.01);
            }
        }
    }
}
