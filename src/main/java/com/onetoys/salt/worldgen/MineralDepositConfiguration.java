package com.onetoys.salt.worldgen;

import com.onetoys.salt.SaltConfiguration;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;
import net.minecraft.world.level.levelgen.structure.templatesystem.RuleTest;

import java.util.List;

public record MineralDepositConfiguration(List<DepositBlockStateInfo> mainStateInfos,
                                          DepositBlockStateInfo clusterStateInfo) implements FeatureConfiguration {
    public static final Codec<MineralDepositConfiguration> CODEC = RecordCodecBuilder.create((instance) ->
            instance.group(
                    Codec.list(DepositBlockStateInfo.CODEC)
                            .fieldOf("mainTargets")
                            .forGetter(config -> config.mainStateInfos),
                    DepositBlockStateInfo.CODEC
                            .fieldOf("clusterTargets")
                            .forGetter(config -> config.clusterStateInfo))
                    .apply(instance, MineralDepositConfiguration::new));

    public int getSize() {
        if (SaltConfiguration.ROCK_SALT_SIZE != null && SaltConfiguration.isLoaded()) {
            return SaltConfiguration.ROCK_SALT_SIZE.get();
        }
        return 24;
    }

    public float getClusterChance() {
        if (SaltConfiguration.ROCK_SALT_CLUSTER_CHANCE != null && SaltConfiguration.isLoaded()) {
            return SaltConfiguration.ROCK_SALT_CLUSTER_CHANCE.get().floatValue();
        }
        return 0.15f;
    }

    public static class DepositBlockStateInfo {
        public static final Codec<DepositBlockStateInfo> CODEC = RecordCodecBuilder.create(instance ->
                instance.group(
                        BlockStateProvider.CODEC.fieldOf("blockStateProvider")
                                .forGetter(target -> target.blockStateProvider),
                        RuleTest.CODEC.fieldOf("ruleTest")
                                .forGetter(target -> target.ruleTest))
                        .apply(instance, DepositBlockStateInfo::new));

        public final BlockStateProvider blockStateProvider;
        public final RuleTest ruleTest;

        public DepositBlockStateInfo(BlockStateProvider blockStateProvider, RuleTest ruleTest) {
            this.ruleTest = ruleTest;
            this.blockStateProvider = blockStateProvider;
        }
    }
}
