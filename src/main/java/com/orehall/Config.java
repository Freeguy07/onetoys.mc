package com.orehall;

import com.orehall.module.ModuleManager;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.ModConfigSpec;

public class Config {
    private static final ModConfigSpec.Builder COMMON_BUILDER = new ModConfigSpec.Builder();
    private static final ModConfigSpec.Builder STARTUP_BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec COMMON_SPEC;
    public static final ModConfigSpec STARTUP_SPEC;

    static {
        ModuleManager.get().init();

        ModuleManager.get().buildConfig(COMMON_BUILDER, ModConfig.Type.COMMON);
        COMMON_SPEC = COMMON_BUILDER.build();

        ModuleManager.get().buildConfig(STARTUP_BUILDER, ModConfig.Type.STARTUP);
        STARTUP_SPEC = STARTUP_BUILDER.build();
    }
}
