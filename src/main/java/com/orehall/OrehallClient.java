package com.orehall;

import com.orehall.client.gui.OrehallConfigurationSectionScreen;
import com.orehall.client.particle.NetherLapisDripParticle;
import com.orehall.module.ModuleManager;
import com.orehall.registry.OrehallParticles;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

// This class will not load on dedicated servers. Accessing client side code from here is safe.
@Mod(value = Orehall.MODID, dist = Dist.CLIENT)
public class OrehallClient {
    public OrehallClient(IEventBus modBus, ModContainer container) {
        // Allows NeoForge to create a config screen for this mod's configs.
        // The config screen is accessed by going to the Mods screen > clicking on your mod > clicking on config.
        // Do not forget to add translations for your config options to the en_us.json file.
        container.registerExtensionPoint(IConfigScreenFactory.class,
                (cont, modListScreen) -> new ConfigurationScreen(cont, modListScreen, OrehallConfigurationSectionScreen::new));

        modBus.addListener(OrehallClient::onClientSetup);
        modBus.addListener(OrehallClient::onRegisterParticleProviders);

        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(com.orehall.client.WetnessClientHelper::onClientEntityTick);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(com.orehall.client.ItemActionBarClientHandler::onClientTick);
    }

    private static final float[] STOPWATCH_FRAMES = new float[32];
    static {
        for (int i = 0; i < 32; i++) {
            STOPWATCH_FRAMES[i] = (float) i;
        }
    }

    private static void onClientSetup(FMLClientSetupEvent event) {
        Orehall.LOGGER.info("Orehall client setup");
        ModuleManager.get().onClientSetup(event);

        event.enqueueWork(() -> {
            var componentHolder = com.orehall.registry.OrehallDataComponents.STOPWATCH;
            net.minecraft.client.renderer.item.ItemProperties.register(
                    com.orehall.registry.OrehallItems.STOPWATCH.get(),
                    net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(Orehall.MODID, "frame"),
                    (stack, level, entity, seed) -> {
                        com.orehall.item.StopwatchData data = stack.get(componentHolder.get());
                        if (data == null || (!data.active() && data.elapsedTicks() <= 0L)) {
                            return 0.0F;
                        }
                        if (!data.active()) {
                            return STOPWATCH_FRAMES[(int) (data.elapsedTicks() & 31L)];
                        }
                        if (level == null) {
                            level = (entity != null && entity.level() instanceof net.minecraft.client.multiplayer.ClientLevel cl)
                                    ? cl : net.minecraft.client.Minecraft.getInstance().level;
                        }
                        if (level == null) {
                            return 0.0F;
                        }
                        long elapsed = data.elapsedTicks() + Math.max(0L, level.getGameTime() - data.startTick());
                        return STOPWATCH_FRAMES[(int) (elapsed & 31L)];
                    }
            );

            net.minecraft.client.renderer.item.ItemProperties.register(
                    com.orehall.registry.OrehallItems.WATERING_CAN.get(),
                    net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(Orehall.MODID, "watered"),
                    (stack, level, entity, seed) -> com.orehall.item.WateringCanItem.getWaterLevel(stack) > 0 ? 1.0F : 0.0F
            );
        });
    }

    private static void onRegisterParticleProviders(RegisterParticleProvidersEvent event) {
        event.registerSprite(OrehallParticles.DRIPPING_NETHER_LAPIS.get(),
                (type, level, x, y, z, xSpeed, ySpeed, zSpeed) ->
                        new NetherLapisDripParticle.HangParticle(
                                level, x, y, z, OrehallParticles.FALLING_NETHER_LAPIS.get()
                        ));

        event.registerSprite(OrehallParticles.FALLING_NETHER_LAPIS.get(),
                (type, level, x, y, z, xSpeed, ySpeed, zSpeed) ->
                        new NetherLapisDripParticle.FallAndLandParticle(
                                level, x, y, z, OrehallParticles.LANDING_NETHER_LAPIS.get()
                        ));

        event.registerSprite(OrehallParticles.LANDING_NETHER_LAPIS.get(),
                (type, level, x, y, z, xSpeed, ySpeed, zSpeed) ->
                        new NetherLapisDripParticle.LandParticle(level, x, y, z));
    }
}
