package com.onetoys.salt;

import com.onetoys.salt.recipe.FoodSeasoningRecipe;
import com.onetoys.salt.recipe.SaltingRecipe;
import com.onetoys.salt.recipe.SatiatingRecipe;
import com.onetoys.salt.recipe.SweeteningRecipe;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class SaltRecipeSerializers {
    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS =
            DeferredRegister.create(BuiltInRegistries.RECIPE_SERIALIZER, Salt.ID);

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<SaltingRecipe>> SALTING =
            RECIPE_SERIALIZERS.register("salting", SaltingRecipe.Serializer::new);

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<SweeteningRecipe>> SWEETENING =
            RECIPE_SERIALIZERS.register("sweetening", SweeteningRecipe.Serializer::new);

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<SatiatingRecipe>> SATIATING =
            RECIPE_SERIALIZERS.register("satiating", SatiatingRecipe.Serializer::new);

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<FoodSeasoningRecipe>> FOOD_SEASONING =
            RECIPE_SERIALIZERS.register("food_seasoning", FoodSeasoningRecipe.Serializer::new);
}

