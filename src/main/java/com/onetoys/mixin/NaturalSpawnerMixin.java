package com.onetoys.mixin;

import com.onetoys.module.gameplay.mobs.MobAiTweaks;
import net.minecraft.world.level.NaturalSpawner;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(NaturalSpawner.class)
public abstract class NaturalSpawnerMixin {

    @ModifyConstant(
            method = "isRightDistanceToPlayerAndSpawnPoint",
            constant = @Constant(doubleValue = 576.0D)
    )
    private static double Onetoys$modifyPlayerNoSpawnDistance(double original) {
        return MobAiTweaks.getPlayerNoSpawnDistanceSquared();
    }
}
