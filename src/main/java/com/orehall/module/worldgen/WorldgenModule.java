package com.orehall.module.worldgen;

import com.orehall.module.ModuleCategory;
import com.orehall.module.OrehallModule;
import net.neoforged.neoforge.common.ModConfigSpec;

public class WorldgenModule extends OrehallModule {
    public static final String ID = "worldgen";

    private static WorldgenModule instance;
    private final GiantOreVeinsDisabler giantOreVeinsDisabler = new GiantOreVeinsDisabler();

    public WorldgenModule() {
        super(ID, "World Generation", "Tweaks and adjustments to world generation.", ModuleCategory.WORLDGEN, true);
        instance = this;
    }

    @Override
    public void buildConfig(ModConfigSpec.Builder builder) {
        giantOreVeinsDisabler.buildConfig(builder);
    }

    public GiantOreVeinsDisabler getGiantOreVeinsDisabler() {
        return giantOreVeinsDisabler;
    }

    public static GiantOreVeinsDisabler getDisabler() {
        WorldgenModule mod = instance;
        return (mod != null && mod.isEnabled()) ? mod.giantOreVeinsDisabler : null;
    }
}
