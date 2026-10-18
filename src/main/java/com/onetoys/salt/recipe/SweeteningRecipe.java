package com.onetoys.salt.recipe;

import com.onetoys.salt.SaltRecipeSerializers;
import com.onetoys.salt.Sweetening;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import org.jetbrains.annotations.NotNull;

public class SweeteningRecipe extends SingleSeasoningRecipe {
    public static final TagKey<Item> COMMON_SUGARS = FoodSeasoningRecipe.COMMON_SUGARS;
    public static final TagKey<Item> FORGE_SUGARS = FoodSeasoningRecipe.FORGE_SUGARS;

    public static final MapCodec<SweeteningRecipe> CODEC = makeCodec(
            Ingredient.of(Items.SUGAR),
            SweeteningRecipe::new
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, SweeteningRecipe> STREAM_CODEC = makeStreamCodec(
            SweeteningRecipe::new
    );

    public SweeteningRecipe(CraftingBookCategory category, NonNullList<Ingredient> ingredients) {
        super(category, ingredients);
    }

    public static boolean isSweetenable(ItemStack stack) {
        return !Sweetening.isSweetened(stack) && FoodSeasoningRecipe.isSeasonableFood(stack);
    }

    public boolean isSugar(ItemStack stack) {
        return FoodSeasoningRecipe.isSugar(stack) || testIngredients(stack);
    }

    @Override
    protected boolean isSeasoning(ItemStack stack) {
        return isSugar(stack);
    }

    @Override
    protected boolean isSeasonable(ItemStack stack) {
        return isSweetenable(stack);
    }

    @Override
    protected ItemStack applySeasoning(ItemStack stack) {
        return Sweetening.setSweetened(stack);
    }

    @Override
    public @NotNull RecipeSerializer<?> getSerializer() {
        return SaltRecipeSerializers.SWEETENING.get();
    }

    public static class Serializer implements RecipeSerializer<SweeteningRecipe> {
        @Override
        public @NotNull MapCodec<SweeteningRecipe> codec() {
            return SweeteningRecipe.CODEC;
        }

        @Override
        public @NotNull StreamCodec<RegistryFriendlyByteBuf, SweeteningRecipe> streamCodec() {
            return SweeteningRecipe.STREAM_CODEC;
        }
    }
}
