package com.orehall.crafting.condition;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.orehall.module.ModuleManager;
import com.orehall.module.recipetweaks.RecipeTweaksModule;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.common.conditions.ICondition;

public record RecipeEnabledCondition(ResourceLocation recipe) implements ICondition {
    public static final MapCodec<RecipeEnabledCondition> CODEC = RecordCodecBuilder.mapCodec(
            builder -> builder.group(
                    ResourceLocation.CODEC.fieldOf("recipe").forGetter(RecipeEnabledCondition::recipe)
            ).apply(builder, RecipeEnabledCondition::new)
    );

    @Override
    public boolean test(IContext context) {
        return ModuleManager.get().getModule(RecipeTweaksModule.class)
                .map(module -> module.isRecipeEnabled(recipe.getPath()))
                .orElse(false);
    }

    @Override
    public MapCodec<? extends ICondition> codec() {
        return CODEC;
    }

    @Override
    public String toString() {
        return "recipe_enabled(\"" + recipe + "\")";
    }
}
