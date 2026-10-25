package com.onetoys.module;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.onetoys.module.content.ContentModule;
import com.onetoys.module.gameplay.GameplayTweaksModule;
import com.onetoys.module.integration.ModIntegrationModule;
import com.onetoys.module.recipetweaks.RecipeTweaksModule;
import com.onetoys.module.worldgen.WorldgenTweaksModule;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

public class ModuleManager {
    private static final ModuleManager INSTANCE = new ModuleManager();
    private final Map<String, OnetoysModule> modules = new LinkedHashMap<>();
    private final Map<Class<? extends OnetoysModule>, OnetoysModule> modulesByClass = new LinkedHashMap<>();

    private ModuleManager() {}

    public static ModuleManager get() {
        return INSTANCE;
    }

    public void init() {
        if (modules.isEmpty()) {
            register(new RecipeTweaksModule());
            register(new GameplayTweaksModule());
            register(new WorldgenTweaksModule());
            register(new ContentModule());
            register(new ModIntegrationModule());
        }
    }

    public void register(OnetoysModule module) {
        if (modules.containsKey(module.getId())) {
            throw new IllegalArgumentException("Module with ID " + module.getId() + " is already registered!");
        }
        modules.put(module.getId(), module);
        modulesByClass.put(module.getClass(), module);
    }

    public List<OnetoysModule> getModules() {
        return List.copyOf(modules.values());
    }

    @SuppressWarnings("unchecked")
    public <T extends OnetoysModule> Optional<T> getModule(Class<T> moduleClass) {
        OnetoysModule module = modulesByClass.get(moduleClass);
        if (module != null) {
            return Optional.of((T) module);
        }
        return modules.values().stream()
                .filter(moduleClass::isInstance)
                .map(m -> (T) m)
                .findFirst();
    }

    public Optional<OnetoysModule> getModule(String id) {
        return Optional.ofNullable(modules.get(id));
    }

    public boolean isModuleEnabled(String id) {
        OnetoysModule module = modules.get(id);
        return module != null && module.isEnabled();
    }

    public void buildConfig(ModConfigSpec.Builder builder) {
        for (OnetoysModule module : modules.values()) {
            builder.comment(module.getDescription())
                    .translation("onetoys.configuration." + module.getId())
                    .push(module.getId());
            ModConfigSpec.BooleanValue enabledVal = builder
                    .comment(module.getDescription(), "Default: " + module.isEnabledByDefault())
                    .translation("onetoys.configuration." + module.getId() + ".enabled")
                    .define("enabled", module.isEnabledByDefault());
            module.setEnabledConfig(enabledVal);

            module.buildConfig(builder);
            builder.pop();
        }
    }

    public void onConstruct(IEventBus modBus) {
        for (OnetoysModule module : modules.values()) {
            module.onConstruct(modBus);
        }
    }

    public void onCommonSetup(FMLCommonSetupEvent event) {
        for (OnetoysModule module : modules.values()) {
            if (module.isEnabled()) {
                module.onCommonSetup(event);
            }
        }
    }

    public void onClientSetup(FMLClientSetupEvent event) {
        for (OnetoysModule module : modules.values()) {
            if (module.isEnabled()) {
                module.onClientSetup(event);
            }
        }
    }
}
