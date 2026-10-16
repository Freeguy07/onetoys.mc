package com.onetoys.module.content;

import com.onetoys.module.ModuleCategory;
import com.onetoys.module.ModuleManager;
import com.onetoys.module.OnetoysModule;
import com.onetoys.salt.SaltConfiguration;
import net.neoforged.neoforge.common.ModConfigSpec;

public class SaltModule extends OnetoysModule {
    public static final String ID = "salt";
    private static SaltModule instance;

    public SaltModule() {
        super(ID, "Salt", "Salt mineral deposits, evaporation, food seasoning, and dissolving mechanics.", ModuleCategory.CONTENT, true);
        instance = this;
    }

    public static SaltModule getInstance() {
        return instance;
    }

    public static boolean isSaltEnabled() {
        SaltModule mod = instance;
        if (mod == null) {
            mod = ModuleManager.get().getModule(SaltModule.class).orElse(null);
        }
        return mod == null || mod.isEnabled();
    }

    @Override
    public void buildConfig(ModConfigSpec.Builder builder) {
        SaltConfiguration.buildConfig(builder);
    }
}
