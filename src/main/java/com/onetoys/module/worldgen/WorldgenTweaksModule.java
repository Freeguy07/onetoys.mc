package com.onetoys.module.worldgen;

import com.onetoys.module.ModuleCategory;
import com.onetoys.module.OnetoysModule;
import net.neoforged.neoforge.common.ModConfigSpec;

public class WorldgenTweaksModule extends OnetoysModule {
    public static final String ID = "worldgen";

    private static WorldgenTweaksModule instance;
    private final OreTweaks oreTweaks = new OreTweaks();
    private final TerrainTweaks terrainTweaks = new TerrainTweaks();

    public WorldgenTweaksModule() {
        super(ID, "Worldgen Tweaks", "Tweaks and adjustments to world generation.", ModuleCategory.WORLDGEN, true);
        instance = this;
    }

    @Override
    public void buildConfig(ModConfigSpec.Builder builder) {
        oreTweaks.buildConfig(builder);
        terrainTweaks.buildConfig(builder);
    }

    public static WorldgenTweaksModule getInstance() {
        return instance;
    }

    public OreTweaks getOreTweaks() {
        return oreTweaks;
    }

    public TerrainTweaks getTerrainTweaks() {
        return terrainTweaks;
    }

    public GiantOreVeinsDisabler getGiantOreVeinsDisabler() {
        return oreTweaks.getGiantOreVeinsDisabler();
    }

    public static GiantOreVeinsDisabler getDisabler() {
        WorldgenTweaksModule mod = instance;
        if (mod == null || !mod.isEnabled()) {
            return null;
        }
        OreTweaks ore = mod.getOreTweaks();
        if (ore == null || !ore.isEnabled()) {
            return null;
        }
        GiantOreVeinsDisabler disabler = ore.getGiantOreVeinsDisabler();
        return (disabler != null && disabler.isEnabled()) ? disabler : null;
    }

    public static boolean isNetherLockedLapisEnabled() {
        WorldgenTweaksModule mod = instance;
        if (mod == null || !mod.isEnabled()) {
            return false;
        }
        OreTweaks ore = mod.getOreTweaks();
        return ore != null && ore.isNetherLockedLapisEnabled();
    }

    public static boolean isTerrainTweaksEnabled() {
        WorldgenTweaksModule mod = instance;
        if (mod == null || !mod.isEnabled()) {
            return false;
        }
        TerrainTweaks terrain = mod.getTerrainTweaks();
        return terrain != null && terrain.isEnabled();
    }
}
