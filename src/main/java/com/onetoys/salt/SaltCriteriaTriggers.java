package com.onetoys.salt;

import com.onetoys.salt.advancement.HarvestSaltCrystalTrigger;
import com.onetoys.salt.advancement.SaltEvaporationTrigger;
import com.onetoys.salt.advancement.SaltedFoodConsumedTrigger;
import net.minecraft.advancements.CriterionTrigger;
import net.minecraft.core.registries.BuiltInRegistries;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class SaltCriteriaTriggers {
    public static final DeferredRegister<CriterionTrigger<?>> CRITERIA_TRIGGERS =
            DeferredRegister.create(BuiltInRegistries.TRIGGER_TYPES, Salt.ID);

    public static final DeferredHolder<CriterionTrigger<?>, SaltEvaporationTrigger> SALT_EVAPORATED =
            CRITERIA_TRIGGERS.register("salt_evaporated", SaltEvaporationTrigger::new);

    public static final DeferredHolder<CriterionTrigger<?>, SaltedFoodConsumedTrigger> SALTED_FOOD_CONSUMED =
            CRITERIA_TRIGGERS.register("salted_food_consumed", SaltedFoodConsumedTrigger::new);

    public static final DeferredHolder<CriterionTrigger<?>, HarvestSaltCrystalTrigger> HARVEST_SALT_CRYSTAL =
            CRITERIA_TRIGGERS.register("harvest_salt_crystal", HarvestSaltCrystalTrigger::new);
}

