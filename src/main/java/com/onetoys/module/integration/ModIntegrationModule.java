package com.onetoys.module.integration;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.onetoys.Onetoys;
import com.onetoys.mixin.HolderSetNamedAccessor;
import com.onetoys.module.ModuleCategory;
import com.onetoys.module.ModuleManager;
import com.onetoys.module.OnetoysModule;
import com.onetoys.module.integration.caverns_and_chasms.CavernsAndChasmsIntegration;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.TagsUpdatedEvent;

public class ModIntegrationModule extends OnetoysModule {
    public static final String ID = "mod_integration";

    public static final TagKey<Block> INCORRECT_FOR_COPPER_TOOL_TAG =
            TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath("caverns_and_chasms", "incorrect_for_copper_tool"));

    private static ModIntegrationModule instance;
    private final CavernsAndChasmsIntegration cavernsAndChasmsIntegration = new CavernsAndChasmsIntegration(this);

    public ModIntegrationModule() {
        super(ID, "Mod Integration", "Integrations and compatibility tweaks with other mods.", ModuleCategory.MOD_INTEGRATION, true);
        instance = this;
    }

    public static ModIntegrationModule get() {
        return instance;
    }

    public CavernsAndChasmsIntegration getCavernsAndChasmsIntegration() {
        return cavernsAndChasmsIntegration;
    }

    @Override
    public void buildConfig(ModConfigSpec.Builder builder) {
        cavernsAndChasmsIntegration.buildConfig(builder);
    }

    @Override
    public void onConstruct(IEventBus modBus) {
        NeoForge.EVENT_BUS.addListener(this::onTagsUpdated);
        modBus.addListener(this::onModConfigEvent);
    }

    @Override
    public void onCommonSetup(FMLCommonSetupEvent event) {
        if (isEnabled()) {
            event.enqueueWork(() -> updateRuntimeCopperTag(isBalancedCopperGearActive()));
        }
    }

    private void onTagsUpdated(TagsUpdatedEvent event) {
        if (isEnabled()) {
            updateRuntimeCopperTag(isBalancedCopperGearActive());
        }
    }

    private void onModConfigEvent(ModConfigEvent event) {
        if (event.getConfig().getModId().equals(Onetoys.MODID)) {
            updateRuntimeCopperTag(isBalancedCopperGearActive());
        }
    }

    public static boolean isBalancedCopperGearActive() {
        return ModuleManager.get().getModule(ModIntegrationModule.class)
                .map(m -> m.isEnabled() && m.getCavernsAndChasmsIntegration().isBalancedCopperGearEnabled())
                .orElse(false);
    }

    /**
     * Dynamically updates the in-memory caverns_and_chasms:incorrect_for_copper_tool tag
     * in BuiltInRegistries.BLOCK to match stone tier (#minecraft:incorrect_for_stone_tool)
     * when balanced copper gear is active, or iron tier (#minecraft:incorrect_for_iron_tool)
     * when inactive.
     */
    @SuppressWarnings("unchecked")
    public static synchronized void updateRuntimeCopperTag(boolean balanced) {
        if (!ModList.get().isLoaded("caverns_and_chasms")) {
            return;
        }

        try {
            Optional<HolderSet.Named<Block>> copperTagOpt = BuiltInRegistries.BLOCK.getTag(INCORRECT_FOR_COPPER_TOOL_TAG);
            if (copperTagOpt.isEmpty()) {
                return;
            }

            TagKey<Block> targetTierTag = balanced ? BlockTags.INCORRECT_FOR_STONE_TOOL : BlockTags.INCORRECT_FOR_IRON_TOOL;
            Optional<HolderSet.Named<Block>> targetTagOpt = BuiltInRegistries.BLOCK.getTag(targetTierTag);
            if (targetTagOpt.isEmpty()) {
                return;
            }

            HolderSet.Named<Block> copperNamedSet = copperTagOpt.get();
            List<Holder<Block>> targetBlocks = new ArrayList<>(targetTagOpt.get().stream().toList());

            ((HolderSetNamedAccessor<Block>) (Object) copperNamedSet).Onetoys$bind(targetBlocks);
            Onetoys.LOGGER.debug("Dynamic Tag: Updated caverns_and_chasms:incorrect_for_copper_tool to match {} ({} blocks).",
                    targetTierTag.location(), targetBlocks.size());
        } catch (Exception e) {
            Onetoys.LOGGER.debug("Could not update runtime copper tool tag: {}", e.getMessage());
        }
    }
}
