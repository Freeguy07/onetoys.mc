package com.onetoys.salt;

import com.onetoys.module.gameplay.wetness.WetnessTweaks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

public class Heater {
    public static boolean isHeatSource(BlockState blockState) {
        return (blockState.is(SaltTags.Blocks.HEATERS) || blockState.is(WetnessTweaks.HEAT_SOURCES) || blockState.is(WetnessTweaks.C_HEAT_SOURCES)) &&
                (!blockState.hasProperty(BlockStateProperties.LIT) || blockState.getValue(BlockStateProperties.LIT));
    }
}

