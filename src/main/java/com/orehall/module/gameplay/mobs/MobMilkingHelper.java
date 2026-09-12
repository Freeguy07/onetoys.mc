package com.orehall.module.gameplay.mobs;

import com.orehall.registry.OrehallAttachments;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.MushroomCow;
import net.minecraft.world.entity.animal.goat.Goat;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/**
 * Shared helper for applying milking cooldown and feeding mechanics across milkable mob types.
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

    public static void handleStew(PlayerInteractEvent.EntityInteract event, MushroomCow mooshroom, boolean isTweakEnabled, int cooldownTicks) {
        if (!isTweakEnabled || cooldownTicks <= 0 || mooshroom.isBaby()) {
            return;
        }

        long gameTime = event.getLevel().getGameTime();
        long nextStewTime = mooshroom.getData(OrehallAttachments.STEW_COOLDOWN);

        // If cooldown in config was reduced, clamp existing cooldown so mobs don't stay locked unexpectedly
        if (nextStewTime - gameTime > cooldownTicks) {
            nextStewTime = gameTime + cooldownTicks;
            if (!event.getLevel().isClientSide()) {
                mooshroom.setData(OrehallAttachments.STEW_COOLDOWN, nextStewTime);
            }
        }

        if (gameTime < nextStewTime) {
            // Mooshroom is currently on stew cooldown - cancel interaction and emit angry particles
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.sidedSuccess(event.getLevel().isClientSide()));

            if (event.getLevel() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(
                        ParticleTypes.ANGRY_VILLAGER,
                        mooshroom.getX(),
                        mooshroom.getY() + mooshroom.getBbHeight() * 0.65,
                        mooshroom.getZ(),
                        5,
                        mooshroom.getBbWidth() * 0.4,
                        mooshroom.getBbHeight() * 0.25,
                        mooshroom.getBbWidth() * 0.4,
                        0.02
                );

                mooshroom.playSound(SoundEvents.COW_AMBIENT, 0.7F, 0.7F);
            }
        } else {
            // Mooshroom is ready to be stew-milked - record next available timestamp on the server
            if (!event.getLevel().isClientSide()) {
                mooshroom.setData(OrehallAttachments.STEW_COOLDOWN, gameTime + cooldownTicks);
            }
        }
    }

    public static void handleFeeding(PlayerInteractEvent.EntityInteract event, Animal animal, boolean isFeedingResetEnabled) {
        if (!isFeedingResetEnabled || animal.isBaby()) {
            return;
        }

        ItemStack stack = event.getItemStack();
        if (!animal.isFood(stack)) {
            return;
        }

        long gameTime = event.getLevel().getGameTime();
        long nextMilkingTime = animal.getData(OrehallAttachments.MILK_COOLDOWN);
        long nextStewTime = (animal instanceof MushroomCow) ? animal.getData(OrehallAttachments.STEW_COOLDOWN) : 0L;

        // If the animal is not on any cooldown, let vanilla handle feeding/breeding normally
        if (nextMilkingTime <= gameTime && nextStewTime <= gameTime) {
            return;
        }

        Player player = event.getEntity();

        // If the animal can enter love mode (breed), let vanilla consume the item and handle love mode,
        // while also clearing both milking and stew cooldowns
        if (animal.canFallInLove()) {
            if (!event.getLevel().isClientSide()) {
                animal.setData(OrehallAttachments.MILK_COOLDOWN, 0L);
                if (animal instanceof MushroomCow) {
                    animal.setData(OrehallAttachments.STEW_COOLDOWN, 0L);
                }
                if (event.getLevel() instanceof ServerLevel serverLevel) {
                    serverLevel.sendParticles(
                            ParticleTypes.HAPPY_VILLAGER,
                            animal.getX(),
                            animal.getY() + animal.getBbHeight() * 0.65,
                            animal.getZ(),
                            5,
                            animal.getBbWidth() * 0.4,
                            animal.getBbHeight() * 0.25,
                            animal.getBbWidth() * 0.4,
                            0.02
                    );
                }
            }
            return;
        }

        // The animal is already in love or on breeding cooldown; vanilla would normally ignore the wheat.
        // Consume 1 food item to satisfy the animal, clear its cooldowns, and play eating feedback.
        if (!event.getLevel().isClientSide()) {
            animal.setData(OrehallAttachments.MILK_COOLDOWN, 0L);
            if (animal instanceof MushroomCow) {
                animal.setData(OrehallAttachments.STEW_COOLDOWN, 0L);
            }
            stack.consume(1, player);

            if (event.getLevel() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(
                        ParticleTypes.HAPPY_VILLAGER,
                        animal.getX(),
                        animal.getY() + animal.getBbHeight() * 0.65,
                        animal.getZ(),
                        7,
                        animal.getBbWidth() * 0.4,
                        animal.getBbHeight() * 0.25,
                        animal.getBbWidth() * 0.4,
                        0.02
                );
            }

            SoundEvent eatSound = (animal instanceof Goat) ? SoundEvents.GOAT_EAT : SoundEvents.GENERIC_EAT;
            animal.playSound(eatSound, 1.0F, 1.0F);
        }

        event.setCancellationResult(InteractionResult.sidedSuccess(event.getLevel().isClientSide()));
        event.setCanceled(true);
    }
}
