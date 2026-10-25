package com.onetoys.mixin;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.onetoys.module.integration.ModIntegrationModule;
import com.onetoys.module.recipetweaks.RecipeTweaksModule;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.tags.TagEntry;
import net.minecraft.tags.TagLoader;
import net.neoforged.neoforge.common.Tags;

@Mixin(TagLoader.class)
public class TagLoaderMixin {
    private static final String ITEM_TAGS_DIR = Registries.tagsDirPath(Registries.ITEM);
    private static final String BLOCK_TAGS_DIR = Registries.tagsDirPath(Registries.BLOCK);

    private static final ResourceLocation STONE_TOOL_MATERIALS_ID = ResourceLocation.withDefaultNamespace("stone_tool_materials");
    private static final ResourceLocation FLINT_ID = ResourceLocation.withDefaultNamespace("flint");
    private static final ResourceLocation C_STONES_ID = Tags.Items.STONES.location();

    private static final ResourceLocation INCORRECT_FOR_COPPER_TOOL_ID =
            ResourceLocation.fromNamespaceAndPath("caverns_and_chasms", "incorrect_for_copper_tool");
    private static final ResourceLocation INCORRECT_FOR_IRON_TOOL_ID = ResourceLocation.withDefaultNamespace("incorrect_for_iron_tool");
    private static final ResourceLocation INCORRECT_FOR_STONE_TOOL_ID = ResourceLocation.withDefaultNamespace("incorrect_for_stone_tool");

    @Shadow
    @Final
    private String directory;

    @Inject(method = "load", at = @At("RETURN"))
    private void Onetoys$injectDynamicTags(ResourceManager resourceManager, CallbackInfoReturnable<Map<ResourceLocation, List<TagLoader.EntryWithSource>>> cir) {
        if (ITEM_TAGS_DIR.equals(this.directory) || "tags/item".equals(this.directory)) {
            if (RecipeTweaksModule.isHandyStoneMaterialsActive()) {
                Map<ResourceLocation, List<TagLoader.EntryWithSource>> map = cir.getReturnValue();
                List<TagLoader.EntryWithSource> entries = map.computeIfAbsent(STONE_TOOL_MATERIALS_ID, k -> new ArrayList<>());

                boolean flintAlreadyPresent = entries.stream().anyMatch(e -> !e.remove() && !e.entry().isTag() && FLINT_ID.equals(e.entry().getId()));
                if (!flintAlreadyPresent) {
                    entries.add(new TagLoader.EntryWithSource(TagEntry.element(FLINT_ID), "onetoys"));
                }

                boolean stonesAlreadyPresent = entries.stream().anyMatch(e -> !e.remove() && e.entry().isTag() && C_STONES_ID.equals(e.entry().getId()));
                if (!stonesAlreadyPresent) {
                    entries.add(new TagLoader.EntryWithSource(TagEntry.optionalTag(C_STONES_ID), "onetoys"));
                }
            }
        } else if (BLOCK_TAGS_DIR.equals(this.directory) || "tags/block".equals(this.directory)) {
            if (ModIntegrationModule.isBalancedCopperGearActive()) {
                Map<ResourceLocation, List<TagLoader.EntryWithSource>> map = cir.getReturnValue();
                List<TagLoader.EntryWithSource> entries = map.computeIfAbsent(INCORRECT_FOR_COPPER_TOOL_ID, k -> new ArrayList<>());

                // Remove incorrect_for_iron_tool references so copper tools no longer mine iron tier blocks
                entries.removeIf(e -> e.entry().isTag() && INCORRECT_FOR_IRON_TOOL_ID.equals(e.entry().getId()));

                // Add incorrect_for_stone_tool so copper tools mine only what stone tools can
                boolean stonePresent = entries.stream().anyMatch(e -> !e.remove() && e.entry().isTag() && INCORRECT_FOR_STONE_TOOL_ID.equals(e.entry().getId()));
                if (!stonePresent) {
                    entries.add(new TagLoader.EntryWithSource(TagEntry.optionalTag(INCORRECT_FOR_STONE_TOOL_ID), "onetoys"));
                }
            }
        }
    }
}
