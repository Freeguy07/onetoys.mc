package com.onetoys.salt.recipe;

import com.onetoys.salt.SaltRecipeSerializers;
import com.onetoys.salt.SaltTags;
import com.onetoys.salt.Sweetening;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class SweeteningRecipe extends CustomRecipe {
    public static final TagKey<Item> COMMON_SUGARS = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("c", "sugars"));
    public static final TagKey<Item> FORGE_SUGARS = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("forge", "sugars"));

    public static final MapCodec<SweeteningRecipe> CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
            CraftingBookCategory.CODEC.optionalFieldOf("category", CraftingBookCategory.MISC).forGetter(CustomRecipe::category),
            Ingredient.CODEC_NONEMPTY.listOf().optionalFieldOf("ingredients", List.of(Ingredient.of(Items.SUGAR))).forGetter(r -> r.ingredients)
    ).apply(inst, (category, ingredients) -> new SweeteningRecipe(category, NonNullList.copyOf(ingredients))));

    public static final StreamCodec<RegistryFriendlyByteBuf, SweeteningRecipe> STREAM_CODEC = StreamCodec.of(
            (buf, recipe) -> {
                CraftingBookCategory.STREAM_CODEC.encode(buf, recipe.category());
                buf.writeVarInt(recipe.ingredients.size());
                for (Ingredient ing : recipe.ingredients) {
                    Ingredient.CONTENTS_STREAM_CODEC.encode(buf, ing);
                }
            },
            buf -> {
                CraftingBookCategory cat = CraftingBookCategory.STREAM_CODEC.decode(buf);
                int size = buf.readVarInt();
                NonNullList<Ingredient> list = NonNullList.withSize(size, Ingredient.EMPTY);
                for (int i = 0; i < size; i++) {
                    list.set(i, Ingredient.CONTENTS_STREAM_CODEC.decode(buf));
                }
                return new SweeteningRecipe(cat, list);
            }
    );

    private final NonNullList<Ingredient> ingredients;

    public SweeteningRecipe(CraftingBookCategory category, NonNullList<Ingredient> ingredients) {
        super(category);
        this.ingredients = ingredients;
    }

    public static boolean isSweetenable(ItemStack stack) {
        if (stack == null || stack.isEmpty() || Sweetening.isSweetened(stack)) {
            return false;
        }
        return stack.has(DataComponents.FOOD) || stack.is(SaltTags.Items.CAN_BE_SALTED);
    }

    public boolean isSugar(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        if (stack.is(Items.SUGAR) || stack.is(COMMON_SUGARS) || stack.is(FORGE_SUGARS)) {
            return true;
        }
        for (Ingredient ingredient : this.ingredients) {
            if (ingredient.test(stack)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public @NotNull NonNullList<ItemStack> getRemainingItems(CraftingInput pContainer) {
        return NonNullList.withSize(pContainer.size(), ItemStack.EMPTY);
    }

    @Override
    public boolean matches(CraftingInput craftingInput, @NotNull Level level) {
        ItemStack food = ItemStack.EMPTY;
        int sugarCount = 0;
        int totalItems = 0;

        for (int i = 0; i < craftingInput.size(); i++) {
            ItemStack stack = craftingInput.getItem(i);
            if (stack.isEmpty()) {
                continue;
            }

            totalItems++;

            if (isSugar(stack)) {
                sugarCount++;
            } else if (isSweetenable(stack) && food.isEmpty()) {
                food = stack;
            } else {
                return false;
            }
        }

        int requiredSugar = Math.max(1, this.ingredients.size());
        return !food.isEmpty() && sugarCount == requiredSugar && totalItems == requiredSugar + 1;
    }

    @Override
    public @NotNull ItemStack assemble(CraftingInput craftingInput, @NotNull HolderLookup.Provider registryAccess) {
        for (int index = 0; index < craftingInput.size(); index++) {
            ItemStack itemStack = craftingInput.getItem(index);

            if (!isSugar(itemStack) && isSweetenable(itemStack)) {
                ItemStack resultStack = itemStack.copyWithCount(1);
                return Sweetening.setSweetened(resultStack);
            }
        }

        return ItemStack.EMPTY;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= Math.max(2, this.ingredients.size() + 1);
    }

    @Override
    public @NotNull RecipeSerializer<?> getSerializer() {
        return SaltRecipeSerializers.SWEETENING.get();
    }

    @Override
    public @NotNull NonNullList<Ingredient> getIngredients() {
        return ingredients;
    }

    public Ingredient getFoodIngredient() {
        return Ingredient.of(SaltTags.Items.CAN_BE_SALTED);
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

