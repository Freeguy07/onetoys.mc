package com.onetoys.worldgen.surface;

import com.onetoys.module.gameplay.weather.HeightWeatherTweaks;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Noises;
import net.minecraft.world.level.levelgen.SurfaceRules;
import net.minecraft.world.level.levelgen.placement.CaveSurface;

public class OnetoysSurfaceRules {

    // --- Tunables ---

    /** Elevation shift applied in near/transition zones to push lines upward: (1.0 - alpha) * LINE_SHIFT. */
    public static final double LINE_SHIFT = 100.0;

    /** Base elevation where steep slopes become exposed stone (surface depth multiplier 1). */
    private static final int STEEP_BASE = 115;

    /** Base elevation for full rock coverage (surface depth multiplier 2). */
    private static final int ROCK_BASE = 145;

    /** Base elevation for underside stone replacement on overhangs and plates (surface depth multiplier 0). */
    private static final int UNDERSIDE_BASE = 100;

    // Snow base is dynamic: HeightWeatherTweaks.getSnowHeight() (surface depth multiplier 2).

    // --- Condition Sources ---

    private static final SurfaceRules.ConditionSource SNOW_LINE = new AltitudeLine(HeightWeatherTweaks::getSnowHeight, 2);
    private static final SurfaceRules.ConditionSource ROCK_LINE = new AltitudeLine(() -> ROCK_BASE, 2);
    private static final SurfaceRules.ConditionSource STEEP_LINE = new AltitudeLine(() -> STEEP_BASE, 1);
    private static final SurfaceRules.ConditionSource UNDERSIDE_LINE = new AltitudeLine(() -> UNDERSIDE_BASE, 0);

    // --- Block State Rules ---

    private static final SurfaceRules.RuleSource STONE = SurfaceRules.state(Blocks.STONE.defaultBlockState());
    private static final SurfaceRules.RuleSource POWDER_SNOW = SurfaceRules.state(Blocks.POWDER_SNOW.defaultBlockState());
    private static final SurfaceRules.RuleSource SNOW_BLOCK = SurfaceRules.state(Blocks.SNOW_BLOCK.defaultBlockState());

    // --- Static Singleton RuleSource ---

    private static final SurfaceRules.RuleSource SKIN = buildSkin();

    public static SurfaceRules.RuleSource skin() {
        return SKIN;
    }

    private static SurfaceRules.RuleSource buildSkin() {
        // TOP BLOCK: ifTrue(stoneDepthCheck(0,false,0,FLOOR), ifTrue(waterBlockCheck(-1,0), sequence(...)))
        SurfaceRules.RuleSource top = SurfaceRules.ifTrue(
                SurfaceRules.stoneDepthCheck(0, false, 0, CaveSurface.FLOOR),
                SurfaceRules.ifTrue(
                        SurfaceRules.waterBlockCheck(-1, 0),
                        SurfaceRules.sequence(
                                SurfaceRules.ifTrue(
                                        SNOW_LINE,
                                        SurfaceRules.sequence(
                                                SurfaceRules.ifTrue(SurfaceRules.steep(), STONE),
                                                SurfaceRules.ifTrue(SurfaceRules.noiseCondition(Noises.POWDER_SNOW, 0.35, 0.6), POWDER_SNOW),
                                                SNOW_BLOCK
                                        )
                                ),
                                SurfaceRules.ifTrue(ROCK_LINE, STONE),
                                SurfaceRules.ifTrue(STEEP_LINE, SurfaceRules.ifTrue(SurfaceRules.steep(), STONE))
                        )
                )
        );

        // SUBSURFACE: ifTrue(stoneDepthCheck(0,true,0,FLOOR), sequence(...))
        SurfaceRules.RuleSource subsurface = SurfaceRules.ifTrue(
                SurfaceRules.stoneDepthCheck(0, true, 0, CaveSurface.FLOOR),
                SurfaceRules.sequence(
                        SurfaceRules.ifTrue(SNOW_LINE, STONE),
                        SurfaceRules.ifTrue(ROCK_LINE, STONE),
                        SurfaceRules.ifTrue(STEEP_LINE, SurfaceRules.ifTrue(SurfaceRules.steep(), STONE))
                )
        );

        // UNDERSIDE: ifTrue(undersideLine, ifTrue(not(stoneDepthCheck(0,true,0,FLOOR)), ifTrue(stoneDepthCheck(0,false,0,CEILING), STONE)))
        SurfaceRules.RuleSource underside = SurfaceRules.ifTrue(
                UNDERSIDE_LINE,
                SurfaceRules.ifTrue(
                        SurfaceRules.not(SurfaceRules.stoneDepthCheck(0, true, 0, CaveSurface.FLOOR)),
                        SurfaceRules.ifTrue(
                                SurfaceRules.stoneDepthCheck(0, false, 0, CaveSurface.CEILING),
                                STONE
                        )
                )
        );

        // Outer guards:
        // ifTrue(abovePreliminarySurface(), ifTrue(not(isBiome(<same EXCLUDED list>)), sequence(top, subsurface, underside)))
        return SurfaceRules.ifTrue(
                SurfaceRules.abovePreliminarySurface(),
                SurfaceRules.ifTrue(
                        SurfaceRules.not(SurfaceRules.isBiome(
                                Biomes.FROZEN_PEAKS,
                                Biomes.JAGGED_PEAKS,
                                Biomes.STONY_PEAKS,
                                Biomes.SNOWY_SLOPES,
                                Biomes.GROVE,
                                Biomes.WINDSWEPT_HILLS,
                                Biomes.WINDSWEPT_GRAVELLY_HILLS,
                                Biomes.WINDSWEPT_FOREST,
                                Biomes.WINDSWEPT_SAVANNA,
                                Biomes.STONY_SHORE,
                                Biomes.BADLANDS,
                                Biomes.ERODED_BADLANDS,
                                Biomes.WOODED_BADLANDS
                        )),
                        SurfaceRules.sequence(top, subsurface, underside)
                )
        );
    }
}
