package com.onetoys.module.gameplay;

import com.onetoys.module.ModuleCategory;
import com.onetoys.module.ModuleManager;
import com.onetoys.module.OnetoysModule;
import net.neoforged.neoforge.common.ModConfigSpec;

public class FoodOverhaulModule extends OnetoysModule {
    public static final String ID = "food_overhaul";
    private static FoodOverhaulModule instance;

    public FoodOverhaulModule() {
        super(ID, "Food & Survival", "Valheim-inspired food and survival overhaul mechanics.", ModuleCategory.GAMEPLAY, true);
        instance = this;
    }

    public static FoodOverhaulModule getInstance() {
        return instance;
    }

    public static boolean isFoodOverhaulEnabled() {
        FoodOverhaulModule mod = instance;
        if (mod == null) {
            mod = ModuleManager.get().getModule(FoodOverhaulModule.class).orElse(null);
        }
        return mod == null || mod.isEnabled();
    }

    @Override
    public void buildConfig(ModConfigSpec.Builder builder) {
        com.onetoys.sol_valheim.ModConfig.buildConfig(builder);
    }
}
