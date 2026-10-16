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

import com.onetoys.module.recipetweaks.RecipeTweaksModule;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.tags.TagEntry;
import net.minecraft.tags.TagLoader;

@Mixin(TagLoader.class)
public class TagLoaderMixin {
    private static final String ITEM_TAGS_DIR = Registries.tagsDirPath(Registries.ITEM);
    private static final ResourceLocation STONE_TOOL_MATERIALS_ID = ResourceLocation.withDefaultNamespace("stone_tool_materials");
    private static final ResourceLocation FLINT_ID = ResourceLocation.withDefaultNamespace("flint");

    @Shadow
    @Final
    private String directory;

    @Inject(method = "load", at = @At("RETURN"))
    private void Onetoys$injectDynamicTags(ResourceManager resourceManager, CallbackInfoReturnable<Map<ResourceLocation, List<TagLoader.EntryWithSource>>> cir) {
        if (ITEM_TAGS_DIR.equals(this.directory) || "tags/item".equals(this.directory)) {
            if (RecipeTweaksModule.isFlintInStoneToolMaterialsActive()) {
                Map<ResourceLocation, List<TagLoader.EntryWithSource>> map = cir.getReturnValue();
                List<TagLoader.EntryWithSource> entries = map.computeIfAbsent(STONE_TOOL_MATERIALS_ID, k -> new ArrayList<>());
                boolean alreadyPresent = entries.stream().anyMatch(e -> !e.remove() && FLINT_ID.equals(e.entry().getId()));
                if (!alreadyPresent) {
                    entries.add(new TagLoader.EntryWithSource(TagEntry.element(FLINT_ID), "onetoys"));
                }
            }
        }
    }
}

