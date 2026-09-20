package com.orehall.worldgen.feature;

import java.util.List;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import net.minecraft.world.level.levelgen.structure.templatesystem.RuleTest;

/**
 * Configuration for geological Calcite Strata layered blobs.
 * Defines target replaceable blocks, dimensions, and ordered 1-block disc layers.
 */
public record CalciteStrataConfiguration(
        List<BlockState> layers,
        RuleTest target,
        int horizontalRadius,
        int verticalRadius,
        float discardChanceOnAirExposure,
        int maxTiltDegrees
) implements FeatureConfiguration {
    public static final Codec<CalciteStrataConfiguration> CODEC = RecordCodecBuilder.create(
            builder -> builder.group(
                    BlockState.CODEC.listOf().fieldOf("layers").forGetter(CalciteStrataConfiguration::layers),
                    RuleTest.CODEC.fieldOf("target").forGetter(CalciteStrataConfiguration::target),
                    Codec.INT.fieldOf("horizontal_radius").orElse(15).forGetter(CalciteStrataConfiguration::horizontalRadius),
                    Codec.INT.fieldOf("vertical_radius").orElse(9).forGetter(CalciteStrataConfiguration::verticalRadius),
                    Codec.FLOAT.fieldOf("discard_chance_on_air_exposure").orElse(0.1F).forGetter(CalciteStrataConfiguration::discardChanceOnAirExposure),
                    Codec.INT.fieldOf("max_tilt_degrees").orElse(25).forGetter(CalciteStrataConfiguration::maxTiltDegrees)
            ).apply(builder, CalciteStrataConfiguration::new)
    );
}
