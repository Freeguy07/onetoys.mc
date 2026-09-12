package com.orehall.module;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

public class ModuleManager {
    private static final ModuleManager INSTANCE = new ModuleManager();
    private final Map<String, OrehallModule> modules = new LinkedHashMap<>();
    private final Map<Class<? extends OrehallModule>, OrehallModule> modulesByClass = new LinkedHashMap<>();

    private ModuleManager() {}

    public static ModuleManager get() {
        return INSTANCE;
    }

    public void init() {
        if (modules.isEmpty()) {
            register(new com.orehall.module.recipetweaks.RecipeTweaksModule());
            register(new com.orehall.module.gameplay.GameplayTweaksModule());
            register(new com.orehall.module.worldgen.WorldgenModule());
        }
    }

    public void register(OrehallModule module) {
        if (modules.containsKey(module.getId())) {
            throw new IllegalArgumentException("Module with ID " + module.getId() + " is already registered!");
        }
        modules.put(module.getId(), module);
        modulesByClass.put(module.getClass(), module);
    }

    public List<OrehallModule> getModules() {
        return List.copyOf(modules.values());
    }

    @SuppressWarnings("unchecked")
    public <T extends OrehallModule> Optional<T> getModule(Class<T> moduleClass) {
        OrehallModule module = modulesByClass.get(moduleClass);
        if (module != null) {
            return Optional.of((T) module);
        }
        return modules.values().stream()
                .filter(moduleClass::isInstance)
                .map(m -> (T) m)
                .findFirst();
    }

    public Optional<OrehallModule> getModule(String id) {
        return Optional.ofNullable(modules.get(id));
    }

    public boolean isModuleEnabled(String id) {
        OrehallModule module = modules.get(id);
        return module != null && module.isEnabled();
    }

    public void buildConfig(ModConfigSpec.Builder builder) {
        for (OrehallModule module : modules.values()) {
            builder.comment(module.getDescription())
                    .translation("orehall.configuration." + module.getId())
                    .push(module.getId());
            ModConfigSpec.BooleanValue enabledVal = builder
                    .comment(module.getDescription(), "Default: " + module.isEnabledByDefault())
                    .translation("orehall.configuration." + module.getId() + ".enabled")
                    .define("enabled", module.isEnabledByDefault());
            module.setEnabledConfig(enabledVal);

            module.buildConfig(builder);
            builder.pop();
        }
    }

    public void onConstruct(IEventBus modBus) {
        for (OrehallModule module : modules.values()) {
            module.onConstruct(modBus);
        }
    }

    public void onCommonSetup(FMLCommonSetupEvent event) {
        for (OrehallModule module : modules.values()) {
            if (module.isEnabled()) {
                module.onCommonSetup(event);
            }
        }
    }

    public void onClientSetup(FMLClientSetupEvent event) {
        for (OrehallModule module : modules.values()) {
            if (module.isEnabled()) {
                module.onClientSetup(event);
            }
        }
    }
}
