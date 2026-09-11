package com.orehall.module.gameplay.mobs;

import com.orehall.registry.OrehallAttachments;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.goat.Goat;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/**
 * Shared helper for applying milking cooldown mechanics across milkable mob types.
 */
public final class MobMilkingHelper {

    private MobMilkingHelper() {}

    public static void handle(PlayerInteractEvent.EntityInteract event, LivingEntity entity, boolean isTweakEnabled, int cooldownTicks) {
        if (!isTweakEnabled || cooldownTicks <= 0 || entity.isBaby()) {
            return;
        }

        long gameTime = event.getLevel().getGameTime();
        long nextMilkingTime = entity.getData(OrehallAttachments.MILK_COOLDOWN);

        // If cooldown in config was reduced, clamp existing cooldown so mobs don't stay locked unexpectedly
        if (nextMilkingTime - gameTime > cooldownTicks) {
            nextMilkingTime = gameTime + cooldownTicks;
            if (!event.getLevel().isClientSide()) {
                entity.setData(OrehallAttachments.MILK_COOLDOWN, nextMilkingTime);
            }
        }

        if (gameTime < nextMilkingTime) {
            // Mob is currently on cooldown - cancel interaction and emit angry particles
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.sidedSuccess(event.getLevel().isClientSide()));

            if (event.getLevel() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(
                        ParticleTypes.ANGRY_VILLAGER,
                        entity.getX(),
                        entity.getY() + entity.getBbHeight() * 0.65,
                        entity.getZ(),
                        5,
                        entity.getBbWidth() * 0.4,
                        entity.getBbHeight() * 0.25,
                        entity.getBbWidth() * 0.4,
                        0.02
                );

                SoundEvent refusalSound = (entity instanceof Goat goat)
                        ? (goat.isScreamingGoat() ? SoundEvents.GOAT_SCREAMING_AMBIENT : SoundEvents.GOAT_AMBIENT)
                        : SoundEvents.COW_AMBIENT;
                entity.playSound(refusalSound, 0.7F, 0.7F);
            }
        } else {
            // Mob is ready to be milked - record next available timestamp on the server
            if (!event.getLevel().isClientSide()) {
                entity.setData(OrehallAttachments.MILK_COOLDOWN, gameTime + cooldownTicks);
            }
        }
    }
}
