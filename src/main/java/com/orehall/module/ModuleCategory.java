package com.orehall.module;

public enum ModuleCategory {
    RECIPE_TWEAKS("recipe_tweaks", "Recipe Tweaks"),
    GAMEPLAY("gameplay", "Gameplay"),
    BUILDING("building", "Building"),
    TWEAKS("tweaks", "Tweaks"),
    TOOLS("tools", "Tools"),
    WORLD("world", "World");

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
