package com.onetoys.module.gameplay;

import com.onetoys.BarkItem;
import com.onetoys.module.ModuleCategory;
import com.onetoys.module.ModuleManager;
import com.onetoys.module.OnetoysModule;
import com.onetoys.module.gameplay.durability.DurabilityTweaks;
import com.onetoys.module.gameplay.items.ItemTweaks;
import com.onetoys.module.gameplay.mining.MiningTweaks;
import com.onetoys.module.gameplay.mobs.MobTweaks;
import com.onetoys.module.gameplay.sleep.SleepingTweaks;
import com.onetoys.module.gameplay.stack.StackTweaks;
import com.onetoys.module.gameplay.weather.HeightWeatherTweaks;
import com.onetoys.module.gameplay.wetness.WetnessTweaks;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.ModifyDefaultComponentsEvent;
import net.neoforged.neoforge.event.TagsUpdatedEvent;

public class GameplayTweaksModule extends OnetoysModule {
    public static final String ID = "gameplay_tweaks";

    private static GameplayTweaksModule instance;

    private final DurabilityTweaks durabilityTweaks = new DurabilityTweaks();
    private final MobTweaks mobTweaks = new MobTweaks(this);
    private final ItemTweaks itemTweaks = new ItemTweaks(this);
    private final MiningTweaks miningTweaks = new MiningTweaks();
    private final SleepingTweaks sleepingTweaks = new SleepingTweaks();
    private final StackTweaks stackTweaks = new StackTweaks();
    private final WetnessTweaks wetnessTweaks = new WetnessTweaks(this);
    private final FoodOverhaulTweaks foodOverhaulTweaks = new FoodOverhaulTweaks(this);
    private final HeightWeatherTweaks heightWeatherTweaks = new HeightWeatherTweaks(this);

    public GameplayTweaksModule() {
        super(ID, "Gameplay Tweaks", "Tweaks and adjustments to core gameplay mechanics.", ModuleCategory.GAMEPLAY, true);
        instance = this;
    }

    @Override
    public void buildConfig(ModConfigSpec.Builder builder) {
        durabilityTweaks.buildConfig(builder);
        mobTweaks.buildConfig(builder);
        itemTweaks.buildConfig(builder);
        miningTweaks.buildConfig(builder);
        sleepingTweaks.buildConfig(builder);
        stackTweaks.buildConfig(builder);
        wetnessTweaks.buildConfig(builder);
        foodOverhaulTweaks.buildConfig(builder);
        heightWeatherTweaks.buildConfig(builder);
    }

    @Override
    public void onConstruct(IEventBus modBus) {
        modBus.addListener(this::onModifyDefaultComponents);
        NeoForge.EVENT_BUS.addListener(this::onTagsUpdated);
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

    private void onTagsUpdated(TagsUpdatedEvent event) {
        BarkItem.invalidateCache();
        if (isEnabled()) {
            durabilityTweaks.applyTagOverrides();
            stackTweaks.applyTagOverrides();
        }
    }

    @Override
    public void onCommonSetup(FMLCommonSetupEvent event) {
        sleepingTweaks.onCommonSetup(event);
    }

    @Override
    public void onClientSetup(FMLClientSetupEvent event) {
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

    public MiningTweaks getMiningTweaks() {
        return miningTweaks;
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

    public FoodOverhaulTweaks getFoodOverhaulTweaks() {
        return foodOverhaulTweaks;
    }

    public HeightWeatherTweaks getHeightWeatherTweaks() {
        return heightWeatherTweaks;
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

    public static boolean isColdWaterFreezingEnabled() {
        return isWetnessEnabled() && WetnessTweaks.isColdWaterFreezingActive();
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

    public static boolean isSwordBlockingEnabled() {
        GameplayTweaksModule mod = instance;
        if (mod == null) {
            mod = ModuleManager.get().getModule(GameplayTweaksModule.class).orElse(null);
        }
        if (mod == null || !mod.isEnabled()) {
            return false;
        }
        ItemTweaks itemTweaks = mod.getItemTweaks();
        return itemTweaks != null && itemTweaks.isSwordBlockingEnabled();
    }

    public static boolean isSpearEnabled() {
        GameplayTweaksModule mod = instance;
        if (mod == null) {
            mod = ModuleManager.get().getModule(GameplayTweaksModule.class).orElse(null);
        }
        if (mod == null || !mod.isEnabled()) {
            return false;
        }
        ItemTweaks itemTweaks = mod.getItemTweaks();
        return itemTweaks != null && itemTweaks.isSpearEnabled();
    }

    public static double getSpearMobSpawnChance() {
        GameplayTweaksModule mod = instance;
        if (mod == null) {
            mod = ModuleManager.get().getModule(GameplayTweaksModule.class).orElse(null);
        }
        if (mod == null || !mod.isEnabled()) {
            return 0.0D;
        }
        ItemTweaks itemTweaks = mod.getItemTweaks();
        return itemTweaks != null ? itemTweaks.getSpearTweaks().getMobSpawnChance() : 0.0D;
    }

    public static boolean isFoodOverhaulEnabled() {
        GameplayTweaksModule mod = instance;
        if (mod == null) {
            mod = ModuleManager.get().getModule(GameplayTweaksModule.class).orElse(null);
        }
        if (mod == null || !mod.isEnabled()) {
            return false;
        }
        return mod.getFoodOverhaulTweaks().isEnabled();
    }
}
