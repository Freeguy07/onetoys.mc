package com.onetoys.crafting.condition;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.onetoys.module.ModuleManager;
import com.onetoys.module.recipetweaks.RecipeTweaksModule;

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
        String path = recipe.getPath();
        if ("stopwatch".equals(path)) {
            return com.onetoys.module.gameplay.GameplayTweaksModule.isStopwatchEnabled();
        }
        if ("dumbbell".equals(path)) {
            return com.onetoys.module.content.ContentModule.isDumbbellEnabled();
        }
        if ("watering_can".equals(path)) {
            return com.onetoys.module.content.ContentModule.isWateringCanEnabled();
        }
        RecipeTweaksModule module = RecipeTweaksModule.get();
        return module != null && module.isRecipeEnabled(path);
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
