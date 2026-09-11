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
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapedRecipePattern;
import net.minecraft.world.item.crafting.ShapelessRecipe;

public class ButtonRecipeReplacer {

    public static class ReplacementResult {
        public final Set<ResourceLocation> recipesToRemove = new HashSet<>();
        public final List<RecipeHolder<?>> recipesToAdd = new ArrayList<>();
    }

    public static ReplacementResult process(Iterable<RecipeHolder<?>> recipes, int outputCount, HolderLookup.Provider registries) {
        ReplacementResult result = new ReplacementResult();
        Ingredient redstoneIngredient = Ingredient.of(Items.REDSTONE);

        for (RecipeHolder<?> holder : recipes) {
            if (!(holder.value() instanceof ShapelessRecipe shapeless)) {
                continue;
            }

            // Must be a 1-ingredient crafting recipe
            if (shapeless.getIngredients().size() != 1) {
                continue;
            }

            ItemStack resultStack = shapeless.getResultItem(registries);
            if (resultStack.isEmpty() || resultStack.getCount() != 1) {
                continue;
            }

            Item item = resultStack.getItem();
            String path = BuiltInRegistries.ITEM.getKey(item).getPath();
            boolean isButton = resultStack.is(ItemTags.BUTTONS) || path.endsWith("_button");
            if (!isButton) {
                continue;
            }

            Ingredient materialIngredient = shapeless.getIngredients().get(0);
            if (materialIngredient.isEmpty()) {
                continue;
            }

            // Build new shaped recipe: Block on top ('#'), Redstone on bottom ('R')
            ShapedRecipePattern pattern = ShapedRecipePattern.of(
                    Map.of(
                            '#', materialIngredient,
                            'R', redstoneIngredient
                    ),
                    "#", "R"
            );

            ItemStack newOutput = resultStack.copyWithCount(outputCount);
            String group = shapeless.getGroup().isEmpty() ? "buttons" : shapeless.getGroup();
            CraftingBookCategory category = shapeless.category();

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
