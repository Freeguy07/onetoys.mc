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
        return switch (tweak) {
            case "salt" -> ContentModule.isSaltEnabled();
            case "nether_locked_lapis", "nether_lapis_ore" -> WorldgenTweaksModule.isNetherLockedLapisEnabled();
            case "tungsten_ore" -> ContentModule.isTungstenOreGenerationEnabled();
            case "calcite_stripes" -> ContentModule.isCalciteStripesEnabled();
            case "tungsten" -> ContentModule.isTungstenTweaksEnabled();
            case "nuggets_and_shards", "content" -> ContentModule.isNuggetsAndShardsEnabled();
            case "wetness" -> GameplayTweaksModule.isWetnessEnabled();
            case "matted_wool" -> GameplayTweaksModule.isMattedWoolFromWetSheepEnabled();
            default -> false;
        };
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
