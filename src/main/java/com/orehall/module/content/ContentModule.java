package com.orehall.module.content;

import com.orehall.module.ModuleCategory;
import com.orehall.module.OrehallModule;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

public class ContentModule extends OrehallModule {
    public static final String ID = "content";

    private static ContentModule instance;
    private final ChromeTweaks chromeTweaks = new ChromeTweaks(this);

    public ContentModule() {
        super(ID, "Content", "Mod-specific items, blocks, and original content additions.", ModuleCategory.CONTENT, true);
        instance = this;
    }

    public static ContentModule getInstance() {
        return instance;
    }

    public static boolean isContentEnabled() {
        return instance != null && instance.isEnabled();
    }

    public static boolean isChromeOreGenerationEnabled() {
        ContentModule mod = instance;
        return mod != null && mod.isEnabled() && mod.getChromeTweaks().isOreGenerationEnabled();
    }

    public static boolean isChromeTweaksEnabled() {
        ContentModule mod = instance;
        return mod != null && mod.isEnabled() && mod.getChromeTweaks().isEnabled();
    }

    public ChromeTweaks getChromeTweaks() {
        return chromeTweaks;
    }

    @Override
    public void buildConfig(ModConfigSpec.Builder builder) {
        chromeTweaks.buildConfig(builder);
    }

    @Override
    public void onConstruct(IEventBus modBus) {
        chromeTweaks.onConstruct(modBus);
    }

    @Override
    public void onCommonSetup(FMLCommonSetupEvent event) {
        chromeTweaks.onCommonSetup(event);
    }

    @Override
    public void onClientSetup(FMLClientSetupEvent event) {
        chromeTweaks.onClientSetup(event);
    }
}
