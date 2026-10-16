package com.onetoys.api;

import com.onetoys.Onetoys;
import com.onetoys.api.event.PerestroykaHungerEvent;
import com.onetoys.sol_valheim.ModConfig;
import com.onetoys.sol_valheim.SOLValheim;
import com.onetoys.sol_valheim.ValheimFoodData;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.common.NeoForge;
import org.jetbrains.annotations.Nullable;

/**
 * Public API for other mods to query and control Onetoys's hunger bar,
 * hunger countdown timers, starvation timers, exhaustion, active foods,
 * and the "immediate hunger" mode.
 */
public final class PerestroykaHungerAPI {

    private static volatile boolean fallbackImmediateHunger = false;

    private PerestroykaHungerAPI() {}

    /**
     * Gets the {@link ValheimFoodData} capability attachment for a player, or null if player is null.
     */
    @Nullable
    public static ValheimFoodData getFoodData(@Nullable Player player) {
        if (player == null) return null;
        return player.getData(SOLValheim.FOOD_DATA);
    }

    /**
     * Returns true if "immediate hunger" is currently active.
     * When active, players without active foods have their hunger timer automatically
     * dropped to 0 immediately.
     */
    public static boolean isImmediateHungerActive() {
        if (ModConfig.isLoaded()) {
            try {
                if (SOLValheim.Config.common != null && SOLValheim.Config.common.immediateHunger != null) {
                    return SOLValheim.Config.common.immediateHunger.get();
                }
            } catch (Exception ignored) {
            }
        }
        return fallbackImmediateHunger;
    }

    /**
     * Controls whether "immediate hunger" is active or not.
     * Immediate hunger works by these rules:
     * Automatically drops the player's hunger timer to its lowest value (0) immediately
     * if they haven't eaten any food. Returns true if modified, false if active food is present.
     *
     * @param active whether immediate hunger should be active
     */
    public static void setImmediateHungerActive(boolean active) {
        setImmediateHungerActive(active, null);
    }

    /**
     * Controls whether "immediate hunger" is active or not, saving the state to server config
     * and optionally enforcing it immediately on all currently connected players.
     *
     * @param active whether immediate hunger should be active
     * @param server optional server to apply the change immediately to online players
     */
    public static void setImmediateHungerActive(boolean active, @Nullable MinecraftServer server) {
        fallbackImmediateHunger = active;
        if (ModConfig.isLoaded()) {
            try {
                if (SOLValheim.Config.common != null && SOLValheim.Config.common.immediateHunger != null) {
                    SOLValheim.Config.common.immediateHunger.set(active);
                    com.onetoys.Config.SPEC.save();
                }
            } catch (Exception ignored) {
            }
        }
        if (active && server != null) {
            for (Player player : server.getPlayerList().getPlayers()) {
                if (!player.isCreative() && !hasActiveFoods(player)) {
                    setHungerToLowest(player);
                }
            }
        }
    }

    /**
     * Returns true if the player has any active solid foods in their stomach or an active beverage.
     * When a player has active foods, their hunger bar is replaced by food duration bars,
     * and their hunger countdown is locked at maximum.
     */
    public static boolean hasActiveFoods(@Nullable Player player) {
        ValheimFoodData data = getFoodData(player);
        return data != null && data.hasActiveFoods();
    }

    /**
     * Returns true if the player has eaten any food (synonym for {@link #hasActiveFoods(Player)}).
     */
    public static boolean hasEatenFood(@Nullable Player player) {
        return hasActiveFoods(player);
    }

    /**
     * Returns the current Phase 1 hunger timer in ticks (between 0 and {@link #getMaxHungerTimer(Player)}).
     * When this reaches 0, Phase 1 hunger is depleted and the player enters Phase 2 (starvation countdown).
     */
    public static int getHungerTimer(@Nullable Player player) {
        ValheimFoodData data = getFoodData(player);
        return data != null ? data.hungerCountdown : 0;
    }

    /**
     * Returns the maximum Phase 1 hunger timer in ticks (default 12,000 ticks = 10 minutes).
     */
    public static int getMaxHungerTimer(@Nullable Player player) {
        ValheimFoodData data = getFoodData(player);
        return data != null ? data.getMaxHungerTimer() : 12000;
    }

    /**
     * Returns the Phase 1 hunger timer as a fraction from 0.0 (fully depleted) to 1.0 (full).
     */
    public static float getHungerFraction(@Nullable Player player) {
        int max = getMaxHungerTimer(player);
        if (max <= 0) return 0f;
        return Mth.clamp((float) getHungerTimer(player) / (float) max, 0f, 1f);
    }

    /**
     * Returns the current Phase 2 starvation timer in ticks (between 0 and {@link #getMaxStarvationTimer(Player)}).
     * When Phase 1 is depleted and this timer runs out, the player receives the harmful Hunger 2 status effect.
     */
    public static int getStarvationTimer(@Nullable Player player) {
        ValheimFoodData data = getFoodData(player);
        return data != null ? data.secondHungerCountdown : 0;
    }

    /**
     * Returns the maximum Phase 2 starvation timer in ticks (default 12,000 ticks = 10 minutes).
     */
    public static int getMaxStarvationTimer(@Nullable Player player) {
        ValheimFoodData data = getFoodData(player);
        return data != null ? data.getMaxSecondHungerTimer() : 12000;
    }

    /**
     * Returns the Phase 2 starvation timer as a fraction from 0.0 (fully depleted) to 1.0 (full).
     */
    public static float getStarvationFraction(@Nullable Player player) {
        int max = getMaxStarvationTimer(player);
        if (max <= 0) return 0f;
        return Mth.clamp((float) getStarvationTimer(player) / (float) max, 0f, 1f);
    }

    /**
     * Returns true if Phase 1 hunger has fully depleted (i.e. hunger timer == 0 and no active foods).
     */
    public static boolean isHungerDepleted(@Nullable Player player) {
        ValheimFoodData data = getFoodData(player);
        return data != null && data.isFirstHungerDepleted();
    }

    /**
     * Returns true if Phase 2 is actively counting down (player is starving, first timer depleted).
     */
    public static boolean isStarving(@Nullable Player player) {
        ValheimFoodData data = getFoodData(player);
        return data != null && data.isSecondTimerActive();
    }

    /**
     * Returns true if both Phase 1 and Phase 2 timers have completely depleted (player has Hunger 2).
     */
    public static boolean isFullyStarving(@Nullable Player player) {
        ValheimFoodData data = getFoodData(player);
        return data != null && data.isSecondTimerDepleted();
    }

    /**
     * Sets the player's primary hunger timer to the specified value in ticks.
     * The value is clamped between 0 and {@link #getMaxHungerTimer(Player)}.
     * <p>
     * If {@code forceIfFoodsActive} is false, this will only take effect if the player has no active foods in stomach.
     * If {@code forceIfFoodsActive} is true, any active foods in stomach will be cleared first so the new timer isn't overwritten.
     *
     * @param player the player
     * @param ticks the target ticks (0 = lowest / empty hunger)
     * @param forceIfFoodsActive whether to clear stomach foods if active
     * @return true if the timer was successfully modified, false otherwise
     */
    public static boolean setHungerTimer(@Nullable Player player, int ticks, boolean forceIfFoodsActive) {
        if (player == null || player.isCreative()) return false;
        ValheimFoodData data = getFoodData(player);
        if (data == null) return false;

        if (data.hasActiveFoods()) {
            if (!forceIfFoodsActive) {
                return false;
            }
            data.clear();
        }

        int max = data.getMaxHungerTimer();
        int target = Mth.clamp(ticks, 0, max);

        PerestroykaHungerEvent.TimerChange event = new PerestroykaHungerEvent.TimerChange(
                player, PerestroykaHungerEvent.TimerType.HUNGER_TIMER, data.hungerCountdown, target
        );
        if (NeoForge.EVENT_BUS.post(event).isCanceled()) {
            return false;
        }

        data.hungerCountdown = Mth.clamp(event.getNewTimer(), 0, max);
        data.markDirty();

        syncAndApplyStatus(player, data);
        return true;
    }

    /**
     * Sets the player's primary hunger timer to the specified value in ticks.
     * Only takes effect if the player hasn't eaten any active food.
     *
     * @param player the player
     * @param ticks the target ticks (0 = lowest / empty hunger)
     * @return true if the timer was successfully modified, false if the player has active food or is creative
     */
    public static boolean setHungerTimer(@Nullable Player player, int ticks) {
        return setHungerTimer(player, ticks, false);
    }

    /**
     * Automatically drops the player's hunger timer to its lowest value (0) immediately
     * if they haven't eaten any food. Returns true if modified, false if active food is present.
     *
     * @param player the player
     * @return true if modified, false if active food is present
     */
    public static boolean setHungerToLowest(@Nullable Player player) {
        return setHungerTimer(player, 0, false);
    }

    /**
     * Immediately drops the player's hunger timer to its lowest value (0),
     * forcibly clearing any active foods if {@code force} is true.
     *
     * @param player the player
     * @param force whether to clear active foods if present
     * @return true if hunger was set to 0, false otherwise
     */
    public static boolean setHungerToLowest(@Nullable Player player, boolean force) {
        return setHungerTimer(player, 0, force);
    }

    /**
     * Sets the player's Phase 2 starvation timer in ticks.
     * Clamped between 0 and {@link #getMaxStarvationTimer(Player)}.
     *
     * @param player the player
     * @param ticks target ticks (0 = completely starved, triggers Hunger 2)
     * @return true if modified, false otherwise
     */
    public static boolean setStarvationTimer(@Nullable Player player, int ticks) {
        if (player == null || player.isCreative()) return false;
        ValheimFoodData data = getFoodData(player);
        if (data == null) return false;

        int max = data.getMaxSecondHungerTimer();
        int target = Mth.clamp(ticks, 0, max);

        PerestroykaHungerEvent.TimerChange event = new PerestroykaHungerEvent.TimerChange(
                player, PerestroykaHungerEvent.TimerType.STARVATION_TIMER, data.secondHungerCountdown, target
        );
        if (NeoForge.EVENT_BUS.post(event).isCanceled()) {
            return false;
        }

        data.secondHungerCountdown = Mth.clamp(event.getNewTimer(), 0, max);
        data.markDirty();

        syncAndApplyStatus(player, data);
        return true;
    }

    /**
     * Drops both hunger and starvation timers to 0 immediately (full starvation with Hunger 2),
     * provided the player has no active foods (or if forced).
     */
    public static boolean setFullyStarving(@Nullable Player player, boolean forceIfFoodsActive) {
        if (player == null || player.isCreative()) return false;
        ValheimFoodData data = getFoodData(player);
        if (data == null) return false;

        if (data.hasActiveFoods()) {
            if (!forceIfFoodsActive) return false;
            data.clear();
        }

        data.setHungerCountdown(player, 0);
        data.setSecondHungerCountdown(player, 0);
        data.hungerExhaustion = 0f;
        data.markDirty();

        syncAndApplyStatus(player, data);
        return true;
    }

    /**
     * Drops both hunger and starvation timers to 0 immediately (full starvation with Hunger 2)
     * if the player has no active foods.
     */
    public static boolean setFullyStarving(@Nullable Player player) {
        return setFullyStarving(player, false);
    }

    /**
     * Adds or subtracts ticks from the player's primary hunger timer.
     */
    public static boolean modifyHungerTimer(@Nullable Player player, int deltaTicks) {
        if (player == null || player.isCreative() || hasActiveFoods(player)) return false;
        return setHungerTimer(player, getHungerTimer(player) + deltaTicks, false);
    }

    /**
     * Adds or subtracts ticks from the player's Phase 2 starvation timer.
     */
    public static boolean modifyStarvationTimer(@Nullable Player player, int deltaTicks) {
        if (player == null || player.isCreative()) return false;
        return setStarvationTimer(player, getStarvationTimer(player) + deltaTicks);
    }

    /**
     * Adds exhaustion to the player's hunger system (same formula as walking/sprinting/mining).
     */
    public static void addHungerExhaustion(@Nullable Player player, float exhaustion) {
        if (player == null || player.isCreative()) return;
        ValheimFoodData data = getFoodData(player);
        if (data != null) {
            data.addExhaustion(player, exhaustion);
            if (!player.level().isClientSide()) {
                player.syncData(SOLValheim.FOOD_DATA);
            }
        }
    }

    /**
     * Depletes a specific number of hunger points directly.
     */
    public static void drainHungerPoints(@Nullable Player player, int points) {
        if (player == null || player.isCreative()) return;
        ValheimFoodData data = getFoodData(player);
        if (data != null) {
            data.drainHungerPoints(player, points);
            if (!player.level().isClientSide()) {
                player.syncData(SOLValheim.FOOD_DATA);
            }
        }
    }

    /**
     * Resets both hunger and starvation timers back to their maximum full values,
     * clears exhaustion, and cleanses hunger/starvation debuffs.
     */
    public static void resetHungerTimers(@Nullable Player player) {
        if (player == null) return;
        ValheimFoodData data = getFoodData(player);
        if (data == null) return;

        data.resetHungerTimers();
        syncAndApplyStatus(player, data);
    }

    /**
     * Clears all active solid foods and the active drink slot from the player's stomach.
     */
    public static void clearEatenFoods(@Nullable Player player) {
        if (player == null) return;
        ValheimFoodData data = getFoodData(player);
        if (data != null) {
            data.clear();
            if (!player.level().isClientSide()) {
                player.syncData(SOLValheim.FOOD_DATA);
            }
        }
    }

    private static void syncAndApplyStatus(Player player, ValheimFoodData foodData) {
        if (!player.level().isClientSide()) {
            player.syncData(SOLValheim.FOOD_DATA);

            // Immediately update status effects
            if (foodData.isFirstHungerDepleted()) {
                if (foodData.isSecondTimerDepleted()) {
                    player.removeEffect(MobEffects.HUNGER);
                    var hunger2 = player.getEffect(Onetoys.HUNGER_2);
                    if (hunger2 == null || hunger2.getDuration() <= 40) {
                        player.addEffect(new MobEffectInstance(Onetoys.HUNGER_2, 100, 0, false, true, true));
                    }
                } else {
                    player.removeEffect(Onetoys.HUNGER_2);
                    var hunger = player.getEffect(MobEffects.HUNGER);
                    if (hunger == null || hunger.getDuration() <= 40) {
                        player.addEffect(new MobEffectInstance(MobEffects.HUNGER, 100, 0, false, true, true));
                    }
                }
            } else {
                if (player.hasEffect(Onetoys.HUNGER_2)) {
                    player.removeEffect(Onetoys.HUNGER_2);
                }
                if (player.hasEffect(MobEffects.HUNGER)) {
                    player.removeEffect(MobEffects.HUNGER);
                }
            }
        }
    }
}
