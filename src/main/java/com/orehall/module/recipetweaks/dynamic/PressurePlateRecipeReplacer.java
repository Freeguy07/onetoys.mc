package com.orehall.module.recipetweaks.dynamic;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.orehall.Orehall;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapedRecipePattern;

public class PressurePlateRecipeReplacer {

    public static class ReplacementResult {
        public final Set<ResourceLocation> recipesToRemove = new HashSet<>();
        public final List<RecipeHolder<?>> recipesToAdd = new ArrayList<>();
    }

    public static ReplacementResult process(Iterable<RecipeHolder<?>> recipes, int outputCount, HolderLookup.Provider registries) {
        ReplacementResult result = new ReplacementResult();
        Ingredient redstoneIngredient = Ingredient.of(Items.REDSTONE);

        for (RecipeHolder<?> holder : recipes) {
            if (!(holder.value() instanceof CraftingRecipe recipe)) {
                continue;
            }

            ItemStack resultStack = recipe.getResultItem(registries);
            if (resultStack.isEmpty()) {
                continue;
            }

            Item item = resultStack.getItem();
            String path = BuiltInRegistries.ITEM.getKey(item).getPath();
            boolean isPressurePlate = resultStack.is(ItemTags.WOODEN_PRESSURE_PLATES) || path.endsWith("_pressure_plate");
            if (!isPressurePlate) {
                continue;
            }

            // Filter out empty ingredients (e.g. from shaped grid empty spaces)
            List<Ingredient> nonEmpties = recipe.getIngredients().stream()
                    .filter(ing -> !ing.isEmpty())
                    .toList();

            // Standard pressure plate recipes require 2 ingredients of the same material
            if (nonEmpties.size() != 2) {
                continue;
            }

            // Do not replace recipes that already require redstone
            if (nonEmpties.stream().anyMatch(ing -> ing.test(Items.REDSTONE.getDefaultInstance()))) {
                continue;
            }

            Ingredient materialIngredient = nonEmpties.get(0);
            if (materialIngredient.isEmpty()) {
                continue;
            }

            // Build new shaped recipe: "###" on top, " R " on bottom
            ShapedRecipePattern pattern = ShapedRecipePattern.of(
                    Map.of(
                            '#', materialIngredient,
                            'R', redstoneIngredient
                    ),
                    "###", " R "
            );

            ItemStack newOutput = resultStack.copyWithCount(outputCount);
            String group = recipe.getGroup().isEmpty() ? "pressure_plates" : recipe.getGroup();
            CraftingBookCategory category = recipe.category();

            ShapedRecipe newRecipe = new ShapedRecipe(
                    group,
                    category,
                    pattern,
                    newOutput
            );

            ResourceLocation newId = ResourceLocation.fromNamespaceAndPath(
                    Orehall.MODID,
                    "dynamic/reliable_" + holder.id().getNamespace() + "_" + holder.id().getPath()
            );

            result.recipesToRemove.add(holder.id());
            result.recipesToAdd.add(new RecipeHolder<>(newId, newRecipe));
        }

        return result;
    }
}
