package com.onetoys.mixin;

import com.onetoys.module.gameplay.wetness.WetnessTweaks;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
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
}
