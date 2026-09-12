package com.orehall.module.recipetweaks;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import com.orehall.Orehall;
import com.orehall.module.ModuleManager;
import com.orehall.module.recipetweaks.dynamic.CustomRecipeReplacer;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.ReloadableServerResources;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;

public class VanillaRecipeDisablerListener implements PreparableReloadListener {
    private final ReloadableServerResources serverResources;

    public VanillaRecipeDisablerListener(ReloadableServerResources serverResources) {
        this.serverResources = serverResources;
    }

    @Override
    public CompletableFuture<Void> reload(PreparationBarrier stage, ResourceManager resourceManager,
                                          ProfilerFiller preparationsProfiler, ProfilerFiller reloadProfiler,
                                          Executor backgroundExecutor, Executor gameExecutor) {
        return stage.wait(null).thenRunAsync(() -> {
            RecipeManager recipeManager = serverResources.getRecipeManager();
            RecipeTweaksModule module = ModuleManager.get().getModule(RecipeTweaksModule.class).orElse(null);
            if (module == null || !module.isEnabled()) {
                return;
            }

            List<RecipeHolder<?>> currentRecipes = List.copyOf(recipeManager.getRecipes());
            Set<ResourceLocation> recipesToRemove = new HashSet<>();
            List<RecipeHolder<?>> recipesToAdd = new ArrayList<>();

            // 1. Static replacement recipes (reliable_*) from RecipeTweaksModule
            Map<ResourceLocation, ResourceLocation> activeReplacements = module.getActiveReplacements();
            for (Map.Entry<ResourceLocation, ResourceLocation> entry : activeReplacements.entrySet()) {
                ResourceLocation vanillaId = entry.getKey();
                ResourceLocation replacementId = entry.getValue();
                // Confirm replacement recipe is actually loaded
                if (recipeManager.byKey(replacementId).isPresent()) {
                    recipesToRemove.add(vanillaId);
                    Orehall.LOGGER.info("Disabling vanilla recipe '{}' in favor of '{}'", vanillaId, replacementId);
                }
            }

            // 2. Dynamic Replacements (Automatic Serialized Recipe Replacement System)
            if (module.isAutomaticSerializedRecipeReplacementSystemEnabled()) {
                List<String> rawRules = module.getAutomaticSerializedRecipeReplacementRules();
                if (!rawRules.isEmpty()) {
                    CustomRecipeReplacer.ReplacementResult customResult =
                            CustomRecipeReplacer.process(currentRecipes, recipesToRemove, rawRules,
                                    serverResources.getRegistryLookup(), serverResources.getConditionContext());
                    recipesToRemove.addAll(customResult.recipesToRemove);
                    recipesToAdd.addAll(customResult.recipesToAdd);
                    Orehall.LOGGER.info("Automatic Serialized Recipe Replacement System: Replaced {} recipe(s) across {} rule(s).",
                            customResult.recipesToRemove.size(), rawRules.size());
                }
            }

            if (recipesToRemove.isEmpty() && recipesToAdd.isEmpty()) {
                return;
            }

            // Filter out disabled recipes and append dynamically generated recipes in one single atomic operation
            List<RecipeHolder<?>> updatedRecipes = new ArrayList<>(currentRecipes.size() - recipesToRemove.size() + recipesToAdd.size());
            for (RecipeHolder<?> holder : currentRecipes) {
                if (!recipesToRemove.contains(holder.id())) {
                    updatedRecipes.add(holder);
                }
            }
            updatedRecipes.addAll(recipesToAdd);

            recipeManager.replaceRecipes(updatedRecipes);
            Orehall.LOGGER.info("Recipe Tweaks: RecipeManager updated with {} recipes ({} removed, {} added).",
                    updatedRecipes.size(), recipesToRemove.size(), recipesToAdd.size());
        }, gameExecutor);
    }
}
