package com.orehall.crafting.condition;

import com.mojang.serialization.MapCodec;
import com.orehall.Orehall;
import com.orehall.module.gameplay.mobs.loot.GoatDropsCondition;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.storage.loot.predicates.LootItemConditionType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.conditions.ICondition;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public class OrehallConditions {
    public static final DeferredRegister<MapCodec<? extends ICondition>> CONDITION_CODECS =
            DeferredRegister.create(NeoForgeRegistries.Keys.CONDITION_CODECS, Orehall.MODID);

    public static final DeferredRegister<LootItemConditionType> LOOT_CONDITION_TYPES =
            DeferredRegister.create(Registries.LOOT_CONDITION_TYPE, Orehall.MODID);

    public static final DeferredHolder<MapCodec<? extends ICondition>, MapCodec<RecipeEnabledCondition>> RECIPE_ENABLED =
            CONDITION_CODECS.register("recipe_enabled", () -> RecipeEnabledCondition.CODEC);

    public static final DeferredHolder<MapCodec<? extends ICondition>, MapCodec<GoatDropsCondition>> GOAT_DROPS_ENABLED =
            CONDITION_CODECS.register("goat_drops_enabled", () -> GoatDropsCondition.CODEC);

    public static final DeferredHolder<MapCodec<? extends ICondition>, MapCodec<OreTweakEnabledCondition>> ORE_TWEAK_ENABLED =
            CONDITION_CODECS.register("ore_tweak_enabled", () -> OreTweakEnabledCondition.CODEC);

    public static final DeferredHolder<LootItemConditionType, LootItemConditionType> GOAT_DROPS_TYPE =
            LOOT_CONDITION_TYPES.register("goat_drops_enabled", () -> new LootItemConditionType(GoatDropsCondition.CODEC));

    public static void register(IEventBus modBus) {
        CONDITION_CODECS.register(modBus);
        LOOT_CONDITION_TYPES.register(modBus);
    }
}
