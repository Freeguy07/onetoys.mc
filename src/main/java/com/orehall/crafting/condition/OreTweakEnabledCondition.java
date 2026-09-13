package com.orehall.crafting.condition;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.orehall.module.worldgen.WorldgenTweaksModule;

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
        if ("chrome_ore".equals(tweak)) {
            return com.orehall.module.content.ContentModule.isChromeOreGenerationEnabled();
        }
        if ("chrome".equals(tweak)) {
            return com.orehall.module.content.ContentModule.isChromeTweaksEnabled();
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
