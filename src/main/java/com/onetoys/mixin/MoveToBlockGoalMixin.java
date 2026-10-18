package com.onetoys.mixin;

import com.onetoys.module.gameplay.mobs.MobAiTweaks;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.MoveToBlockGoal;
import net.minecraft.world.entity.ai.goal.RemoveBlockGoal;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MoveToBlockGoal.class)
public abstract class MoveToBlockGoalMixin {

    @Shadow
    protected int nextStartTick;

    @Shadow
    @Final
    protected PathfinderMob mob;

    @Inject(method = "nextStartTick(Lnet/minecraft/world/entity/PathfinderMob;)I", at = @At("RETURN"), cancellable = true)
    private void Onetoys$throttleZombieEggScan(PathfinderMob mob, CallbackInfoReturnable<Integer> cir) {
        if (MobAiTweaks.isThrottleZombieEggScan() && (Object) this instanceof RemoveBlockGoal) {
            if (mob.getTarget() != null) {
                cir.setReturnValue(200);
            } else {
                cir.setReturnValue(cir.getReturnValue() * 4);
            }
        }
    }
}
