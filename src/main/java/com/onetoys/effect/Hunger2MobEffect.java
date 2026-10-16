package com.onetoys.effect;

import com.onetoys.api.PerestroykaHungerAPI;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

public class Hunger2MobEffect extends MobEffect {
    public Hunger2MobEffect(MobEffectCategory category, int color) {
        super(category, color);
    }

    @Override
    public boolean applyEffectTick(LivingEntity entity, int amplifier) {
        if (entity instanceof Player player) {
            // Deplete exhaustion like vanilla hunger, scaled with amplifier
            if (!player.level().isClientSide) {
                PerestroykaHungerAPI.addHungerExhaustion(player, 0.005F * (float) (amplifier + 1));
            }
            if (player.isSprinting()) {
                player.setSprinting(false);
            }
        }
        return true;
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return true;
    }
}

