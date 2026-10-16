package com.onetoys.module.gameplay.mobs.loot;

import com.mojang.serialization.MapCodec;
import com.onetoys.crafting.condition.OnetoysConditions;
import com.onetoys.module.ModuleManager;
import com.onetoys.module.gameplay.GameplayTweaksModule;

import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemConditionType;
import net.neoforged.neoforge.common.conditions.ICondition;

public class GoatDropsCondition implements ICondition, LootItemCondition {
    public static final GoatDropsCondition INSTANCE = new GoatDropsCondition();
    public static final MapCodec<GoatDropsCondition> CODEC = MapCodec.unit(INSTANCE);

    @Override
    public boolean test(IContext context) {
        return isEnabled();
    }

    @Override
    public boolean test(LootContext lootContext) {
        return isEnabled();
    }

    public static boolean isEnabled() {
        return ModuleManager.get().getModule(GameplayTweaksModule.class)
                .map(m -> m.isEnabled() && m.getMobTweaks().isEnabled() && m.getMobTweaks().getGoatTweaks().isMuttonDropEnabled())
                .orElse(false);
    }

    @Override
    public MapCodec<? extends ICondition> codec() {
        return CODEC;
    }

    @Override
    public LootItemConditionType getType() {
        return OnetoysConditions.GOAT_DROPS_TYPE.get();
    }
}
