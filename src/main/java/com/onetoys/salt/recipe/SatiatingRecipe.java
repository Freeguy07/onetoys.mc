package com.onetoys.salt.recipe;

import com.onetoys.Onetoys;
import com.onetoys.salt.SaltRecipeSerializers;
import com.onetoys.salt.Satiating;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import org.jetbrains.annotations.NotNull;

public class SatiatingRecipe extends SingleSeasoningRecipe {
    public static final MapCodec<SatiatingRecipe> CODEC = makeCodec(
            Ingredient.of(Onetoys.CHRYSANTHEMUM),
            SatiatingRecipe::new
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, SatiatingRecipe> STREAM_CODEC = makeStreamCodec(
            SatiatingRecipe::new
    );

    public SatiatingRecipe(CraftingBookCategory category, NonNullList<Ingredient> ingredients) {
        super(category, ingredients);
    }

    public static boolean isSatiable(ItemStack stack) {
        return !Satiating.isSatiating(stack) && FoodSeasoningRecipe.isSeasonableFood(stack);
    }

    public boolean isChrysanthemum(ItemStack stack) {
        return FoodSeasoningRecipe.isChrysanthemum(stack) || testIngredients(stack);
    }

    @Override
    protected boolean isSeasoning(ItemStack stack) {
        return isChrysanthemum(stack);
    }

    @Override
    protected boolean isSeasonable(ItemStack stack) {
        return isSatiable(stack);
    }

    @Override
    protected ItemStack applySeasoning(ItemStack stack) {
        return Satiating.setSatiating(stack);
    }

    @Override
    public @NotNull RecipeSerializer<?> getSerializer() {
        return SaltRecipeSerializers.SATIATING.get();
    }

    public static class Serializer implements RecipeSerializer<SatiatingRecipe> {
        @Override
        public @NotNull MapCodec<SatiatingRecipe> codec() {
            return SatiatingRecipe.CODEC;
        }

        @Override
        public @NotNull StreamCodec<RegistryFriendlyByteBuf, SatiatingRecipe> streamCodec() {
            return SatiatingRecipe.STREAM_CODEC;
        }
    }
}
