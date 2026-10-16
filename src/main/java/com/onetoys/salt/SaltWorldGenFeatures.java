package com.onetoys.salt;

import com.onetoys.salt.worldgen.MineralDepositConfiguration;
import com.onetoys.salt.worldgen.MineralDepositFeature;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class SaltWorldGenFeatures {
    public static final DeferredRegister<Feature<?>> FEATURES =
            DeferredRegister.create(BuiltInRegistries.FEATURE, Salt.ID);

    public static final DeferredHolder<Feature<?>, Feature<MineralDepositConfiguration>> MINERAL_DEPOSIT =
            FEATURES.register("mineral_deposit", () -> new MineralDepositFeature(MineralDepositConfiguration.CODEC));
}

