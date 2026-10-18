package com.onetoys.mixin;

import com.onetoys.module.gameplay.wetness.WetnessTweaks;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin extends Entity {

    @Unique
    private boolean onetoys$cachedIsInColdWater = false;

    @Unique
    private int onetoys$nextColdWaterCheckTick = 0;

    public LivingEntityMixin(EntityType<?> entityType, Level level) {
        super(entityType, level);
    }

    @Inject(method = "aiStep", at = @At("HEAD"))
    private void onAiStep(CallbackInfo ci) {
        if (!WetnessTweaks.isColdWaterFreezingActive()) {
            this.onetoys$cachedIsInColdWater = false;
            return;
        }

        if (this.isInWaterOrBubble()) {
            LivingEntity entity = (LivingEntity) (Object) this;
            if (entity.tickCount >= this.onetoys$nextColdWaterCheckTick) {
                this.onetoys$nextColdWaterCheckTick = entity.tickCount + 10;
                this.onetoys$cachedIsInColdWater = WetnessTweaks.isInColdWater(entity);
            }
            if (this.onetoys$cachedIsInColdWater) {
                this.setIsInPowderSnow(true);
            }
        } else {
            this.onetoys$cachedIsInColdWater = false;
            this.onetoys$nextColdWaterCheckTick = 0;
        }
    }

    @Inject(method = "aiStep", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;removeFrost()V"))
    private void onUpdateFrost(CallbackInfo ci) {
        LivingEntity entity = (LivingEntity) (Object) this;
        if (entity.level().isClientSide() || entity.isDeadOrDying()) {
            return;
        }

        if (!(entity instanceof Player player)) {
            return;
        }

        if (player.isCreative() || player.isSpectator() || player.getAbilities().invulnerable || !player.canFreeze()) {
            WetnessTweaks.setWetFrozenTicks(player, 0);
            return;
        }

        // If primary freezing (powder snow, cold water, etc.) is active this tick,
        // wetness frost takes a back seat and is deferred until primary freezing ends.
        if (this.isInPowderSnow) {
            return;
        }

        int currentTicksFrozen = this.getTicksFrozen();
        int wetFrozen = WetnessTweaks.getWetFrozenTicks(player);

        // If the player's current frost is higher than their wetness frost level,
        // the main frost is actively thawing / passing. Wetness freezing remains deferred.
        if (currentTicksFrozen > wetFrozen) {
            return;
        }

        // Main frost has passed (or thawed down to the wetness frost baseline).
        boolean qualifiesForWetFreeze = WetnessTweaks.isWetPlayerFreezingActive()
                && WetnessTweaks.isWet(player)
                && WetnessTweaks.isColdBiome(player.level(), player.blockPosition())
                && !WetnessTweaks.isNearHeatSource(player);

        if (qualifiesForWetFreeze) {
            int interval = WetnessTweaks.getWetPlayerFreezingInterval();
            if (player.tickCount % interval == 0) {
                wetFrozen = Math.min(player.getTicksRequiredToFreeze(), wetFrozen + 1);
                WetnessTweaks.setWetFrozenTicks(player, wetFrozen);
            }
            if (currentTicksFrozen < wetFrozen) {
                this.setTicksFrozen(wetFrozen);
            }
        } else if (wetFrozen > 0) {
            // Thawing out when no longer wet, leaving cold biome, or warming near a heat source
            if (WetnessTweaks.isNearHeatSource(player) || player.isOnFire() || player.isInLava()) {
                wetFrozen = Math.max(0, wetFrozen - 4);
            } else {
                wetFrozen = Math.max(0, wetFrozen - 2);
            }
            WetnessTweaks.setWetFrozenTicks(player, wetFrozen);
            if (currentTicksFrozen < wetFrozen) {
                this.setTicksFrozen(wetFrozen);
            }
        }
    }
}
