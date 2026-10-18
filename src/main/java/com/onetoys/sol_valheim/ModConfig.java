package com.onetoys.sol_valheim;

import com.onetoys.salt.Salting;
import com.onetoys.salt.Sweetening;
import com.onetoys.salt.Satiating;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.UseAnim;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ModConfig {

    public static final int MAX_SLOTS = 3;

    public static final Common COMMON = new Common();
    public static final Client CLIENT = new Client();
    public static ModConfigSpec COMMON_SPEC;
    public static ModConfigSpec CLIENT_SPEC;

    public record FoodCacheKey(Item item, byte seasoningFlags) {}

    private static final Map<FoodCacheKey, Common.FoodConfig> ITEM_CONFIG_CACHE = new ConcurrentHashMap<>();

    public static boolean isLoaded() {
        return com.onetoys.Config.SPEC != null && com.onetoys.Config.SPEC.isLoaded();
    }

    public static void buildConfig(ModConfigSpec.Builder builder) {
        COMMON.buildConfig(builder);
        CLIENT.buildConfig(builder);
    }

    public static void clearCache() {
        ITEM_CONFIG_CACHE.clear();
        COMMON.foodConfigs.clear();
    }

    private static Common.FoodConfig applyFoodModifiers(ItemStack stack, Common.FoodConfig base) {
        if (base == null) {
            return null;
        }
        boolean isSalted = Salting.isSalted(stack);
        boolean isSweetened = Sweetening.isSweetened(stack);
        boolean isSatiating = Satiating.isSatiating(stack);
        if (!isSalted && !isSweetened && !isSatiating) {
            return base;
        }

        var modified = new Common.FoodConfig();
        modified.nutrition = base.nutrition;
        modified.saturationModifier = base.saturationModifier;
        modified.healthRegenModifier = base.healthRegenModifier;
        modified.extraHearts = base.extraHearts;
        modified.time = base.time;
        modified.extraEffects.addAll(base.extraEffects);

        if (isSalted) {
            modified.extraHearts += Salting.getBonusHearts(stack);
        }

        if (isSweetened) {
            float baseRegen = base.getHealAmountPerSecond();
            float bonus = Sweetening.getHealthRegenBonus(stack);
            float sweetenedRegen = baseRegen * (1.0f + bonus);
            // Round to one decimal place (0.1 increments, e.g. Steak 0.4 -> 0.5, Bread 0.3 -> 0.4)
            float baseRounded = Math.round(baseRegen * 10.0f) / 10.0f;
            float roundedRegen = Math.round(sweetenedRegen * 10.0f) / 10.0f;
            if (bonus > 0f && roundedRegen <= baseRounded) {
                roundedRegen = baseRounded + 0.1f;
            }
            modified.forcedHealthRegen = Math.max(0.1f, roundedRegen);
        }

        if (isSatiating) {
            int baseSec = base.getTime() / 20;
            modified.time = Math.round(baseSec * Satiating.getDurationMultiplier(stack));
        }

        return modified;
    }

    public static Common.FoodConfig getFoodConfig(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return null;
        var item = stack.getItem();

        byte seasoningFlags = (byte) ((Salting.isSalted(stack) ? 1 : 0) | (Sweetening.isSweetened(stack) ? 2 : 0) | (Satiating.isSatiating(stack) ? 4 : 0));
        FoodCacheKey cacheKey = new FoodCacheKey(item, seasoningFlags);

        Common.FoodConfig cached = ITEM_CONFIG_CACHE.get(cacheKey);
        if (cached != null) {
            return cached;
        }

        var isDrink = stack.getUseAnimation() == UseAnim.DRINK;
        if (item != Items.CAKE && !stack.has(net.minecraft.core.component.DataComponents.FOOD) && !isDrink)
            return null;

        var resourceLocation = BuiltInRegistries.ITEM.getKey(item);
        var registry = resourceLocation.toString();
        var existing = SOLValheim.Config.common.foodConfigs.get(registry);

        if (existing == null) {
            FoodProperties food = stack.get(net.minecraft.core.component.DataComponents.FOOD);

            if (item == Items.CAKE) {
                existing = new Common.FoodConfig();
                existing.nutrition = 2;
                existing.saturationModifier = 0.1f;
                existing.healthRegenModifier = 1.0f;
                existing.time = 0;
                SOLValheim.Config.common.foodConfigs.put(registry, existing);
            } else {
                if (isDrink) {
                    if (registry.contains("potion")) {
                        food = new FoodProperties.Builder().nutrition(4).saturationModifier(0.75f).build();
                    } else if (registry.contains("milk")) {
                        food = new FoodProperties.Builder().nutrition(6).saturationModifier(1f).build();
                    } else {
                        food = new FoodProperties.Builder().nutrition(2).saturationModifier(0.5f).build();
                    }
                }

                if (food == null) {
                    food = new FoodProperties.Builder().nutrition(2).saturationModifier(0.5f).build();
                }

                existing = new Common.FoodConfig();
                existing.nutrition = food.nutrition();
                existing.healthRegenModifier = 1f;
                existing.saturationModifier = food.nutrition() > 0 ? (food.saturation() / (food.nutrition() * 2.0f)) : 0.5f;

                if (registry.startsWith("farmers")) {
                    existing.nutrition = (int) ((existing.nutrition * 1.25));
                    existing.saturationModifier = existing.saturationModifier * 1.10f;
                    existing.healthRegenModifier = 1.25f;
                }

                if (registry.equals("minecraft:golden_apple") || registry.equals("minecraft:enchanted_golden_apple")) {
                    existing.nutrition = 10;
                    existing.healthRegenModifier = 1.5f;
                }

                if (registry.equals("minecraft:beetroot_soup")) {
                    var effectConfig = new Common.MobEffectConfig();
                    effectConfig.ID = BuiltInRegistries.MOB_EFFECT.getKey(MobEffects.MOVEMENT_SPEED.value()).toString();
                    existing.extraEffects.add(effectConfig);
                }
                SOLValheim.Config.common.foodConfigs.put(registry, existing);
            }
        }

        Common.FoodConfig modified = applyFoodModifiers(stack, existing);
        if (modified != null) {
            ITEM_CONFIG_CACHE.put(cacheKey, modified);
        }
        return modified;
    }


    public static class Common {
        public ModConfigSpec.IntValue startingHealth;
        public ModConfigSpec.IntValue maxFoodHealth;
        public ModConfigSpec.IntValue defaultTimer;
        public ModConfigSpec.IntValue hungerTimerSeconds;
        public ModConfigSpec.IntValue secondHungerTimerSeconds;
        public ModConfigSpec.DoubleValue regenSpeedModifier;
        public ModConfigSpec.DoubleValue regenAmountModifier;
        public ModConfigSpec.DoubleValue speedBoost;
        public ModConfigSpec.IntValue regenDelay;
        public ModConfigSpec.IntValue respawnGracePeriod;
        public ModConfigSpec.DoubleValue eatAgainPercentage;
        public ModConfigSpec.DoubleValue drinkSlotFoodEffectivenessBonus;
        public ModConfigSpec.BooleanValue passTicksDuringNight;
        public ModConfigSpec.BooleanValue displayEffects;
        public ModConfigSpec.BooleanValue immediateHunger;

        public Map<String, FoodConfig> foodConfigs = new ConcurrentHashMap<>();

        public Common() {
        }

        public void buildConfig(ModConfigSpec.Builder builder) {
            builder.push("General");
            startingHealth = builder.comment("Number of base hearts the player starts with").defineInRange("startingHealth", 3, 1, 20);
            maxFoodHealth = builder.comment("Maximum allowed hearts from food").defineInRange("maxFoodHealth", 100, 1, 1000);
            defaultTimer = builder.comment("Base seconds per nutrition*saturation unit (default 70)").defineInRange("defaultTimer", 70, 10, 10000);
            hungerTimerSeconds = builder.comment("Duration of first hunger phase in seconds").defineInRange("hungerTimerSeconds", 600, 10, 36000);
            secondHungerTimerSeconds = builder.comment("Duration of second hunger phase in seconds").defineInRange("secondHungerTimerSeconds", 600, 10, 36000);
            regenSpeedModifier = builder.comment("Speed at which natural health regeneration occurs").defineInRange("regenSpeedModifier", 1.0, 0.0, 10.0);
            regenAmountModifier = builder.comment("Heal amount per second modifier").defineInRange("regenAmountModifier", 0.5, 0.05, 5.0);
            speedBoost = builder.comment("Extra movement speed multiplier given when hearts are full").defineInRange("speedBoost", 0.0, 0.0, 10.0);
            regenDelay = builder.comment("Ticks to wait after taking damage before health regenerates").defineInRange("regenDelay", 200, 0, 10000);
            respawnGracePeriod = builder.comment("Time in seconds after spawning before sprinting is disabled if hungry").defineInRange("respawnGracePeriod", 300, 0, 10000);
            eatAgainPercentage = builder.comment("Percentage of food duration remaining before eating the same food again").defineInRange("eatAgainPercentage", 0.20, 0.0, 1.0);
            drinkSlotFoodEffectivenessBonus = builder.comment("Bonus effectiveness given to other foods when drinking").defineInRange("drinkSlotFoodEffectivenessBonus", 0.10, 0.0, 1.0);
            passTicksDuringNight = builder.comment("Simulate food duration ticking down during sleeping").define("passTicksDuringNight", true);
            displayEffects = builder.comment("Whether food tooltips show extra potion effects").define("displayEffects", true);
            immediateHunger = builder.comment("Whether immediate hunger is active. When active, players without active food immediately have their hunger timer dropped to 0.")
                    .define("immediateHunger", false);
            builder.pop();
        }

        public static class MobEffectConfig {
            public String ID = "";
            public int amplifier = 0;
            public float duration = 1.0f;

            public net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect> getEffect() {
                var resourceLocation = ResourceLocation.parse(ID);
                return BuiltInRegistries.MOB_EFFECT.getHolder(resourceLocation).orElse(null);
            }
        }

        public static class FoodConfig {
            public int time;
            public int nutrition;
            public float saturationModifier;
            public float healthRegenModifier;
            public int extraHearts = 0;
            public float forcedHealthRegen = -1f;
            public List<MobEffectConfig> extraEffects = new ArrayList<>();

            public int getTime() {
                if (time > 0) return time * 20;
                int timerFactor = (SOLValheim.Config.common != null && SOLValheim.Config.common.defaultTimer != null && isLoaded())
                        ? SOLValheim.Config.common.defaultTimer.get() : 70;
                int baseSeconds = (int) (nutrition * timerFactor * saturationModifier);
                int clampedSeconds = Mth.clamp(baseSeconds, 120, 1200);
                return clampedSeconds * 20;
            }

            public float getHealAmountPerSecond() {
                if (forcedHealthRegen >= 0f) {
                    return forcedHealthRegen;
                }
                return (saturationModifier * healthRegenModifier) * (SOLValheim.Config.common != null && SOLValheim.Config.common.regenAmountModifier != null && isLoaded() ? SOLValheim.Config.common.regenAmountModifier.get().floatValue() : 0.5f);
            }

            public float getHeartsBonus() {
                return (nutrition * 0.75f) + (extraHearts * 2.0f);
            }

            public int getHearts() {
                return (int) (nutrition * 0.75f) + extraHearts;
            }

            public float getHealthRegen() {
                return getHealAmountPerSecond();
            }
        }
    }

    public static class Client {
        public ModConfigSpec.BooleanValue useLargeIcons;

        public Client() {
        }

        public void buildConfig(ModConfigSpec.Builder builder) {
            builder.push("HUD");
            useLargeIcons = builder.comment("Use large food icons").define("useLargeIcons", true);
            builder.pop();
        }
    }
}
