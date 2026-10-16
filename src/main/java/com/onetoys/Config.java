package com.onetoys;

import com.onetoys.module.ModuleManager;
import com.onetoys.salt.SaltConfiguration;
import com.onetoys.sol_valheim.ModConfig;
import net.neoforged.neoforge.common.ModConfigSpec;

public class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();
    public static final ModConfigSpec SPEC;

    static {
        ModuleManager.get().init();
        ModuleManager.get().buildConfig(BUILDER);
        SPEC = BUILDER.build();
        SaltConfiguration.COMMON_SPEC = SPEC;
        SaltConfiguration.CLIENT_SPEC = SPEC;
        ModConfig.COMMON_SPEC = SPEC;
        ModConfig.CLIENT_SPEC = SPEC;
    }
}
