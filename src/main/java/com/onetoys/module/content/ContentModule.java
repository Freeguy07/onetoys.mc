package com.onetoys.module.content;

import com.onetoys.module.ModuleCategory;
import com.onetoys.module.OnetoysModule;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

public class ContentModule extends OnetoysModule {
    public static final String ID = "content";

    private static ContentModule instance;
    private final TungstenTweaks tungstenTweaks = new TungstenTweaks(this);
    private final NuggetsAndShardsTweaks nuggetsAndShardsTweaks = new NuggetsAndShardsTweaks(this);
    private final SaltTweaks saltTweaks = new SaltTweaks(this);

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

    public static boolean isTungstenTweaksEnabled() {
        ContentModule mod = instance;
        return mod != null && mod.isEnabled() && mod.getTungstenTweaks().isEnabled();
    }

    public static boolean isTungstenEnabled() {
        return isTungstenTweaksEnabled();
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

    public static boolean isSaltEnabled() {
        ContentModule mod = instance;
        return mod != null && mod.isEnabled() && mod.getSaltTweaks().isEnabled();
    }

    public TungstenTweaks getTungstenTweaks() {
        return tungstenTweaks;
    }

    public NuggetsAndShardsTweaks getNuggetsAndShardsTweaks() {
        return nuggetsAndShardsTweaks;
    }

    public SaltTweaks getSaltTweaks() {
        return saltTweaks;
    }

    @Override
    public void buildConfig(ModConfigSpec.Builder builder) {
        tungstenTweaks.buildConfig(builder);
        nuggetsAndShardsTweaks.buildConfig(builder);
        saltTweaks.buildConfig(builder);
    }

    @Override
    public void onConstruct(IEventBus modBus) {
        modBus.addListener(this::onModifyDefaultComponents);
    }

    private void onModifyDefaultComponents(net.neoforged.neoforge.event.ModifyDefaultComponentsEvent event) {
        if (com.onetoys.registry.OnetoysItems.DUMBBELL != null && com.onetoys.registry.OnetoysItems.DUMBBELL.isBound()) {
            event.modify(com.onetoys.registry.OnetoysItems.DUMBBELL.get(), builder -> {
                builder.set(
                        net.minecraft.core.component.DataComponents.ATTRIBUTE_MODIFIERS,
                        com.onetoys.item.DumbbellItem.createAttributes(tungstenTweaks.getDumbbellGravity())
                );
            });
        }
        if (com.onetoys.registry.OnetoysItems.WATERING_CAN != null && com.onetoys.registry.OnetoysItems.WATERING_CAN.isBound()) {
            event.modify(com.onetoys.registry.OnetoysItems.WATERING_CAN.get(), builder -> {
                builder.set(
                        com.onetoys.registry.OnetoysDataComponents.WATERING_CAN_USES.get(),
                        0
                );
            });
        }
    }

    @Override
    public void onCommonSetup(FMLCommonSetupEvent event) {
    }

    @Override
    public void onClientSetup(FMLClientSetupEvent event) {
    }
}