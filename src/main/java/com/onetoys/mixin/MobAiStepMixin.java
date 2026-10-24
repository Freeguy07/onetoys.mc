package com.onetoys.mixin;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.onetoys.module.gameplay.mobs.MobAiTweaks;
import javax.annotation.Nullable;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.raid.Raider;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Mob.class)
public abstract class MobAiStepMixin {

    @Shadow
    @Nullable
    public abstract LivingEntity getTarget();

    @Unique
    private int Onetoys$lastSelectorCheckTick = -1;

    @Unique
    private boolean Onetoys$cachedShouldTick = true;

    @Unique
    private int Onetoys$cachedInterval = 1;

    @Unique
    private int Onetoys$nextDistanceCheckTick = 0;

    @Unique
    private boolean Onetoys$shouldTickSelectors(Mob mob) {
        if (this.Onetoys$lastSelectorCheckTick == mob.tickCount) {
            return this.Onetoys$cachedShouldTick;
        }
        this.Onetoys$lastSelectorCheckTick = mob.tickCount;

        if (!MobAiTweaks.isEnableDistanceAiThrottling()) {
            return this.Onetoys$cachedShouldTick = true;
        }

        if (mob.getTarget() != null
                || mob.hurtTime > 0
                || !mob.canChangeDimensions(mob.level(), mob.level())
                || mob.hasCustomName()
                || (mob instanceof Raider raider && raider.hasActiveRaid())) {
            this.Onetoys$cachedInterval = 1;
            this.Onetoys$nextDistanceCheckTick = mob.tickCount + 10;
            return this.Onetoys$cachedShouldTick = true;
        }

        if (mob.tickCount >= this.Onetoys$nextDistanceCheckTick) {
            this.Onetoys$nextDistanceCheckTick = mob.tickCount + 10;

            Player nearest = mob.level().getNearestPlayer(mob.getX(), mob.getY(), mob.getZ(), 128.0D, false);
            if (nearest == null) {
                this.Onetoys$cachedInterval = 10;
            } else {
                double distSqr = mob.distanceToSqr(nearest);
                int startDist = MobAiTweaks.getAiThrottleStartDistance();
                double startDistSqr = (double) (startDist * startDist);
                if (distSqr <= startDistSqr) {
                    this.Onetoys$cachedInterval = 1;
                } else {
                    double maxDistSqr = 128.0D * 128.0D;
                    if (startDistSqr >= maxDistSqr) {
                        this.Onetoys$cachedInterval = 2;
                    } else {
                        double ratio = Math.min(1.0D, (distSqr - startDistSqr) / (maxDistSqr - startDistSqr));
                        int rawInterval = 2 + (int) Math.round(ratio * 6.0D);
                        this.Onetoys$cachedInterval = Math.max(2, rawInterval & ~1);
                    }
                }
            }
        }

        if (this.Onetoys$cachedInterval <= 1) {
            return this.Onetoys$cachedShouldTick = true;
        }

        return this.Onetoys$cachedShouldTick = (((mob.tickCount + mob.getId()) % this.Onetoys$cachedInterval) == 0);
    }

    @WrapWithCondition(
            method = "serverAiStep",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/ai/goal/GoalSelector;tick()V")
    )
    private boolean Onetoys$throttleSelectorTick(GoalSelector instance) {
        return this.Onetoys$shouldTickSelectors((Mob) (Object) this);
    }
}
