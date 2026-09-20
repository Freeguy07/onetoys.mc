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
import com.orehall.module.gameplay.GameplayTweaksModule;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.ReloadableServerResources;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
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
            boolean recipeTweaksActive = module != null && module.isEnabled();
            boolean disableCharcoalActive = GameplayTweaksModule.isDisableCharcoal();

            if (!recipeTweaksActive && !disableCharcoalActive) {
                return;
            }

            List<RecipeHolder<?>> currentRecipes = List.copyOf(recipeManager.getRecipes());
            Set<ResourceLocation> recipesToRemove = new HashSet<>();

            if (recipeTweaksActive) {
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
            }

            // 2. Charcoal Disabler from Item Tweaks
            if (disableCharcoalActive) {
                int charcoalRecipesCount = 0;
                for (RecipeHolder<?> holder : currentRecipes) {
                    if (holder.id().equals(ResourceLocation.withDefaultNamespace("charcoal"))) {
                        if (recipesToRemove.add(holder.id())) {
                            charcoalRecipesCount++;
                        }
                    } else {
                        try {
                            ItemStack result = holder.value().getResultItem(serverResources.getRegistryLookup());
                            if (result.is(Items.CHARCOAL)) {
                                if (recipesToRemove.add(holder.id())) {
                                    charcoalRecipesCount++;
                                }
                            }
                        } catch (Exception ignored) {
                        }
                    }
                }
                if (charcoalRecipesCount > 0) {
                    Orehall.LOGGER.info("Item Tweaks: Disabled {} charcoal producing recipe(s).", charcoalRecipesCount);
                }
            }

            if (recipesToRemove.isEmpty()) {
                return;
            }

            // Filter out disabled recipes in one single atomic operation
            List<RecipeHolder<?>> updatedRecipes = new ArrayList<>(currentRecipes.size() - recipesToRemove.size());
            for (RecipeHolder<?> holder : currentRecipes) {
                if (!recipesToRemove.contains(holder.id())) {
                    updatedRecipes.add(holder);
                }
            }

            recipeManager.replaceRecipes(updatedRecipes);
            Orehall.LOGGER.info("Recipe Tweaks: RecipeManager updated with {} recipes ({} removed).",
                    updatedRecipes.size(), recipesToRemove.size());
        }, gameExecutor);
    }
}
