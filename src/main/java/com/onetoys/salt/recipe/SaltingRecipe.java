package com.onetoys.salt.recipe;

import com.onetoys.salt.SaltItems;
import com.onetoys.salt.SaltRecipeSerializers;
import com.onetoys.salt.SaltTags;
import com.onetoys.salt.Salting;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class SaltingRecipe extends CustomRecipe {
    public static final MapCodec<SaltingRecipe> CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
            CraftingBookCategory.CODEC.optionalFieldOf("category", CraftingBookCategory.MISC).forGetter(CustomRecipe::category),
            Ingredient.CODEC_NONEMPTY.listOf().optionalFieldOf("ingredients", List.of(Ingredient.of(SaltTags.Items.COMMON_SALTS))).forGetter(r -> r.ingredients)
    ).apply(inst, (category, ingredients) -> new SaltingRecipe(category, NonNullList.copyOf(ingredients))));

    public static final StreamCodec<RegistryFriendlyByteBuf, SaltingRecipe> STREAM_CODEC = StreamCodec.of(
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
                return new SaltingRecipe(cat, list);
            }
    );

    private final NonNullList<Ingredient> ingredients;

    public SaltingRecipe(CraftingBookCategory category, NonNullList<Ingredient> ingredients) {
        super(category);
        this.ingredients = ingredients;
    }

    public static boolean isSaltable(ItemStack stack) {
        if (stack == null || stack.isEmpty() || Salting.isSalted(stack)) {
            return false;
        }
        return stack.has(DataComponents.FOOD) || stack.is(SaltTags.Items.CAN_BE_SALTED);
    }

    public boolean isSalt(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        if (stack.is(SaltItems.SALT) || stack.is(SaltTags.Items.COMMON_SALTS) || stack.is(SaltTags.Items.FORGE_SALTS)) {
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
        int saltCount = 0;
        int totalItems = 0;

        for (int i = 0; i < craftingInput.size(); i++) {
            ItemStack stack = craftingInput.getItem(i);
            if (stack.isEmpty()) {
                continue;
            }

            totalItems++;

            if (isSalt(stack)) {
                saltCount++;
            } else if (isSaltable(stack) && food.isEmpty()) {
                food = stack;
            } else {
                return false;
            }
        }

        int requiredSalt = Math.max(1, this.ingredients.size());
        return !food.isEmpty() && saltCount == requiredSalt && totalItems == requiredSalt + 1;
    }

    @Override
    public @NotNull ItemStack assemble(CraftingInput craftingInput, @NotNull HolderLookup.Provider registryAccess) {
        for (int index = 0; index < craftingInput.size(); index++) {
            ItemStack itemStack = craftingInput.getItem(index);

            if (!isSalt(itemStack) && isSaltable(itemStack)) {
                ItemStack resultStack = itemStack.copyWithCount(1);
                return Salting.setSalted(resultStack);
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
        return SaltRecipeSerializers.SALTING.get();
    }

    @Override
    public @NotNull NonNullList<Ingredient> getIngredients() {
        return ingredients;
    }

    public Ingredient getFoodIngredient() {
        return Ingredient.of(SaltTags.Items.CAN_BE_SALTED);
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

