package com.onetoys;

import com.onetoys.client.gui.OnetoysConfigurationSectionScreen;
import com.onetoys.client.particle.NetherLapisDripParticle;
import com.onetoys.module.ModuleManager;
import com.onetoys.registry.OnetoysEntityTypes;
import com.onetoys.registry.OnetoysParticles;

import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

// This class will not load on dedicated servers. Accessing client side code from here is safe.
@Mod(value = Onetoys.MODID, dist = Dist.CLIENT)
public class OnetoysClient {
    public OnetoysClient(IEventBus modBus, ModContainer container) {
        // Allows NeoForge to create a config screen for this mod's configs.
        // The config screen is accessed by going to the Mods screen > clicking on your mod > clicking on config.
        // Do not forget to add translations for your config options to the en_us.json file.
        container.registerExtensionPoint(IConfigScreenFactory.class,
                (cont, modListScreen) -> new ConfigurationScreen(cont, modListScreen, OnetoysConfigurationSectionScreen::new));

        modBus.addListener(OnetoysClient::onClientSetup);
        modBus.addListener(OnetoysClient::onRegisterEntityRenderers);
        modBus.addListener(OnetoysClient::onRegisterParticleProviders);
        modBus.addListener(OnetoysClient::registerBlockColors);

        com.onetoys.salt.client.SaltClientEvents.init(modBus);

        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(com.onetoys.client.WetnessClientHelper::onClientEntityTick);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(com.onetoys.client.SpecialMobClientHelper::onClientEntityTick);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(com.onetoys.client.ItemActionBarClientHandler::onClientTick);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(com.onetoys.client.RecoveryCompassTooltipHandler::onItemTooltip);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(com.onetoys.client.CharcoalTooltipHandler::onItemTooltip);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(com.onetoys.client.StopwatchTooltipHandler::onItemTooltip);
    }

    private static void onRegisterEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(OnetoysEntityTypes.TANGLED_THREAD.get(), ThrownItemRenderer::new);
    }

    private static final float[] STOPWATCH_FRAMES = new float[32];
    static {
        for (int i = 0; i < 32; i++) {
            STOPWATCH_FRAMES[i] = (float) i;
        }
    }

    private static void onClientSetup(FMLClientSetupEvent event) {
        Onetoys.LOGGER.info("Onetoys client setup");
        ModuleManager.get().onClientSetup(event);

        event.enqueueWork(() -> {
            var componentHolder = com.onetoys.registry.OnetoysDataComponents.STOPWATCH;
            net.minecraft.client.renderer.item.ItemProperties.register(
                    com.onetoys.registry.OnetoysItems.STOPWATCH.get(),
                    net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(Onetoys.MODID, "frame"),
                    (stack, level, entity, seed) -> {
                        com.onetoys.item.StopwatchData data = stack.get(componentHolder.get());
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
                    com.onetoys.registry.OnetoysItems.WATERING_CAN.get(),
                    net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(Onetoys.MODID, "watered"),
                    (stack, level, entity, seed) -> com.onetoys.item.WateringCanItem.getWaterLevel(stack) > 0 ? 1.0F : 0.0F
            );
        });
    }

    private static void onRegisterParticleProviders(RegisterParticleProvidersEvent event) {
        event.registerSprite(OnetoysParticles.DRIPPING_NETHER_LAPIS.get(),
                (type, level, x, y, z, xSpeed, ySpeed, zSpeed) ->
                        new NetherLapisDripParticle.HangParticle(
                                level, x, y, z, OnetoysParticles.FALLING_NETHER_LAPIS.get()
                        ));

        event.registerSprite(OnetoysParticles.FALLING_NETHER_LAPIS.get(),
                (type, level, x, y, z, xSpeed, ySpeed, zSpeed) ->
                        new NetherLapisDripParticle.FallAndLandParticle(
                                level, x, y, z, OnetoysParticles.LANDING_NETHER_LAPIS.get()
                        ));

        event.registerSprite(OnetoysParticles.LANDING_NETHER_LAPIS.get(),
                (type, level, x, y, z, xSpeed, ySpeed, zSpeed) ->
                        new NetherLapisDripParticle.LandParticle(level, x, y, z));
    }

    private static void registerBlockColors(net.neoforged.neoforge.client.event.RegisterColorHandlersEvent.Block event) {
        event.register(
                (state, level, pos, tintIndex) -> BluestoneWireBlock.getColorForPower(state.getValue(BluestoneWireBlock.POWER)),
                Onetoys.BLUESTONE_WIRE.get()
        );
    }
}
