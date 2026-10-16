package com.onetoys.module.gameplay;

import com.onetoys.module.ModuleCategory;
import com.onetoys.module.ModuleManager;
import com.onetoys.module.OnetoysModule;
import com.onetoys.module.gameplay.durability.DurabilityTweaks;
import com.onetoys.module.gameplay.items.ItemTweaks;
import com.onetoys.module.gameplay.mobs.MobTweaks;
import com.onetoys.module.gameplay.sleep.SleepingTweaks;
import com.onetoys.module.gameplay.stack.StackTweaks;
import com.onetoys.module.gameplay.wetness.WetnessTweaks;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.event.ModifyDefaultComponentsEvent;

public class GameplayTweaksModule extends OnetoysModule {
    public static final String ID = "gameplay_tweaks";

    private static GameplayTweaksModule instance;

    private final DurabilityTweaks durabilityTweaks = new DurabilityTweaks();
    private final MobTweaks mobTweaks = new MobTweaks();
    private final ItemTweaks itemTweaks = new ItemTweaks();
    private final SleepingTweaks sleepingTweaks = new SleepingTweaks();
    private final StackTweaks stackTweaks = new StackTweaks();
    private final WetnessTweaks wetnessTweaks = new WetnessTweaks(this);

    public GameplayTweaksModule() {
        super(ID, "Gameplay Tweaks", "Tweaks and adjustments to core gameplay mechanics.", ModuleCategory.GAMEPLAY, true);
        instance = this;
    }

    @Override
    public void buildConfig(ModConfigSpec.Builder builder) {
        durabilityTweaks.buildConfig(builder);
        mobTweaks.buildConfig(builder);
        itemTweaks.buildConfig(builder);
        sleepingTweaks.buildConfig(builder);
        stackTweaks.buildConfig(builder);
        wetnessTweaks.buildConfig(builder);
    }

    @Override
    public void onConstruct(IEventBus modBus) {
        modBus.addListener(this::onModifyDefaultComponents);
        mobTweaks.onConstruct(modBus);
        itemTweaks.onConstruct(modBus);
        sleepingTweaks.onConstruct(modBus);
        wetnessTweaks.onConstruct(modBus);
    }

    private void onModifyDefaultComponents(ModifyDefaultComponentsEvent event) {
        event.modify(com.onetoys.registry.OnetoysItems.STOPWATCH.get(), builder ->
                builder.set(com.onetoys.registry.OnetoysDataComponents.STOPWATCH.get(), com.onetoys.item.StopwatchData.DEFAULT));

        if (isEnabled()) {
            durabilityTweaks.apply(event);
            stackTweaks.apply(event);
        }
    }

    @Override
    public void onCommonSetup(FMLCommonSetupEvent event) {
        sleepingTweaks.onCommonSetup(event);
        wetnessTweaks.onCommonSetup(event);
    }

    @Override
    public void onClientSetup(FMLClientSetupEvent event) {
        itemTweaks.onClientSetup(event);
        wetnessTweaks.onClientSetup(event);
    }

    public static GameplayTweaksModule getInstance() {
        return instance;
    }

    public DurabilityTweaks getDurabilityTweaks() {
        return durabilityTweaks;
    }

    public MobTweaks getMobTweaks() {
        return mobTweaks;
    }

    public ItemTweaks getItemTweaks() {
        return itemTweaks;
    }

    public SleepingTweaks getSleepingTweaks() {
        return sleepingTweaks;
    }

    public StackTweaks getStackTweaks() {
        return stackTweaks;
    }

    public WetnessTweaks getWetnessTweaks() {
        return wetnessTweaks;
    }

    public static boolean isWetnessEnabled() {
        GameplayTweaksModule mod = instance;
        if (mod == null) {
            mod = ModuleManager.get().getModule(GameplayTweaksModule.class).orElse(null);
        }
        if (mod == null || !mod.isEnabled()) {
            return false;
        }
        WetnessTweaks wetness = mod.getWetnessTweaks();
        return wetness != null && wetness.isEnabled();
    }

    public static boolean isStableFarmlandEnabled() {
        return WetnessTweaks.isStableFarmlandEnabled();
    }

    public static boolean isDisableCharcoal() {
        GameplayTweaksModule mod = instance;
        if (mod == null) {
            mod = ModuleManager.get().getModule(GameplayTweaksModule.class).orElse(null);
        }
        if (mod == null || !mod.isEnabled()) {
            return false;
        }
        ItemTweaks itemTweaks = mod.getItemTweaks();
        return itemTweaks != null && itemTweaks.isDisableCharcoal();
    }

    public static boolean isMattedWoolFromWetSheepEnabled() {
        return isWetnessEnabled() && WetnessTweaks.isMattedWoolFromWetSheep();
    }

    public static boolean isStopwatchEnabled() {
        GameplayTweaksModule mod = instance;
        if (mod == null) {
            mod = ModuleManager.get().getModule(GameplayTweaksModule.class).orElse(null);
        }
        if (mod == null || !mod.isEnabled()) {
            return false;
        }
        ItemTweaks itemTweaks = mod.getItemTweaks();
        return itemTweaks != null && itemTweaks.isStopwatchEnabled();
    }

    public static boolean isStopwatchHotbarDisplayEnabled() {
        GameplayTweaksModule mod = instance;
        if (mod == null) {
            mod = ModuleManager.get().getModule(GameplayTweaksModule.class).orElse(null);
        }
        if (mod == null || !mod.isEnabled()) {
            return false;
        }
        ItemTweaks itemTweaks = mod.getItemTweaks();
        return itemTweaks != null && itemTweaks.isStopwatchHotbarDisplayEnabled();
    }
}
