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
    private final TungstenTweaks tungstenTweaks = new TungstenTweaks(this);
    private final NuggetsAndShardsTweaks nuggetsAndShardsTweaks = new NuggetsAndShardsTweaks(this);

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

    public static boolean isTungstenOreGenerationEnabled() {
        ContentModule mod = instance;
        return mod != null && mod.isEnabled() && mod.getTungstenTweaks().isOreGenerationEnabled();
    }

    public static boolean isCalciteStripesEnabled() {
        ContentModule mod = instance;
        return mod != null && mod.isEnabled() && mod.getTungstenTweaks().isCalciteStripesEnabled();
    }

    public static boolean isTungstenTweaksEnabled() {
        ContentModule mod = instance;
        return mod != null && mod.isEnabled() && mod.getTungstenTweaks().isEnabled();
    }

    public static boolean isNuggetsAndShardsEnabled() {
        ContentModule mod = instance;
        return mod != null && mod.isEnabled() && mod.getNuggetsAndShardsTweaks().isEnabled();
    }

    public static boolean isDumbbellEnabled() {
        ContentModule mod = instance;
        return mod != null && mod.isEnabled() && mod.getTungstenTweaks().isDumbbellEnabled();
    }

    public static boolean isWateringCanEnabled() {
        ContentModule mod = instance;
        return mod != null && mod.isEnabled() && mod.getTungstenTweaks().isWateringCanEnabled();
    }

    public TungstenTweaks getTungstenTweaks() {
        return tungstenTweaks;
    }

    public NuggetsAndShardsTweaks getNuggetsAndShardsTweaks() {
        return nuggetsAndShardsTweaks;
    }

    @Override
    public void buildConfig(ModConfigSpec.Builder builder) {
        tungstenTweaks.buildConfig(builder);
        nuggetsAndShardsTweaks.buildConfig(builder);
    }

    @Override
    public void onConstruct(IEventBus modBus) {
        tungstenTweaks.onConstruct(modBus);
        nuggetsAndShardsTweaks.onConstruct(modBus);
        modBus.addListener(this::onModifyDefaultComponents);
    }

    private void onModifyDefaultComponents(net.neoforged.neoforge.event.ModifyDefaultComponentsEvent event) {
        if (com.orehall.registry.OrehallItems.DUMBBELL != null && com.orehall.registry.OrehallItems.DUMBBELL.isBound()) {
            event.modify(com.orehall.registry.OrehallItems.DUMBBELL.get(), builder -> {
                builder.set(
                        net.minecraft.core.component.DataComponents.ATTRIBUTE_MODIFIERS,
                        com.orehall.item.DumbbellItem.createAttributes(tungstenTweaks.getDumbbellGravity())
                );
            });
        }
        if (com.orehall.registry.OrehallItems.WATERING_CAN != null && com.orehall.registry.OrehallItems.WATERING_CAN.isBound()) {
            event.modify(com.orehall.registry.OrehallItems.WATERING_CAN.get(), builder -> {
                builder.set(
                        com.orehall.registry.OrehallDataComponents.WATERING_CAN_USES.get(),
                        0
                );
            });
        }
    }

    @Override
    public void onCommonSetup(FMLCommonSetupEvent event) {
        tungstenTweaks.onCommonSetup(event);
        nuggetsAndShardsTweaks.onCommonSetup(event);
    }

    @Override
    public void onClientSetup(FMLClientSetupEvent event) {
        tungstenTweaks.onClientSetup(event);
        nuggetsAndShardsTweaks.onClientSetup(event);
    }
}
