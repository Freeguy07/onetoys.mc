package com.onetoys;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;
import com.onetoys.crafting.condition.OnetoysConditions;
import com.onetoys.effect.Hunger2MobEffect;
import com.onetoys.effect.SlowDigestionMobEffect;
import com.onetoys.module.ModuleManager;
import com.onetoys.module.content.ContentModule;
import com.onetoys.module.gameplay.GameplayTweaksModule;
import com.onetoys.registry.OnetoysAttachments;
import com.onetoys.registry.OnetoysBlockEntityTypes;
import com.onetoys.registry.OnetoysBlocks;
import com.onetoys.registry.OnetoysCreativeModeTabs;
import com.onetoys.registry.OnetoysFeatures;
import com.onetoys.registry.OnetoysItems;
import com.onetoys.registry.OnetoysParticles;
import com.onetoys.salt.Salt;
import com.onetoys.sol_valheim.SOLValheim;
import com.onetoys.worldgen.surface.FarAlpha;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemNameBlockItem;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionBrewing;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.ModifyDefaultComponentsEvent;
import net.neoforged.neoforge.event.brewing.RegisterBrewingRecipesEvent;
import net.neoforged.neoforge.event.furnace.FurnaceFuelBurnTimeEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

@Mod(Onetoys.MODID)
public class Onetoys {
    public static final String MODID = "onetoys";
    public static final Logger LOGGER = LogUtils.getLogger();

    public static final DeferredRegister<MobEffect> MOB_EFFECTS = DeferredRegister.create(BuiltInRegistries.MOB_EFFECT, MODID);
    public static final DeferredRegister<Potion> POTIONS = DeferredRegister.create(BuiltInRegistries.POTION, MODID);

    public static final DeferredBlock<BluestoneWireBlock> BLUESTONE_WIRE = OnetoysBlocks.BLOCKS.registerBlock(
            "bluestone_wire",
            BluestoneWireBlock::new,
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_LIGHT_BLUE)
                    .noCollission()
                    .instabreak()
                    .pushReaction(PushReaction.DESTROY)
    );

    public static final FoodProperties BARK_FOOD = new FoodProperties.Builder()
            .nutrition(1)
            .saturationModifier(0.5F)
            .alwaysEdible()
            .fast()
            .build();

    public static final FoodProperties GLOW_LICHEN_FOOD = new FoodProperties.Builder()
            .nutrition(1)
            .saturationModifier(0.5F)
            .alwaysEdible()
            .fast()
            .effect(() -> new MobEffectInstance(MobEffects.GLOWING, 300, 0), 1.0F)
            .build();

    public static final DeferredItem<Item> BARK = OnetoysItems.ITEMS.registerItem(
            "bark",
            BarkItem::new,
            new Item.Properties().food(BARK_FOOD)
    );

    public static final DeferredItem<Item> COPPER_PATINA = OnetoysItems.ITEMS.registerItem(
            "copper_patina",
            CopperPatinaItem::new,
            new Item.Properties()
    );

    public static final DeferredItem<Item> BLUESTONE = OnetoysItems.ITEMS.registerItem(
            "bluestone",
            props -> new ItemNameBlockItem(BLUESTONE_WIRE.get(), props),
            new Item.Properties()
    );

    public static final DeferredItem<Item> CHRYSANTHEMUM = OnetoysItems.ITEMS.registerItem(
            "chrysanthemum",
            Item::new,
            new Item.Properties()
    );

    public static final DeferredHolder<MobEffect, Hunger2MobEffect> HUNGER_2 = MOB_EFFECTS.register(
            "hunger_2",
            () -> new Hunger2MobEffect(MobEffectCategory.HARMFUL, 5797459)
    );

    public static final DeferredHolder<MobEffect, SlowDigestionMobEffect> SLOW_DIGESTION = MOB_EFFECTS.register(
            "slow_digestion",
            () -> new SlowDigestionMobEffect(MobEffectCategory.BENEFICIAL, 0xCAA14A)
    );

    public static final DeferredHolder<Potion, Potion> SLOW_DIGESTION_POTION = POTIONS.register(
            "slow_digestion",
            () -> new Potion(new MobEffectInstance(SLOW_DIGESTION, 3600))
    );

    public static final DeferredHolder<Potion, Potion> LONG_SLOW_DIGESTION_POTION = POTIONS.register(
            "long_slow_digestion",
            () -> new Potion("slow_digestion", new MobEffectInstance(SLOW_DIGESTION, 9600))
    );

    public Onetoys(IEventBus modEventBus, ModContainer modContainer) {
        // Register condition serializers
        OnetoysConditions.register(modEventBus);
        // Register data attachments
        OnetoysAttachments.register(modEventBus);
        // Register data components
        com.onetoys.registry.OnetoysDataComponents.register(modEventBus);
        // Register particles
        OnetoysParticles.register(modEventBus);
        // Register features
        OnetoysFeatures.register(modEventBus);
        com.onetoys.registry.OnetoysDensityFunctions.register(modEventBus);
        // Register blocks, items, block entities, effects, potions, and creative tabs
        OnetoysBlocks.register(modEventBus);
        OnetoysBlockEntityTypes.register(modEventBus);
        OnetoysItems.register(modEventBus);
        MOB_EFFECTS.register(modEventBus);
        POTIONS.register(modEventBus);
        com.onetoys.registry.OnetoysEntityTypes.register(modEventBus);
        com.onetoys.registry.OnetoysSoundEvents.register(modEventBus);
        OnetoysCreativeModeTabs.register(modEventBus);

        Salt.init(modEventBus, modContainer);
        SOLValheim.init(modContainer);
        SOLValheim.ATTACHMENT_TYPES.register(modEventBus);

        // Initialize modules and lifecycle
        ModuleManager.get().init();
        ModuleManager.get().onConstruct(modEventBus);

        // Register mod event bus listeners
        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::addCreative);
        modEventBus.addListener(this::modifyDefaultComponents);
        modEventBus.addListener(this::onModConfigEvent);

        // Register game event bus listeners
        NeoForge.EVENT_BUS.addListener(this::onFurnaceFuelBurnTime);
        NeoForge.EVENT_BUS.addListener(this::registerBrewingRecipes);
        NeoForge.EVENT_BUS.addListener(FarAlpha::onServerAboutToStart);
        NeoForge.EVENT_BUS.addListener(FarAlpha::onServerStopped);

        // Register unified mod configuration
        modContainer.registerConfig(ModConfig.Type.STARTUP, Config.SPEC, "onetoys.toml");
    }

    private void onModConfigEvent(ModConfigEvent event) {
        if (MODID.equals(event.getConfig().getModId())) {
            com.onetoys.sol_valheim.ModConfig.clearCache();
        }
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        LOGGER.info("Onetoys common setup");
        ModuleManager.get().onCommonSetup(event);
        event.enqueueWork(() -> {
            Salt.registerDispenserBehaviors();
            DispenserBlock.registerProjectileBehavior(OnetoysItems.SPEAR.get());
            DispenserBlock.registerProjectileBehavior(OnetoysItems.MATTED_WOOL.get());
            MobEffects.HUNGER.value().addAttributeModifier(
                    Attributes.ATTACK_SPEED,
                    ResourceLocation.fromNamespaceAndPath(MODID, "hunger_attack_speed"),
                    -0.10D,
                    AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
            );
            HUNGER_2.get().addAttributeModifier(
                    Attributes.ATTACK_SPEED,
                    ResourceLocation.fromNamespaceAndPath(MODID, "hunger_2_attack_speed"),
                    -0.10D,
                    AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
            );
        });
    }

    private void registerBrewingRecipes(RegisterBrewingRecipesEvent event) {
        PotionBrewing.Builder builder = event.getBuilder();
        builder.addMix(Potions.SLOWNESS, CHRYSANTHEMUM.get(), SLOW_DIGESTION_POTION);
        builder.addMix(SLOW_DIGESTION_POTION, Items.REDSTONE, LONG_SLOW_DIGESTION_POTION);
    }

    private void modifyDefaultComponents(ModifyDefaultComponentsEvent event) {
        event.modify(Items.GLOW_LICHEN, builder -> builder.set(DataComponents.FOOD, GLOW_LICHEN_FOOD));
    }

    private void addCreative(BuildCreativeModeTabContentsEvent event) {
        Salt.onBuildCreativeTabs(event);
        if (event.getTabKey() == CreativeModeTabs.FOOD_AND_DRINKS) {
            event.accept(BARK);
        }
        if (event.getTabKey() == CreativeModeTabs.INGREDIENTS) {
            event.accept(BARK);
            event.accept(COPPER_PATINA);
            event.accept(CHRYSANTHEMUM);
        }
        if (event.getTabKey() == CreativeModeTabs.REDSTONE_BLOCKS) {
            event.accept(BLUESTONE);
            event.accept(OnetoysBlocks.QUARTZ_GLASS);
        }
        if (event.getTabKey() == CreativeModeTabs.BUILDING_BLOCKS) {
            event.accept(OnetoysBlocks.QUARTZ_GLASS);
        }
        if (event.getTabKey() == CreativeModeTabs.COMBAT) {
            if (GameplayTweaksModule.isSpearEnabled()) {
                event.accept(OnetoysItems.SPEAR);
            }
            if (GameplayTweaksModule.isMacheteEnabled()) {
                event.accept(OnetoysItems.MACHETE);
            }
            if (GameplayTweaksModule.isClubEnabled()) {
                event.accept(OnetoysItems.CLUB);
            }
        }
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES && GameplayTweaksModule.isClubEnabled()) {
            event.accept(OnetoysItems.CLUB);
        }
    }

    private void onFurnaceFuelBurnTime(FurnaceFuelBurnTimeEvent event) {
        if (ContentModule.isNuggetsAndShardsEnabled() && event.getItemStack().is(OnetoysItems.COAL_LUMP.get())) {
            event.setBurnTime(200);
        }
        if (event.getItemStack().is(Items.CHARCOAL) && GameplayTweaksModule.isDisableCharcoal()) {
            event.setBurnTime(0);
        }
    }
}
