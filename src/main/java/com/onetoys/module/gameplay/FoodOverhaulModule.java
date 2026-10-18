package com.onetoys.module.gameplay;

public class FoodOverhaulModule {
    public static final String ID = "food_overhaul";
    private static final FoodOverhaulModule INSTANCE = new FoodOverhaulModule();

    public static FoodOverhaulModule getInstance() {
        return INSTANCE;
    }

    public static boolean isFoodOverhaulEnabled() {
        return GameplayTweaksModule.isFoodOverhaulEnabled();
    }

    public boolean isEnabled() {
        return isFoodOverhaulEnabled();
    }
}
