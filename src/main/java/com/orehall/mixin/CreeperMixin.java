package com.orehall.mixin;

import com.orehall.module.gameplay.wetness.WetnessTweaks;
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
    private boolean orehall$isDeflatingFromFizzle = false;

    @Unique
    private long orehall$fizzleCooldownUntil = 0L;

    @Inject(method = "explodeCreeper", at = @At("HEAD"), cancellable = true)
    private void orehall$cancelWetExplosion(CallbackInfo ci) {
        if (WetnessTweaks.isWetCreepersCannotExplode()) {
            Creeper creeper = (Creeper) (Object) this;
            if (WetnessTweaks.isWet(creeper)) {
                ci.cancel();

                // Clear ignited state from flint & steel
                if (this.isIgnited()) {
                    creeper.getEntityData().set(DATA_IS_IGNITED, false);
                }

                // Trigger smooth deflation back to normal size
                this.orehall$isDeflatingFromFizzle = true;
                this.orehall$fizzleCooldownUntil = creeper.level().getGameTime() + 60L;
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
    private void orehall$enforceDeflation(int dir, CallbackInfo ci) {
        if (dir > 0 && (this.orehall$isDeflatingFromFizzle || this.orehall$fizzleCooldownUntil > 0L)) {
            Creeper creeper = (Creeper) (Object) this;
            long gameTime = creeper.level().getGameTime();
            if (this.orehall$isDeflatingFromFizzle || gameTime < this.orehall$fizzleCooldownUntil) {
                ci.cancel();
            } else {
                this.orehall$fizzleCooldownUntil = 0L;
            }
        }
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void orehall$tickFizzleDeflation(CallbackInfo ci) {
        if (this.orehall$isDeflatingFromFizzle) {
            Creeper creeper = (Creeper) (Object) this;
            if (this.swell <= 0) {
                this.orehall$isDeflatingFromFizzle = false;
            } else if (creeper.tickCount % 5 == 0 && creeper.level() instanceof ServerLevel serverLevel) {
                // Occasional steam wisp while visibly shrinking back to normal
                serverLevel.sendParticles(ParticleTypes.SMOKE, creeper.getX(), creeper.getY() + creeper.getBbHeight() * 0.6, creeper.getZ(), 1, 0.1, 0.1, 0.1, 0.01);
            }
        }
    }
}
