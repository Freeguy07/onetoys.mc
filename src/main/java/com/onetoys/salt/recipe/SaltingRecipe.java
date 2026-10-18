package com.onetoys.salt.recipe;

import com.onetoys.salt.SaltRecipeSerializers;
import com.onetoys.salt.SaltTags;
import com.onetoys.salt.Salting;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import org.jetbrains.annotations.NotNull;

public class SaltingRecipe extends SingleSeasoningRecipe {
    public static final MapCodec<SaltingRecipe> CODEC = makeCodec(
            Ingredient.of(SaltTags.Items.COMMON_SALTS),
            SaltingRecipe::new
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, SaltingRecipe> STREAM_CODEC = makeStreamCodec(
            SaltingRecipe::new
    );

    public SaltingRecipe(CraftingBookCategory category, NonNullList<Ingredient> ingredients) {
        super(category, ingredients);
    }

    public static boolean isSaltable(ItemStack stack) {
        return !Salting.isSalted(stack) && FoodSeasoningRecipe.isSeasonableFood(stack);
    }

    public boolean isSalt(ItemStack stack) {
        return FoodSeasoningRecipe.isSalt(stack) || testIngredients(stack);
    }

    @Override
    protected boolean isSeasoning(ItemStack stack) {
        return isSalt(stack);
    }

    @Override
    protected boolean isSeasonable(ItemStack stack) {
        return isSaltable(stack);
    }

    @Override
    protected ItemStack applySeasoning(ItemStack stack) {
        return Salting.setSalted(stack);
    }

    @Override
    public @NotNull RecipeSerializer<?> getSerializer() {
        return SaltRecipeSerializers.SALTING.get();
    }

    public static class Serializer implements RecipeSerializer<SaltingRecipe> {
        @Override
        public @NotNull MapCodec<SaltingRecipe> codec() {
            return SaltingRecipe.CODEC;
        }

        @Override
        public @NotNull StreamCodec<RegistryFriendlyByteBuf, SaltingRecipe> streamCodec() {
            return SaltingRecipe.STREAM_CODEC;
        }
    }
}
