package com.onetoys.salt;

import com.onetoys.module.content.SaltModule;
import net.neoforged.neoforge.common.ModConfigSpec;

public class SaltConfiguration {
    // Salting:
    public static ModConfigSpec.IntValue SALTING_BONUS_HEARTS;

    // Sweetening:
    public static ModConfigSpec.DoubleValue SWEETENING_REGEN_BONUS;
    public static ModConfigSpec.DoubleValue SATIATING_DURATION_MULTIPLIER;

    // Dissolving
    public static ModConfigSpec.BooleanValue DISSOLVING_ENABLED;
    public static ModConfigSpec.DoubleValue DISSOLVING_CHANCE;
    public static ModConfigSpec.BooleanValue DISSOLVING_FLUID_SOURCE_CONVERSION;
    public static ModConfigSpec.BooleanValue DISSOLVING_IN_RAIN;
    public static ModConfigSpec.DoubleValue DISSOLVING_IN_RAIN_CHANCE;

    // Melting
    public static ModConfigSpec.BooleanValue MELTING_ITEM_ENABLED;
    public static ModConfigSpec.BooleanValue MELTING_BY_BLOCK_ENABLED;
    public static ModConfigSpec.DoubleValue MELTING_BLOCK_CHANCE;
    public static ModConfigSpec.BooleanValue MELTING_PLACES_WATER;

    // Evaporation
    public static ModConfigSpec.BooleanValue EVAPORATION_ENABLED;
    public static ModConfigSpec.DoubleValue EVAPORATION_CHANCE;

    // Cluster Growing
    public static ModConfigSpec.BooleanValue SALT_CLUSTER_GROWING_ENABLED;
    public static ModConfigSpec.DoubleValue SALT_CLUSTER_GROWING_CHANCE;

    // Rock Salt:
    public static ModConfigSpec.IntValue ROCK_SALT_SIZE;
    public static ModConfigSpec.DoubleValue ROCK_SALT_CLUSTER_CHANCE;

    // Villager Trades:
    public static ModConfigSpec.BooleanValue BUTCHER_SALT_TRADES_ENABLED;

    // CLIENT
    public static ModConfigSpec.BooleanValue SALTED_OVERLAY_ENABLED;
    public static ModConfigSpec.BooleanValue SWEETENED_OVERLAY_ENABLED;
    public static ModConfigSpec.BooleanValue SATIATING_OVERLAY_ENABLED;

    public static boolean isLoaded() {
        return com.onetoys.Config.SPEC != null && com.onetoys.Config.SPEC.isLoaded() && SaltModule.isSaltEnabled();
    }

    public static void buildConfig(ModConfigSpec.Builder builder) {
        builder.comment("Salting foods to provide an additional heart to max health. Can be disabled by removing 'onetoys:salting' recipe " +
                "or removing all items from 'onetoys/tags/item/can_be_salted' tag.").push("Salting");

        SALTING_BONUS_HEARTS = builder
                .comment("Bonus hearts provided to max health by salted food (1 = +1 heart / +2 HP).")
                .defineInRange("SaltingBonusHearts", 1, 0, 10);

        builder.pop();

        builder.push("Sweetening");

        SWEETENING_REGEN_BONUS = builder
                .comment("Bonus percentage to health regeneration provided by sweetened food (0.25 = +25%).")
                .defineInRange("SweeteningRegenBonus", 0.25d, 0.0d, 10.0d);

        builder.pop();

        builder.push("Satiating");

        SATIATING_DURATION_MULTIPLIER = builder
                .comment("Duration multiplier provided by satiating (chrysanthemum) food (2.0 = 200% of base / double duration).")
                .defineInRange("SatiatingDurationMultiplier", 2.0d, 1.0d, 10.0d);

        builder.pop();

        builder.push("Dissolving");

        DISSOLVING_ENABLED = builder
                .comment("Salt blocks will dissolve if adjacent to blocks defined in the tag 'onetoys/tags/blocks/salt_dissolvables'")
                .define("SaltDissolvingEnabled", true);

        DISSOLVING_CHANCE = builder
                .comment("Chance of water dissolving in fluid on random tick. 1.0 = first random tick. 0.0 = never.")
                .defineInRange("SaltDissolvingChance", 0.35d, 0.0d, 1.0d);

        DISSOLVING_FLUID_SOURCE_CONVERSION = builder
                .comment("If dissolved by a fluid source block - salt will convert to a fluid source block instead of air.")
                .define("SaltDissolvesIntoSourceBlocks", true);

        DISSOLVING_IN_RAIN = builder
                .comment("Salt blocks will dissolve when exposed to rain")
                .define("SaltDissolvingInRain", true);

        DISSOLVING_IN_RAIN_CHANCE = builder
                .comment("Chance of salt blocks dissolving in rain on random tick. 1.0 = first random tick. 0.0 = never.")
                .defineInRange("SaltDissolvingInRainChance", 0.15d, 0.0d, 1.0d);

        builder.pop();

        builder.push("Melting");

        MELTING_ITEM_ENABLED = builder
                .comment("Salt (item) will melt clicked blocks defined in tag 'onetoys/tags/blocks/meltables'.")
                .define("SaltItemMeltingEnabled", true);

        MELTING_BY_BLOCK_ENABLED = builder
                .comment("Salt blocks will melt adjacent blocks defined in tag 'onetoys/tags/blocks/meltables' on random tick.")
                .define("SaltBlockMeltingEnabled", true);

        MELTING_BLOCK_CHANCE = builder
                .comment("Chance of block melting on random tick. 1.0 = first random tick. 0.0 = never.")
                .defineInRange("SaltBlockMeltingChance", 0.4d, 0.0d, 1.0d);

        MELTING_PLACES_WATER = builder
                .comment("Melted block will be replaced with Water source block.")
                .define("SaltMeltingPlaceWater", true);

        builder.pop();

        builder.push("Evaporation");

        EVAPORATION_ENABLED = builder
                .comment("Water in a cauldron with a heat source beneath (defined in tag 'onetoys/tags/blocks/heaters') " +
                        "will evaporate and salt will be formed in the cauldron")
                .define("EvaporationEnabled", true);
        EVAPORATION_CHANCE = builder
                .comment("Chance of water evaporating on random tick. 1.0 = first random tick. 0.0 = never.")
                .defineInRange("EvaporationChance", 0.3d, 0.0d, 1.0d);

        builder.pop();

        builder.push("GrowingSaltClusters");

        SALT_CLUSTER_GROWING_ENABLED = builder
                .comment("Water dripping from a Pointed Dripstone on a blocks tagged as 'onetoys/tags/blocks/salt_cluster_growables' will grow Salt Clusters")
                .define("SaltClusterGrowingEnabled", true);
        SALT_CLUSTER_GROWING_CHANCE = builder
                .comment("Chance of cluster growing by one stage on random tick. 1.0 = first random tick. 0.0 = never.")
                .defineInRange("SaltClusterGrowingChance", 0.1d, 0.0d, 1.0d);

        builder.pop();

        builder.comment("Rock Salt Deposits will generate in biomes defined in tag 'onetoys/tags/worldgen/biome/has_rock_salt_deposits'",
                        "Since 1.19 - parts of the Rock Salt generation is defined in jsons.",
                        "To enable/disable the generation or configure the generation chances you'll need to create a datapack.",
                        "Enable/disable generation - 'onetoys/worldgen/biome_modifier/add_rock_salt_deposit.json' - {\"type\": \"neoforge:none\"} will disable the generation.",
                        "Changing generation chances - 'onetoys/worldgen/placed_feature/mineral_rock_salt.json'")
                .push("RockSalt");

        ROCK_SALT_SIZE = builder
                .comment("Size of the Rock Salt deposit")
                .defineInRange("RockSaltSize", 24, 1, 64);

        ROCK_SALT_CLUSTER_CHANCE = builder
                .comment("Chance of the Salt Clusters generating on the deposits (per side)")
                .defineInRange("RockSaltClusterChance", 0.15d, 0.0d, 1.0d);

        builder.pop();

        builder.push("VillagerTrades");

        BUTCHER_SALT_TRADES_ENABLED = builder
                .comment("Butcher will buy and sell Salt.")
                .define("ButcherSaltTradesEnabled", true);

        builder.pop();

        builder.push("Compatibility");

        SALTED_OVERLAY_ENABLED = builder
                .comment("Overlay texture will be drawn over salted foods. Disable if there's an issue with rendering items.")
                .define("SaltingOverlayEnabled", true);

        SWEETENED_OVERLAY_ENABLED = builder
                .comment("Overlay texture will be drawn over sweetened foods. Disable if there's an issue with rendering items.")
                .define("SweeteningOverlayEnabled", true);

        SATIATING_OVERLAY_ENABLED = builder
                .comment("Overlay texture will be drawn over satiating (chrysanthemum) foods. Disable if there's an issue with rendering items.")
                .define("SatiatingOverlayEnabled", true);

        builder.pop();
    }
}
