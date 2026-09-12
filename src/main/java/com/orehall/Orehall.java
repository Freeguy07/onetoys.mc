package com.orehall;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;
import com.orehall.crafting.condition.OrehallConditions;
import com.orehall.module.ModuleManager;
import com.orehall.registry.OrehallAttachments;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;

@Mod(Orehall.MODID)
public class Orehall {
    public static final String MODID = "orehall";
    public static final Logger LOGGER = LogUtils.getLogger();

    public Orehall(IEventBus modEventBus, ModContainer modContainer) {
        // Register condition serializers
        OrehallConditions.register(modEventBus);
        // Register data attachments
        OrehallAttachments.register(modEventBus);

        // Initialize modules and lifecycle
        ModuleManager.get().init();
        ModuleManager.get().onConstruct(modEventBus);

        // Register the commonSetup method for modloading
        modEventBus.addListener(this::commonSetup);

        // Register unified mod configuration
        modContainer.registerConfig(ModConfig.Type.STARTUP, Config.SPEC, "orehall.toml");
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        LOGGER.info("Orehall common setup");
        ModuleManager.get().onCommonSetup(event);
    }
}
