package com.onetoys.module;

public enum ModuleCategory {
    RECIPE_TWEAKS("recipe_tweaks", "Recipe Tweaks"),
    GAMEPLAY("gameplay", "Gameplay"),
    WORLDGEN("worldgen", "Worldgen Tweaks"),
    CONTENT("content", "Content"),
    MOD_INTEGRATION("mod_integration", "Mod Integration");

    private final String id;
    private final String displayName;

    ModuleCategory(String id, String displayName) {
        this.id = id;
        this.displayName = displayName;
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }
}
