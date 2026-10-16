package com.onetoys.crafting.condition;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.onetoys.module.content.ContentModule;
import com.onetoys.module.gameplay.GameplayTweaksModule;
import com.onetoys.module.worldgen.WorldgenTweaksModule;

import net.neoforged.neoforge.common.conditions.ICondition;

public record OreTweakEnabledCondition(String tweak) implements ICondition {
    public static final MapCodec<OreTweakEnabledCondition> CODEC = RecordCodecBuilder.mapCodec(
            builder -> builder.group(
                    Codec.STRING.fieldOf("tweak").forGetter(OreTweakEnabledCondition::tweak)
            ).apply(builder, OreTweakEnabledCondition::new)
    );

    @Override
    public boolean test(IContext context) {
        if ("nether_locked_lapis".equals(tweak) || "nether_lapis_ore".equals(tweak)) {
            return WorldgenTweaksModule.isNetherLockedLapisEnabled();
        }
        if ("tungsten_ore".equals(tweak)) {
            return ContentModule.isTungstenOreGenerationEnabled();
        }
        if ("calcite_stripes".equals(tweak)) {
            return ContentModule.isCalciteStripesEnabled();
        }
        if ("tungsten".equals(tweak)) {
            return ContentModule.isTungstenTweaksEnabled();
        }
        if ("nuggets_and_shards".equals(tweak) || "content".equals(tweak)) {
            return ContentModule.isNuggetsAndShardsEnabled();
        }
        if ("wetness".equals(tweak)) {
            return GameplayTweaksModule.isWetnessEnabled();
        }
        if ("matted_wool".equals(tweak)) {
            return GameplayTweaksModule.isMattedWoolFromWetSheepEnabled();
        }
        return false;
    }

    @Override
    public MapCodec<? extends ICondition> codec() {
        return CODEC;
    }

    @Override
    public String toString() {
        return "ore_tweak_enabled(\"" + tweak + "\")";
    }
}
