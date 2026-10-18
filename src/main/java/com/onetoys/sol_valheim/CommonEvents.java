package com.onetoys.sol_valheim;

import com.onetoys.Onetoys;
import com.onetoys.api.PerestroykaHungerAPI;
import com.onetoys.module.gameplay.FoodOverhaulModule;
import com.onetoys.salt.SaltCriteriaTriggers;
import com.onetoys.salt.Salting;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.SleepFinishedTimeEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

@EventBusSubscriber(modid = Onetoys.MODID)
public class CommonEvents {

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!FoodOverhaulModule.isFoodOverhaulEnabled()) {
            Player player = event.getEntity();
            if (!player.level().isClientSide && player.isAlive()) {
                if (player.hasEffect(Onetoys.HUNGER_2)) {
                    player.removeEffect(Onetoys.HUNGER_2);
                }
                var maxHealth = player.getAttribute(Attributes.MAX_HEALTH);
                if (maxHealth != null && maxHealth.getBaseValue() != 20.0) {
                    maxHealth.setBaseValue(20.0);
                }
                var movementSpeed = player.getAttribute(Attributes.MOVEMENT_SPEED);
                if (movementSpeed != null && movementSpeed.hasModifier(SOLValheim.SPEED_BUFF_ID)) {
                    movementSpeed.removeModifier(SOLValheim.SPEED_BUFF_ID);
                }
            }
            return;
        }
        Player player = event.getEntity();
        if (player.level().isClientSide || !player.isAlive()) return;

        if (player.isCreative()) {
            // Remove any leftover hunger effects in creative mode
            if (player.hasEffect(Onetoys.HUNGER_2)) {
                player.removeEffect(Onetoys.HUNGER_2);
            }
            if (player.hasEffect(MobEffects.HUNGER)) {
                player.removeEffect(MobEffects.HUNGER);
            }
            var maxHealth = player.getAttribute(Attributes.MAX_HEALTH);
            if (maxHealth != null && maxHealth.getBaseValue() != 20.0) {
                maxHealth.setBaseValue(20.0);
            }
            var movementSpeed = player.getAttribute(Attributes.MOVEMENT_SPEED);
            if (movementSpeed != null && movementSpeed.hasModifier(SOLValheim.SPEED_BUFF_ID)) {
                movementSpeed.removeModifier(SOLValheim.SPEED_BUFF_ID);
            }
            return;
        }

        ValheimFoodData foodData = player.getData(SOLValheim.FOOD_DATA);

        boolean slowDigestion = player.hasEffect(Onetoys.SLOW_DIGESTION);
        boolean hadItems = !foodData.ItemEntries.isEmpty() || foodData.DrinkSlot != null;
        foodData.tick(slowDigestion);
        boolean hasItems = !foodData.ItemEntries.isEmpty() || foodData.DrinkSlot != null;

        // Immediate hunger: automatically drops the player's hunger timer to lowest value (0) immediately if they haven't eaten any food
        if (PerestroykaHungerAPI.isImmediateHungerActive() && !hasItems && foodData.hungerCountdown > 0) {
            PerestroykaHungerAPI.setHungerToLowest(player);
        }

        // Drain hunger points based on player activity when not on active food buffs
        if (!hasItems) {
            foodData.tickPlayerActions(player);
        }

        boolean wasDirty = foodData.isDirty();

        // Sync immediately if all items expired, if dirty (e.g. item expired or phase change), or on periodic interval (0.5s during hunger, 5s when full)
        if ((hadItems && !hasItems) || wasDirty || player.tickCount % (hasItems ? 100 : 10) == 0) {
            foodData.clearDirty();
            player.syncData(SOLValheim.FOOD_DATA);
        }

        // Apply hunger effects when starving
        if (foodData.isFirstHungerDepleted()) {
            if (foodData.isSecondTimerDepleted()) {
                // Second timer has run out: Hunger 2 effect is applied, first hunger effect is removed
                player.removeEffect(MobEffects.HUNGER);
                var hunger2 = player.getEffect(Onetoys.HUNGER_2);
                if (hunger2 == null || hunger2.getDuration() <= 40) {
                    player.addEffect(new MobEffectInstance(Onetoys.HUNGER_2, 100, 0, false, true, true));
                }
            } else {
                // Second timer is active: first hunger effect is applied, hunger 2 is removed
                player.removeEffect(Onetoys.HUNGER_2);
                var hunger = player.getEffect(MobEffects.HUNGER);
                if (hunger == null || hunger.getDuration() <= 40) {
                    player.addEffect(new MobEffectInstance(MobEffects.HUNGER, 100, 0, false, true, true));
                }
            }
        } else if (player.hasEffect(Onetoys.HUNGER_2)) {
            player.removeEffect(Onetoys.HUNGER_2);
        }

        if (hasItems && player.tickCount % 20 == 0) {
            for (var eaten : foodData.ItemEntries) {
                var config = ModConfig.getFoodConfig(eaten.item);
                if (config != null) {
                    applyFoodEffectsToPlayer(player, eaten, config);
                }
            }

            if (foodData.DrinkSlot != null) {
                var config = ModConfig.getFoodConfig(foodData.DrinkSlot.item);
                if (config != null) {
                    applyFoodEffectsToPlayer(player, foodData.DrinkSlot, config);
                }
            }
        }

        // Optimize health & attribute evaluation: execute on 10-tick interval (0.5s) or on food state changes
        boolean checkPeriodic = player.tickCount % 10 == 0;
        if (checkPeriodic || wasDirty || (hadItems != hasItems)) {
            var maxHealth = player.getAttribute(Attributes.MAX_HEALTH);
            if (maxHealth != null) {
                float maxHealthBonus = foodData.getTotalMaxHealthBonus();
                float healPerSecond = foodData.getTotalHealPerSecond();

                double baseStartingHealth = (SOLValheim.Config.common != null && SOLValheim.Config.common.startingHealth != null)
                        ? SOLValheim.Config.common.startingHealth.get() * 2.0 : 6.0;
                double maxAllowedBonus = (SOLValheim.Config.common != null && SOLValheim.Config.common.maxFoodHealth != null)
                        ? SOLValheim.Config.common.maxFoodHealth.get() * 2.0 : 200.0;

                // Check if player has Hunger 2 effect (starving)
                if (player.hasEffect(Onetoys.HUNGER_2)) {
                    // When starving with Hunger 2, lock max health to starting health (default 3 hearts = 6.0 HP)
                    if (maxHealth.getBaseValue() != baseStartingHealth) {
                        maxHealth.setBaseValue(baseStartingHealth);
                    }
                    // Also cap current health to baseStartingHealth
                    if (player.getHealth() > (float) baseStartingHealth) {
                        player.setHealth((float) baseStartingHealth);
                    }
                } else {
                    // Normal health calculation based on starting health + eaten foods, capped by maxFoodHealth
                    double targetMaxHealth = Math.min(maxAllowedBonus, baseStartingHealth + maxHealthBonus);
                    targetMaxHealth = Math.round(targetMaxHealth / 2.0) * 2.0; // round to full hearts
                    if (maxHealth.getBaseValue() != targetMaxHealth) {
                        maxHealth.setBaseValue(targetMaxHealth);
                    }
                }

                // Health regeneration from food buffs (every 10 ticks = 0.5s)
                // Combat cooldown: pauses after taking non-hunger damage for regenDelay ticks
                if (checkPeriodic) {
                    long lastDamageTime = foodData.lastDamageGameTime;
                    long gameTime = player.level().getGameTime();
                    int delay = (SOLValheim.Config.common != null && SOLValheim.Config.common.regenDelay != null)
                            ? SOLValheim.Config.common.regenDelay.get() : 80;
                    boolean inCombat = lastDamageTime >= 0L && lastDamageTime <= gameTime && (gameTime - lastDamageTime) < delay;

                    if (!inCombat && player.getHealth() < maxHealth.getValue() && player.getHealth() > 0) {
                        if (healPerSecond > 0f) {
                            float speedMod = (SOLValheim.Config.common != null && SOLValheim.Config.common.regenSpeedModifier != null)
                                    ? SOLValheim.Config.common.regenSpeedModifier.get().floatValue() : 1.0f;
                            player.heal(healPerSecond * 0.5f * speedMod); // 10 ticks = 0.5s
                        }
                    }
                }
            }

            var movementSpeed = player.getAttribute(Attributes.MOVEMENT_SPEED);
            if (movementSpeed != null) {
                double speedConfig = (SOLValheim.Config.common != null && SOLValheim.Config.common.speedBoost != null)
                        ? SOLValheim.Config.common.speedBoost.get() : 0.0;
                if (speedConfig > 0.0 && player.getHealth() >= player.getMaxHealth() && player.getMaxHealth() >= 20.0f) {
                    if (!movementSpeed.hasModifier(SOLValheim.SPEED_BUFF_ID)) {
                        movementSpeed.addTransientModifier(SOLValheim.getSpeedBuffModifier());
                    }
                } else if (movementSpeed.hasModifier(SOLValheim.SPEED_BUFF_ID)) {
                    movementSpeed.removeModifier(SOLValheim.SPEED_BUFF_ID);
                }
            }
        }
    }

    @SubscribeEvent
    public static void onBreakSpeed(PlayerEvent.BreakSpeed event) {
        if (!FoodOverhaulModule.isFoodOverhaulEnabled()) return;
        Player player = event.getEntity();
        if (player.isCreative()) return;
        if (player.hasEffect(Onetoys.HUNGER_2)) {
            // Hunger 2 slows down mining speed by 50%
            event.setNewSpeed(event.getNewSpeed() * 0.5f);
        } else if (player.hasEffect(MobEffects.HUNGER)) {
            // Normal hunger slows down mining speed by 25%
            event.setNewSpeed(event.getNewSpeed() * 0.75f);
        }
    }

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (!FoodOverhaulModule.isFoodOverhaulEnabled()) return;
        if (event.isCanceled()) return;
        Player player = event.getPlayer();
        if (player != null && !player.level().isClientSide && !player.isCreative()) {
            ValheimFoodData foodData = player.getData(SOLValheim.FOOD_DATA);
            if (foodData != null && !foodData.hasActiveFoods()) {
                foodData.addExhaustion(player, 0.02F); // ~4 hunger points per block broken
            }
        }
    }

    @SubscribeEvent
    public static void onAttackEntity(AttackEntityEvent event) {
        if (!FoodOverhaulModule.isFoodOverhaulEnabled()) return;
        if (event.isCanceled()) return;
        Player player = event.getEntity();
        if (player != null && !player.level().isClientSide && !player.isCreative()) {
            ValheimFoodData foodData = player.getData(SOLValheim.FOOD_DATA);
            if (foodData != null && !foodData.hasActiveFoods()) {
                foodData.addExhaustion(player, 0.1F); // ~20 hunger points per attack
            }
        }
    }

    private static void applyFoodEffectsToPlayer(Player player, ValheimFoodData.EatenFoodItem eatenData, ModConfig.Common.FoodConfig config) {
        int ticksLeft = eatenData.ticksLeft;

        for (var effectCfg : config.extraEffects) {
            Holder<MobEffect> mobEffect = effectCfg.getEffect();
            if (mobEffect == null) continue;

            int amplifier = Math.max(effectCfg.amplifier - 1, 0);
            float fractionActive = effectCfg.duration;
            int totalTime = config.getTime();
            int threshold = (int) (totalTime * fractionActive);

            if (ticksLeft >= (totalTime - threshold)) {
                player.addEffect(new net.minecraft.world.effect.MobEffectInstance(mobEffect, 120, amplifier, false, false));
            } else {
                player.removeEffect(mobEffect);
            }
        }
    }

    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (!FoodOverhaulModule.isFoodOverhaulEnabled()) return;
        Player player = event.getEntity();
        if (player.isCreative()) return;
        ItemStack stack = event.getItemStack();
        if (stack.isEmpty()) return;

        boolean isFood = stack.has(net.minecraft.core.component.DataComponents.FOOD);
        boolean isDrink = stack.getUseAnimation() == net.minecraft.world.item.UseAnim.DRINK;

        if (isFood || isDrink) {
            if (stack.is(Items.ROTTEN_FLESH)) return;

            ValheimFoodData foodData = player.getData(SOLValheim.FOOD_DATA);
            if (foodData != null && !foodData.canEat(stack)) {
                event.setCancellationResult(net.minecraft.world.InteractionResult.FAIL);
                event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent
    public static void onItemUseFinish(LivingEntityUseItemEvent.Finish event) {
        if (!FoodOverhaulModule.isFoodOverhaulEnabled()) return;
        LivingEntity entity = event.getEntity();
        if (entity instanceof Player player) {
            if (player.isCreative()) return;
            ItemStack stack = event.getItem();
            if (stack.has(net.minecraft.core.component.DataComponents.FOOD) || stack.getItem() == Items.CAKE || stack.getUseAnimation() == net.minecraft.world.item.UseAnim.DRINK) {
                ValheimFoodData foodData = player.getData(SOLValheim.FOOD_DATA);
                if (stack.is(Items.ROTTEN_FLESH)) {
                    foodData.clear();
                } else {
                    foodData.eatItem(stack.copyWithCount(1));
                    player.removeEffect(MobEffects.HUNGER);
                    player.removeEffect(Onetoys.HUNGER_2);
                }
                if (!player.level().isClientSide) {
                    if (Salting.isSalted(stack)) {
                        if (player instanceof ServerPlayer serverPlayer && !serverPlayer.isCreative()) {
                            SaltCriteriaTriggers.SALTED_FOOD_CONSUMED.get().trigger(serverPlayer);
                        }
                    }
                    player.syncData(SOLValheim.FOOD_DATA);
                }
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent event) {
        if (!FoodOverhaulModule.isFoodOverhaulEnabled()) return;
        Player player = event.getEntity();
        ValheimFoodData foodData = player.getData(SOLValheim.FOOD_DATA);
        if (foodData != null) {
            foodData.lastDamageGameTime = -1L;
        }
        if (!player.isCreative() && PerestroykaHungerAPI.isImmediateHungerActive() && !PerestroykaHungerAPI.hasActiveFoods(player)) {
            PerestroykaHungerAPI.setHungerToLowest(player);
        }
        player.syncData(SOLValheim.FOOD_DATA);
    }

    @SubscribeEvent
    public static void onPlayerRespawn(net.neoforged.neoforge.event.entity.player.PlayerEvent.Clone event) {
        if (!FoodOverhaulModule.isFoodOverhaulEnabled()) return;
        Player player = event.getEntity();
        ValheimFoodData foodData = player.getData(SOLValheim.FOOD_DATA);
        if (event.isWasDeath()) {
            if (PerestroykaHungerAPI.isImmediateHungerActive()) {
                foodData.hungerCountdown = 0;
            } else {
                int graceSeconds = (SOLValheim.Config.common != null && SOLValheim.Config.common.respawnGracePeriod != null)
                        ? SOLValheim.Config.common.respawnGracePeriod.get() : 300;
                int graceTicks = Math.max(200, graceSeconds * 20);
                foodData.hungerCountdown = Math.max(foodData.hungerCountdown, graceTicks);
            }
            foodData.secondHungerCountdown = foodData.getMaxSecondHungerTimer();
        } else {
            Player original = event.getOriginal();
            var originalMaxHealth = original.getAttribute(Attributes.MAX_HEALTH);
            var newMaxHealth = player.getAttribute(Attributes.MAX_HEALTH);
            if (originalMaxHealth != null && newMaxHealth != null) {
                newMaxHealth.setBaseValue(originalMaxHealth.getBaseValue());
            }
            player.setHealth(original.getHealth());
        }
        foodData.lastDamageGameTime = -1L;
        player.syncData(SOLValheim.FOOD_DATA);
    }

    @SubscribeEvent
    public static void onDamage(LivingDamageEvent.Pre event) {
        if (!FoodOverhaulModule.isFoodOverhaulEnabled()) return;
        if (event.getEntity() instanceof Player && event.getSource().is(DamageTypes.STARVE)) {
            event.setNewDamage(0f);
        }
    }

    @SubscribeEvent
    public static void onLivingDamagePost(LivingDamageEvent.Post event) {
        if (!FoodOverhaulModule.isFoodOverhaulEnabled()) return;
        if (event.getEntity() instanceof Player player && !player.level().isClientSide && !player.isCreative()) {
            if (event.getNewDamage() > 0f) {
                ValheimFoodData foodData = player.getData(SOLValheim.FOOD_DATA);
                if (foodData != null) {
                    foodData.lastDamageGameTime = player.level().getGameTime();
                    if (!foodData.hasActiveFoods()) {
                        foodData.addExhaustion(player, 0.05F); // Taking damage drains ~10 hunger points
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public static void onWakeUp(SleepFinishedTimeEvent event) {
        if (!FoodOverhaulModule.isFoodOverhaulEnabled()) return;
        for (var player : event.getLevel().players()) {
            if (player.isCreative()) continue;
            boolean slowDigestion = player.hasEffect(Onetoys.SLOW_DIGESTION);
            player.getData(SOLValheim.FOOD_DATA).onWakeUp(slowDigestion);
            player.syncData(SOLValheim.FOOD_DATA);
        }
    }
}
