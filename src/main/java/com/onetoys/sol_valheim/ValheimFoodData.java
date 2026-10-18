package com.onetoys.sol_valheim;

import com.onetoys.api.event.PerestroykaHungerEvent;
import com.onetoys.salt.Salting;
import com.onetoys.salt.Satiating;
import com.onetoys.salt.Sweetening;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.INBTSerializable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class ValheimFoodData implements INBTSerializable<CompoundTag> {

    public static final int MAX_ITEM_SLOTS = 3;

    private static final Comparator<EatenFoodItem> TICKS_COMPARATOR = Comparator.comparingInt(a -> a.ticksLeft);

    public static final StreamCodec<RegistryFriendlyByteBuf, ValheimFoodData> STREAM_CODEC =
            StreamCodec.of(
                    (buffer, val) -> {
                        ByteBufCodecs.VAR_INT.encode(buffer, val.hungerCountdown);
                        ByteBufCodecs.VAR_INT.encode(buffer, val.secondHungerCountdown);
                        ByteBufCodecs.FLOAT.encode(buffer, val.hungerExhaustion);

                        buffer.writeVarInt(val.ItemEntries.size());
                        for (EatenFoodItem entry : val.ItemEntries) {
                            ItemStack.OPTIONAL_STREAM_CODEC.encode(buffer, entry.item);
                            buffer.writeVarInt(entry.ticksLeft);
                            buffer.writeVarLong(entry.id);
                        }

                        if (val.DrinkSlot != null) {
                            buffer.writeBoolean(true);
                            ItemStack.OPTIONAL_STREAM_CODEC.encode(buffer, val.DrinkSlot.item);
                            buffer.writeVarInt(val.DrinkSlot.ticksLeft);
                            buffer.writeVarLong(val.DrinkSlot.id);
                        } else {
                            buffer.writeBoolean(false);
                        }
                    },
                    buffer -> {
                        ValheimFoodData data = new ValheimFoodData();
                        data.hungerCountdown = ByteBufCodecs.VAR_INT.decode(buffer);
                        data.secondHungerCountdown = ByteBufCodecs.VAR_INT.decode(buffer);
                        data.hungerExhaustion = ByteBufCodecs.FLOAT.decode(buffer);

                        int count = buffer.readVarInt();
                        data.ItemEntries.clear();
                        for (int i = 0; i < count; i++) {
                            ItemStack stack = ItemStack.OPTIONAL_STREAM_CODEC.decode(buffer);
                            int ticksLeft = buffer.readVarInt();
                            long id = buffer.readVarLong();
                            data.ItemEntries.add(new EatenFoodItem(stack, ticksLeft, id));
                        }
                        data.ItemEntries.sort(TICKS_COMPARATOR);

                        boolean hasDrink = buffer.readBoolean();
                        if (hasDrink) {
                            ItemStack drinkStack = ItemStack.OPTIONAL_STREAM_CODEC.decode(buffer);
                            int drinkTicks = buffer.readVarInt();
                            long drinkId = buffer.readVarLong();
                            data.DrinkSlot = new EatenFoodItem(drinkStack, drinkTicks, drinkId);
                        } else {
                            data.DrinkSlot = null;
                        }

                        return data;
                    }
            );

    private static long nextIdCounter = 1L;
    public static synchronized long getNextId() {
        return nextIdCounter++;
    }

    public static boolean hasAnySeasoning(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        return Salting.isSalted(stack) || Sweetening.isSweetened(stack) || Satiating.isSatiating(stack);
    }

    public static boolean canReplaceWithSeasoned(ItemStack existingFood, ItemStack newFood) {
        if (existingFood == null || existingFood.isEmpty() || newFood == null || newFood.isEmpty()) {
            return false;
        }
        if (!ItemStack.isSameItem(existingFood, newFood)) {
            return false;
        }
        return !hasAnySeasoning(existingFood) && hasAnySeasoning(newFood);
    }

    public List<EatenFoodItem> ItemEntries = new ArrayList<>();
    public EatenFoodItem DrinkSlot;
    public int MaxItemSlots = MAX_ITEM_SLOTS;
    public int hungerCountdown;
    public int secondHungerCountdown;
    public float hungerExhaustion = 0f;
    public transient long lastDamageGameTime = -1L;
    private boolean isDirty = false;

    private transient double lastPosX = Double.NaN;
    private transient double lastPosY = Double.NaN;
    private transient double lastPosZ = Double.NaN;
    private transient ResourceKey<Level> lastDimension = null;

    public ValheimFoodData() {
        this.MaxItemSlots = MAX_ITEM_SLOTS;
        this.hungerCountdown = getMaxHungerTimer();
        this.secondHungerCountdown = getMaxSecondHungerTimer();
    }

    public boolean isDirty() {
        return isDirty;
    }

    public void clearDirty() {
        isDirty = false;
    }

    public void markDirty() {
        this.isDirty = true;
    }

    public int getHungerCountdown() {
        return hungerCountdown;
    }

    public void setHungerCountdown(int ticks) {
        setHungerCountdown(null, ticks);
    }

    public void setHungerCountdown(Player player, int ticks) {
        int max = getMaxHungerTimer();
        int clamped = Mth.clamp(ticks, 0, max);
        if (player != null) {
            PerestroykaHungerEvent.TimerChange event = new PerestroykaHungerEvent.TimerChange(
                    player, PerestroykaHungerEvent.TimerType.HUNGER_TIMER, this.hungerCountdown, clamped
            );
            if (NeoForge.EVENT_BUS.post(event).isCanceled()) {
                return;
            }
            clamped = Mth.clamp(event.getNewTimer(), 0, max);
        }
        if (this.hungerCountdown != clamped) {
            this.hungerCountdown = clamped;
            this.isDirty = true;
        }
    }

    public int getSecondHungerCountdown() {
        return secondHungerCountdown;
    }

    public void setSecondHungerCountdown(int ticks) {
        setSecondHungerCountdown(null, ticks);
    }

    public void setSecondHungerCountdown(Player player, int ticks) {
        int max = getMaxSecondHungerTimer();
        int clamped = Mth.clamp(ticks, 0, max);
        if (player != null) {
            PerestroykaHungerEvent.TimerChange event = new PerestroykaHungerEvent.TimerChange(
                    player, PerestroykaHungerEvent.TimerType.STARVATION_TIMER, this.secondHungerCountdown, clamped
            );
            if (NeoForge.EVENT_BUS.post(event).isCanceled()) {
                return;
            }
            clamped = Mth.clamp(event.getNewTimer(), 0, max);
        }
        if (this.secondHungerCountdown != clamped) {
            this.secondHungerCountdown = clamped;
            this.isDirty = true;
        }
    }

    public void resetHungerTimers() {
        this.hungerCountdown = getMaxHungerTimer();
        this.secondHungerCountdown = getMaxSecondHungerTimer();
        this.hungerExhaustion = 0f;
        this.isDirty = true;
    }

    public int getMaxHungerTimer() {
        if (SOLValheim.Config.common != null && SOLValheim.Config.common.hungerTimerSeconds != null && ModConfig.isLoaded()) {
            return Math.max(200, SOLValheim.Config.common.hungerTimerSeconds.get() * 20);
        }
        return 12000;
    }

    public int getMaxSecondHungerTimer() {
        if (SOLValheim.Config.common != null && SOLValheim.Config.common.secondHungerTimerSeconds != null && ModConfig.isLoaded()) {
            return Math.max(200, SOLValheim.Config.common.secondHungerTimerSeconds.get() * 20);
        }
        return 12000;
    }

    public boolean hasActiveFoods() {
        return !ItemEntries.isEmpty() || DrinkSlot != null;
    }

    public boolean isFirstHungerDepleted() {
        return !hasActiveFoods() && hungerCountdown <= 0;
    }

    public boolean isSecondTimerActive() {
        return isFirstHungerDepleted() && secondHungerCountdown > 0;
    }

    public boolean isSecondTimerDepleted() {
        return isFirstHungerDepleted() && secondHungerCountdown <= 0;
    }

    public boolean isStarving() {
        return isFirstHungerDepleted();
    }

    /**
     * Adds food exhaustion which depletes hunger/starvation points.
     * Standing still (idle) causes 0 exhaustion, so points are preserved while idle.
     */
    public void addExhaustion(float exhaustion) {
        addExhaustion(null, exhaustion);
    }

    public void addExhaustion(Player player, float exhaustion) {
        if (hasActiveFoods() || exhaustion <= 0.0f || (hungerCountdown <= 0 && secondHungerCountdown <= 0)) return;
        if (player != null) {
            PerestroykaHungerEvent.Exhaustion event = new PerestroykaHungerEvent.Exhaustion(player, exhaustion);
            if (NeoForge.EVENT_BUS.post(event).isCanceled()) {
                return;
            }
            exhaustion = event.getExhaustion();
            if (exhaustion <= 0.0f) return;
        }
        this.hungerExhaustion += exhaustion;
        float pointsToDrain = this.hungerExhaustion * 200.0f;
        if (pointsToDrain >= 1.0f) {
            int points = (int) pointsToDrain;
            this.hungerExhaustion -= (float) points / 200.0f;
            drainHungerPoints(player, points);
        }
    }

    public void drainHungerPoints(int points) {
        drainHungerPoints(null, points);
    }

    public void drainHungerPoints(Player player, int points) {
        if (hasActiveFoods() || points <= 0) return;
        if (hungerCountdown > 0) {
            int target = Math.max(0, hungerCountdown - points);
            if (player != null) {
                PerestroykaHungerEvent.TimerChange event = new PerestroykaHungerEvent.TimerChange(
                        player, PerestroykaHungerEvent.TimerType.HUNGER_TIMER, hungerCountdown, target
                );
                if (NeoForge.EVENT_BUS.post(event).isCanceled()) {
                    return;
                }
                target = event.getNewTimer();
            }
            hungerCountdown = target;
            if (hungerCountdown <= 0) {
                hungerCountdown = 0;
                isDirty = true;
            }
        } else if (secondHungerCountdown > 0) {
            int target = Math.max(0, secondHungerCountdown - points);
            if (player != null) {
                PerestroykaHungerEvent.TimerChange event = new PerestroykaHungerEvent.TimerChange(
                        player, PerestroykaHungerEvent.TimerType.STARVATION_TIMER, secondHungerCountdown, target
                );
                if (NeoForge.EVENT_BUS.post(event).isCanceled()) {
                    return;
                }
                target = event.getNewTimer();
            }
            secondHungerCountdown = target;
            if (secondHungerCountdown <= 0) {
                secondHungerCountdown = 0;
                isDirty = true;
            }
        }
    }

    /**
     * Drains hunger points based on player activity (walking, sprinting, swimming, mining, jumping).
     * If the player is standing still / idle, 0 points are consumed.
     */
    public void tickPlayerActions(Player player) {
        if (player == null || !player.isAlive() || player.isSpectator() || player.isCreative() || player.getAbilities().invulnerable) {
            if (player != null) {
                lastPosX = player.getX();
                lastPosY = player.getY();
                lastPosZ = player.getZ();
                lastDimension = player.level().dimension();
            }
            return;
        }

        if (Double.isNaN(lastPosX) || lastDimension != player.level().dimension()) {
            lastPosX = player.getX();
            lastPosY = player.getY();
            lastPosZ = player.getZ();
            lastDimension = player.level().dimension();
            return;
        }

        if (hasActiveFoods()) {
            lastPosX = player.getX();
            lastPosY = player.getY();
            lastPosZ = player.getZ();
            lastDimension = player.level().dimension();
            return;
        }

        double dx = player.getX() - lastPosX;
        double dy = player.getY() - lastPosY;
        double dz = player.getZ() - lastPosZ;

        lastPosX = player.getX();
        lastPosY = player.getY();
        lastPosZ = player.getZ();
        lastDimension = player.level().dimension();

        double horizontalDistSq = dx * dx + dz * dz;
        double dist3dSq = horizontalDistSq + dy * dy;

        // Fast-path: When standing completely still and not swinging, skip square roots and calculations
        if (horizontalDistSq < 0.0001 && dist3dSq < 0.0001 && !player.swinging) {
            return;
        }

        // Teleportation / chunk boundary / respawn protection (> 1.5 blocks horizontal or > 2.0 blocks 3D)
        if (horizontalDistSq > 2.25 || dist3dSq > 4.0) {
            return;
        }

        double horizontalDist = Math.sqrt(horizontalDistSq);
        double dist3d = Math.sqrt(dist3dSq);

        // 1. Swimming & Fluid movement
        if (player.isInWater() || player.isInLava() || player.isSwimming() || player.isUnderWater()) {
            if (dist3d > 0.01) {
                float swimRate = player.isSprinting() ? 0.05F : 0.03F;
                addExhaustion(player, (float) (swimRate * dist3d));
            }
        }
        // 2. Sprinting on ground
        else if (player.isSprinting() && player.onGround() && !player.isPassenger()) {
            if (horizontalDist > 0.01) {
                addExhaustion(player, (float) (0.08F * horizontalDist));
            }
        }
        // 3. Normal Walking on ground
        else if (player.onGround() && !player.isPassenger()) {
            if (horizontalDist > 0.01) {
                addExhaustion(player, (float) (0.025F * horizontalDist));
            }
        }
        // 4. In air (jumping / falling while moving horizontally)
        else if (!player.onGround() && !player.isPassenger()) {
            if (horizontalDist > 0.01) {
                float airRate = player.isSprinting() ? 0.06F : 0.02F;
                addExhaustion(player, (float) (airRate * horizontalDist));
            }
        }

        // 5. Active mining / attacking arm swing
        if (player.swinging) {
            addExhaustion(player, 0.01F);
        }
    }

    public ValheimFoodData copy() {
        ValheimFoodData copy = new ValheimFoodData();
        copy.MaxItemSlots = MAX_ITEM_SLOTS;
        copy.hungerCountdown = this.hungerCountdown;
        copy.secondHungerCountdown = this.secondHungerCountdown;
        copy.hungerExhaustion = this.hungerExhaustion;
        copy.lastDamageGameTime = this.lastDamageGameTime;
        for (EatenFoodItem entry : this.ItemEntries) {
            copy.ItemEntries.add(new EatenFoodItem(entry));
        }
        if (this.DrinkSlot != null) {
            copy.DrinkSlot = new EatenFoodItem(this.DrinkSlot);
        }
        return copy;
    }

    public void eatItem(ItemStack food) {
        if (food == null || food.isEmpty() || food.is(Items.ROTTEN_FLESH))
            return;

        var config = ModConfig.getFoodConfig(food);
        if (config == null)
            return;

        // If second hunger timer is active (or depleted):
        // Eating food only resets both of the hunger timers, without applying any other bonuses.
        if (hungerCountdown <= 0) {
            hungerCountdown = getMaxHungerTimer();
            secondHungerCountdown = getMaxSecondHungerTimer();
            hungerExhaustion = 0f;
            isDirty = true;
            return;
        }

        ItemStack singleFood = food.copyWithCount(1);
        var isDrink = singleFood.getUseAnimation() == UseAnim.DRINK;
        if (isDrink) {
            if (DrinkSlot != null && !DrinkSlot.canEatEarly() && !canReplaceWithSeasoned(DrinkSlot.item, singleFood))
                return;

            if (DrinkSlot == null)
                DrinkSlot = new EatenFoodItem(singleFood, config.getTime(), getNextId());
            else {
                DrinkSlot.ticksLeft = config.getTime();
                DrinkSlot.item = singleFood;
                DrinkSlot.id = getNextId();
            }
            hungerCountdown = getMaxHungerTimer();
            secondHungerCountdown = getMaxSecondHungerTimer();
            hungerExhaustion = 0f;
            isDirty = true;
            return;
        }

        var existing = getEatenFood(singleFood);
        if (existing != null) {
            if (!existing.canEatEarly() && !canReplaceWithSeasoned(existing.item, singleFood))
                return;

            existing.ticksLeft = config.getTime();
            existing.item = singleFood;
            existing.id = getNextId();
            ItemEntries.sort(TICKS_COMPARATOR);
            hungerCountdown = getMaxHungerTimer();
            secondHungerCountdown = getMaxSecondHungerTimer();
            hungerExhaustion = 0f;
            isDirty = true;
            return;
        }

        if (ItemEntries.size() < MAX_ITEM_SLOTS) {
            ItemEntries.add(new EatenFoodItem(singleFood, config.getTime(), getNextId()));
            ItemEntries.sort(TICKS_COMPARATOR);
            hungerCountdown = getMaxHungerTimer();
            secondHungerCountdown = getMaxSecondHungerTimer();
            hungerExhaustion = 0f;
            isDirty = true;
            return;
        }

        for (var item : ItemEntries) {
            if (item.canEatEarly()) {
                item.ticksLeft = config.getTime();
                item.item = singleFood;
                item.id = getNextId();
                ItemEntries.sort(TICKS_COMPARATOR);
                hungerCountdown = getMaxHungerTimer();
                secondHungerCountdown = getMaxSecondHungerTimer();
                hungerExhaustion = 0f;
                isDirty = true;
                return;
            }
        }
    }

    public boolean canEat(ItemStack food) {
        if (food == null || food.isEmpty() || food.is(Items.ROTTEN_FLESH))
            return true;

        if (hungerCountdown <= 0)
            return true;

        if (food.getUseAnimation() == UseAnim.DRINK)
            return DrinkSlot == null || DrinkSlot.canEatEarly() || canReplaceWithSeasoned(DrinkSlot.item, food);

        var existing = getEatenFood(food);
        if (existing != null)
            return existing.canEatEarly() || canReplaceWithSeasoned(existing.item, food);

        if (ItemEntries.size() < MAX_ITEM_SLOTS)
            return true;

        for (var entry : ItemEntries) {
            if (entry.canEatEarly()) return true;
        }
        return false;
    }

    public EatenFoodItem getEatenFood(ItemStack food) {
        for (var item : ItemEntries) {
            if (ItemStack.isSameItem(item.item, food)) {
                return item;
            }
        }
        return null;
    }

    public void clear() {
        ItemEntries.clear();
        DrinkSlot = null;
        isDirty = true;
    }

    /**
     * Standard concurrent digestion tick (matching original SOL Valheim):
     * All eaten food items tick down simultaneously.
     * Expired foods (ticksLeft <= 0) are removed.
     * Remaining foods are kept sorted by remaining duration.
     * If pauseFoodTimers is true (e.g. Slow Digestion effect active),
     * currently active food items' timers are stopped.
     */
    public void tick(boolean pauseFoodTimers) {
        if (!pauseFoodTimers) {
            for (var item : ItemEntries) {
                item.ticksLeft--;
            }

            if (DrinkSlot != null) {
                DrinkSlot.ticksLeft--;
                if (DrinkSlot.ticksLeft <= 0) {
                    DrinkSlot = null;
                    isDirty = true;
                }
            }
        }

        int prevSize = ItemEntries.size();
        ItemEntries.removeIf(item -> item.ticksLeft <= 0);
        if (ItemEntries.size() != prevSize) {
            isDirty = true;
        }
        ItemEntries.sort(TICKS_COMPARATOR);

        boolean active = !ItemEntries.isEmpty() || DrinkSlot != null;
        if (active) {
            hungerCountdown = getMaxHungerTimer();
            secondHungerCountdown = getMaxSecondHungerTimer();
            hungerExhaustion = 0f;
        }
    }

    public void tick() {
        tick(false);
    }

    public void onWakeUp(boolean pauseFoodTimers) {
        if (SOLValheim.Config.common != null && SOLValheim.Config.common.passTicksDuringNight != null && ModConfig.isLoaded() && !SOLValheim.Config.common.passTicksDuringNight.get()) {
            return;
        }
        if (!pauseFoodTimers) {
            int sleepTicks = 6000;
            for (var item : ItemEntries) {
                item.ticksLeft -= sleepTicks;
            }
            if (DrinkSlot != null) {
                DrinkSlot.ticksLeft -= sleepTicks;
                if (DrinkSlot.ticksLeft <= 0) {
                    DrinkSlot = null;
                    isDirty = true;
                }
            }
            int prevSize = ItemEntries.size();
            ItemEntries.removeIf(item -> item.ticksLeft <= 0);
            if (ItemEntries.size() != prevSize) {
                isDirty = true;
            }
            ItemEntries.sort(TICKS_COMPARATOR);
        }

        boolean active = !ItemEntries.isEmpty() || DrinkSlot != null;
        if (active) {
            hungerCountdown = getMaxHungerTimer();
            secondHungerCountdown = getMaxSecondHungerTimer();
            hungerExhaustion = 0f;
        }
    }

    public void onWakeUp() {
        onWakeUp(false);
    }

    public float getTotalFoodNutrition() {
        float nutrition = 0f;
        for (var item : ItemEntries) {
            ModConfig.Common.FoodConfig food = ModConfig.getFoodConfig(item.item);
            if (food == null)
                continue;

            nutrition += food.getHearts();
        }

        if (DrinkSlot != null) {
            ModConfig.Common.FoodConfig food = ModConfig.getFoodConfig(DrinkSlot.item);
            if (food != null) {
                nutrition += food.getHearts();
            }

            double bonus = (SOLValheim.Config.common != null && SOLValheim.Config.common.drinkSlotFoodEffectivenessBonus != null && ModConfig.isLoaded())
                    ? SOLValheim.Config.common.drinkSlotFoodEffectivenessBonus.get() : 0.10;
            nutrition = (float) (nutrition * (1.0 + bonus));
        }

        return nutrition;
    }

    public float getTotalMaxHealthBonus() {
        float maxHealthBonus = 0f;
        float drinkBonus = (DrinkSlot != null && SOLValheim.Config.common != null && SOLValheim.Config.common.drinkSlotFoodEffectivenessBonus != null && ModConfig.isLoaded())
                ? (float) (1.0 + SOLValheim.Config.common.drinkSlotFoodEffectivenessBonus.get()) : 1.0f;

        for (var eaten : ItemEntries) {
            var config = ModConfig.getFoodConfig(eaten.item);
            if (config != null) {
                maxHealthBonus += eaten.getHeartsBonus(config) * drinkBonus;
            }
        }

        if (DrinkSlot != null) {
            var config = ModConfig.getFoodConfig(DrinkSlot.item);
            if (config != null) {
                maxHealthBonus += DrinkSlot.getHeartsBonus(config);
            }
        }
        return maxHealthBonus;
    }

    public float getTotalHealPerSecond() {
        float healPerSecond = 0f;
        float drinkBonus = (DrinkSlot != null && SOLValheim.Config.common != null && SOLValheim.Config.common.drinkSlotFoodEffectivenessBonus != null && ModConfig.isLoaded())
                ? (float) (1.0 + SOLValheim.Config.common.drinkSlotFoodEffectivenessBonus.get()) : 1.0f;

        for (var eaten : ItemEntries) {
            var config = ModConfig.getFoodConfig(eaten.item);
            if (config != null) {
                healPerSecond += eaten.getHealAmount(config) * drinkBonus;
            }
        }

        if (DrinkSlot != null) {
            var config = ModConfig.getFoodConfig(DrinkSlot.item);
            if (config != null) {
                healPerSecond += DrinkSlot.getHealAmount(config);
            }
        }
        return healPerSecond;
    }

    public float getRegenSpeed() {
        return 0.25f + getTotalHealPerSecond();
    }

    @Override
    public CompoundTag serializeNBT(HolderLookup.Provider provider) {
        CompoundTag tag = new CompoundTag();
        int count = 0;
        tag.putInt("max_slots", MAX_ITEM_SLOTS);
        tag.putInt("hunger_timer", hungerCountdown);
        tag.putInt("second_hunger_timer", secondHungerCountdown);
        tag.putFloat("hunger_exhaustion", hungerExhaustion);
        tag.putInt("count", ItemEntries.size());
        for (var item : ItemEntries) {
            tag.putString("id" + count, net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(item.item.getItem()).toString());
            tag.putLong("uid" + count, item.id);
            Tag stackData = item.item.saveOptional(provider);
            if (stackData instanceof CompoundTag comp) {
                tag.put("data" + count, comp);
            }
            tag.putInt("ticks" + count, item.ticksLeft);
            count++;
        }

        if (DrinkSlot != null) {
            tag.putString("drink", net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(DrinkSlot.item.getItem()).toString());
            tag.putLong("drinkUid", DrinkSlot.id);
            Tag stackData = DrinkSlot.item.saveOptional(provider);
            if (stackData instanceof CompoundTag comp) {
                tag.put("drinkData", comp);
            }
            tag.putInt("drinkticks", DrinkSlot.ticksLeft);
        }

        return tag;
    }

    @Override
    public void deserializeNBT(HolderLookup.Provider provider, CompoundTag tag) {
        this.MaxItemSlots = MAX_ITEM_SLOTS;
        if (tag.contains("hunger_timer")) {
            hungerCountdown = tag.getInt("hunger_timer");
        } else {
            hungerCountdown = getMaxHungerTimer();
        }
        if (tag.contains("second_hunger_timer")) {
            secondHungerCountdown = tag.getInt("second_hunger_timer");
        } else {
            secondHungerCountdown = getMaxSecondHungerTimer();
        }
        if (tag.contains("hunger_exhaustion")) {
            hungerExhaustion = tag.getFloat("hunger_exhaustion");
        } else {
            hungerExhaustion = 0f;
        }
        ItemEntries.clear();
        DrinkSlot = null;

        var size = tag.getInt("count");
        for (int count = 0; count < size; count++) {
            var str = tag.getString("id" + count);
            var ticks = tag.getInt("ticks" + count);
            long id = tag.contains("uid" + count) ? tag.getLong("uid" + count) : getNextId();
            ItemStack stack = ItemStack.EMPTY;

            if (tag.contains("data" + count)) {
                var data = tag.getCompound("data" + count);
                stack = ItemStack.parseOptional(provider, data);
            } else if (!str.isBlank()) {
                ResourceLocation loc = ResourceLocation.tryParse(str);
                if (loc != null) {
                    var item = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(loc);
                    if (item != Items.AIR) {
                        stack = new ItemStack(item, 1);
                    }
                }
            }

            if (!stack.isEmpty() && ItemEntries.size() < MAX_ITEM_SLOTS) {
                ItemEntries.add(new EatenFoodItem(stack, ticks, id));
            }
        }
        ItemEntries.sort(TICKS_COMPARATOR);

        var drink = tag.getString("drink");
        var drinkTicks = tag.getInt("drinkticks");
        long drinkId = tag.contains("drinkUid") ? tag.getLong("drinkUid") : getNextId();

        if (tag.contains("drinkData")) {
            var data = tag.getCompound("drinkData");
            ItemStack stack = ItemStack.parseOptional(provider, data);
            if (!stack.isEmpty()) {
                DrinkSlot = new EatenFoodItem(stack, drinkTicks, drinkId);
            }
        } else if (!drink.isBlank()) {
            ResourceLocation loc = ResourceLocation.tryParse(drink);
            if (loc != null) {
                var item = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(loc);
                if (item != Items.AIR) {
                    DrinkSlot = new EatenFoodItem(new ItemStack(item, 1), drinkTicks, drinkId);
                }
            }
        }
    }

    public static class EatenFoodItem {
        public ItemStack item;
        public int ticksLeft;
        public long id;

        public boolean canEatEarly() {
            if (ticksLeft < 1200)
                return true;

            var config = ModConfig.getFoodConfig(item);
            if (config == null)
                return false;

            double pct = (SOLValheim.Config.common != null && SOLValheim.Config.common.eatAgainPercentage != null && ModConfig.isLoaded())
                    ? SOLValheim.Config.common.eatAgainPercentage.get() : 0.20;
            return ((float) this.ticksLeft / config.getTime()) < pct;
        }

        public float getHeartsBonus(ModConfig.Common.FoodConfig config) {
            return config != null ? config.getHeartsBonus() : 0f;
        }

        public float getHealAmount(ModConfig.Common.FoodConfig config) {
            return config != null ? config.getHealAmountPerSecond() : 0f;
        }

        public EatenFoodItem(ItemStack item, int ticksLeft) {
            this(item, ticksLeft, getNextId());
        }

        public EatenFoodItem(ItemStack item, int ticksLeft, long id) {
            this.item = item;
            this.ticksLeft = ticksLeft;
            this.id = id;
        }

        public EatenFoodItem(EatenFoodItem eaten) {
            this.item = eaten.item.copy();
            this.ticksLeft = eaten.ticksLeft;
            this.id = eaten.id;
        }
    }
}
