package com.orehall;

import com.orehall.module.ModuleManager;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.ModConfigSpec;

public class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();
    public static final ModConfigSpec SPEC;

    static {
        ModuleManager.get().init();
        ModuleManager.get().buildConfig(BUILDER);
        SPEC = BUILDER.build();
    }
}
