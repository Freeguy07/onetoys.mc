package com.orehall;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import com.orehall.client.gui.OrehallConfigurationSectionScreen;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

// This class will not load on dedicated servers. Accessing client side code from here is safe.
@Mod(value = Orehall.MODID, dist = Dist.CLIENT)
// You can use EventBusSubscriber to automatically register all static methods in the class annotated with @SubscribeEvent
@EventBusSubscriber(modid = Orehall.MODID, value = Dist.CLIENT)
public class OrehallClient {
    public OrehallClient(ModContainer container) {
        // Allows NeoForge to create a config screen for this mod's configs.
        // The config screen is accessed by going to the Mods screen > clicking on your mod > clicking on config.
        // Do not forget to add translations for your config options to the en_us.json file.
        container.registerExtensionPoint(IConfigScreenFactory.class,
                (cont, modListScreen) -> new ConfigurationScreen(cont, modListScreen, OrehallConfigurationSectionScreen::new));
    }

    @SubscribeEvent
    static void onClientSetup(FMLClientSetupEvent event) {
        Orehall.LOGGER.info("Orehall client setup");
        com.orehall.module.ModuleManager.get().onClientSetup(event);
    }
}
