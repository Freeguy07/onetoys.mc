package com.orehall.module;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

public abstract class OrehallModule {
    private final String id;
    private final String name;
    private final String description;
    private final ModuleCategory category;
    private final boolean defaultEnabled;
    private ModConfigSpec.BooleanValue enabledConfig;

    public OrehallModule(String id, String name, String description, ModuleCategory category) {
        this(id, name, description, category, true);
    }

    public OrehallModule(String id, String name, String description, ModuleCategory category, boolean defaultEnabled) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.category = category;
        this.defaultEnabled = defaultEnabled;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public ModuleCategory getCategory() {
        return category;
    }

    public boolean isEnabledByDefault() {
        return defaultEnabled;
    }

    public boolean isEnabled() {
        return enabledConfig == null ? defaultEnabled : enabledConfig.get();
    }

    public void setEnabledConfig(ModConfigSpec.BooleanValue enabledConfig) {
        this.enabledConfig = enabledConfig;
    }

    public net.neoforged.fml.config.ModConfig.Type getConfigType() {
        return net.neoforged.fml.config.ModConfig.Type.COMMON;
    }

    public void buildConfig(ModConfigSpec.Builder builder) {
        // Optional child config options for modules
    }

    public void onConstruct(IEventBus modBus) {
        // Called during mod construction
    }

    public void onCommonSetup(FMLCommonSetupEvent event) {
        // Called during common setup
    }

    public void onClientSetup(FMLClientSetupEvent event) {
        // Called during client setup
    }
}
