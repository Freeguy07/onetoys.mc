package com.orehall;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;
import com.orehall.crafting.condition.OrehallConditions;
import com.orehall.module.ModuleManager;
import com.orehall.registry.OrehallAttachments;
import com.orehall.registry.OrehallBlocks;
import com.orehall.registry.OrehallCreativeModeTabs;
import com.orehall.registry.OrehallItems;
import com.orehall.registry.OrehallParticles;

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
        // Register particles
        OrehallParticles.register(modEventBus);
        // Register blocks, items, and creative tabs
        OrehallBlocks.register(modEventBus);
        OrehallItems.register(modEventBus);
        OrehallCreativeModeTabs.register(modEventBus);

        // Initialize modules and lifecycle
        ModuleManager.get().init();
        ModuleManager.get().onConstruct(modEventBus);

        // Register mod event bus listeners
        modEventBus.addListener(this::commonSetup);

        // Register game event bus listeners
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(this::onFurnaceFuelBurnTime);

        // Register unified mod configuration
        modContainer.registerConfig(ModConfig.Type.STARTUP, Config.SPEC, "orehall.toml");
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        LOGGER.info("Orehall common setup");
        ModuleManager.get().onCommonSetup(event);
    }

    private void onFurnaceFuelBurnTime(net.neoforged.neoforge.event.furnace.FurnaceFuelBurnTimeEvent event) {
        if (event.getItemStack().is(OrehallItems.COAL_LUMP.get())) {
            event.setBurnTime(200);
        }
        if (event.getItemStack().is(net.minecraft.world.item.Items.CHARCOAL) && com.orehall.module.gameplay.GameplayTweaksModule.isDisableCharcoal()) {
            event.setBurnTime(0);
        }
    }
}
