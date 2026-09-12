package com.orehall.module.gameplay;

import com.orehall.module.ModuleCategory;
import com.orehall.module.OrehallModule;
import com.orehall.module.gameplay.durability.DurabilityTweaks;
import com.orehall.module.gameplay.items.ItemTweaks;
import com.orehall.module.gameplay.mobs.MobTweaks;
import com.orehall.module.gameplay.sleep.SleepingTweaks;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.event.ModifyDefaultComponentsEvent;

public class GameplayTweaksModule extends OrehallModule {
    public static final String ID = "gameplay_tweaks";

    private final DurabilityTweaks durabilityTweaks = new DurabilityTweaks();
    private final MobTweaks mobTweaks = new MobTweaks();
    private final ItemTweaks itemTweaks = new ItemTweaks();
    private final SleepingTweaks sleepingTweaks = new SleepingTweaks();

    public GameplayTweaksModule() {
        super(ID, "Gameplay Tweaks", "Tweaks and adjustments to core gameplay mechanics.", ModuleCategory.GAMEPLAY, true);
    }

    @Override
    public void buildConfig(ModConfigSpec.Builder builder) {
        durabilityTweaks.buildConfig(builder);
        mobTweaks.buildConfig(builder);
        itemTweaks.buildConfig(builder);
        sleepingTweaks.buildConfig(builder);
    }

    @Override
    public void onConstruct(IEventBus modBus) {
        modBus.addListener(this::onModifyDefaultComponents);
        mobTweaks.onConstruct(modBus);
        itemTweaks.onConstruct(modBus);
        sleepingTweaks.onConstruct(modBus);
    }

    private void onModifyDefaultComponents(ModifyDefaultComponentsEvent event) {
        if (isEnabled()) {
            durabilityTweaks.apply(event);
        }
    }

    @Override
    public void onCommonSetup(net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent event) {
        if (isEnabled()) {
            sleepingTweaks.onCommonSetup(event);
        }
    }

    @Override
    public void onClientSetup(net.neoforged.fml.event.lifecycle.FMLClientSetupEvent event) {
        if (isEnabled()) {
            itemTweaks.onClientSetup(event);
        }
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
}
