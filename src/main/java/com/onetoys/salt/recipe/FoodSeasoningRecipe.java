package com.onetoys.salt.recipe;

import com.onetoys.Onetoys;
import com.onetoys.module.content.ContentModule;
import com.onetoys.salt.SaltItems;
import com.onetoys.salt.SaltRecipeSerializers;
import com.onetoys.salt.SaltTags;
import com.onetoys.salt.Salting;
import com.onetoys.salt.Satiating;
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
import java.util.function.BiFunction;

public class FoodSeasoningRecipe extends CustomRecipe {
    public static final TagKey<Item> COMMON_SUGARS = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("c", "sugars"));
    public static final TagKey<Item> FORGE_SUGARS = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("forge", "sugars"));

    public static final MapCodec<FoodSeasoningRecipe> CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
            CraftingBookCategory.CODEC.optionalFieldOf("category", CraftingBookCategory.MISC).forGetter(CustomRecipe::category)
    ).apply(inst, FoodSeasoningRecipe::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, FoodSeasoningRecipe> STREAM_CODEC = StreamCodec.of(
            (buf, recipe) -> CraftingBookCategory.STREAM_CODEC.encode(buf, recipe.category()),
            buf -> new FoodSeasoningRecipe(CraftingBookCategory.STREAM_CODEC.decode(buf))
    );

    public FoodSeasoningRecipe(CraftingBookCategory category) {
        super(category);
    }

    public static boolean isSalt(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        return stack.is(SaltItems.SALT) || stack.is(SaltTags.Items.COMMON_SALTS) || stack.is(SaltTags.Items.FORGE_SALTS);
    }

    public static boolean isSugar(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        return stack.is(Items.SUGAR) || stack.is(COMMON_SUGARS) || stack.is(FORGE_SUGARS);
    }

    public static boolean isChrysanthemum(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        return stack.is(Onetoys.CHRYSANTHEMUM);
    }

    public static boolean isSeasonableFood(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        return stack.has(DataComponents.FOOD) || stack.is(SaltTags.Items.CAN_BE_SALTED);
    }

    @Override
    public boolean matches(CraftingInput craftingInput, @NotNull Level level) {
        if (!ContentModule.isSaltEnabled()) {
            return false;
        }

        ItemStack food = ItemStack.EMPTY;
        int saltCount = 0;
        int sugarCount = 0;
        int chrysanthemumCount = 0;
        int totalItems = 0;

        for (int i = 0; i < craftingInput.size(); i++) {
            ItemStack stack = craftingInput.getItem(i);
            if (stack.isEmpty()) {
                continue;
            }

            totalItems++;

            if (isSalt(stack)) {
                saltCount++;
            } else if (isSugar(stack)) {
                sugarCount++;
            } else if (isChrysanthemum(stack)) {
                chrysanthemumCount++;
            } else if (isSeasonableFood(stack) && food.isEmpty()) {
                food = stack;
            } else {
                return false;
            }
        }

        if (food.isEmpty()) {
            return false;
        }

        // Must not have duplicate seasoning stacks of the same kind
        if (saltCount > 1 || sugarCount > 1 || chrysanthemumCount > 1) {
            return false;
        }

        int seasoningCount = saltCount + sugarCount + chrysanthemumCount;
        // Requires at least 2 seasonings in one go (combinations of 2 or 3 seasonings; single seasonings are handled by dedicated recipes)
        if (seasoningCount < 2) {
            return false;
        }

        if (totalItems != 1 + seasoningCount) {
            return false;
        }

        // Cannot apply seasonings that the food already has
        if (saltCount > 0 && Salting.isSalted(food)) {
            return false;
        }
        if (sugarCount > 0 && Sweetening.isSweetened(food)) {
            return false;
        }
        if (chrysanthemumCount > 0 && Satiating.isSatiating(food)) {
            return false;
        }

        return true;
    }

    @Override
    public @NotNull ItemStack assemble(CraftingInput craftingInput, @NotNull HolderLookup.Provider registryAccess) {
        ItemStack food = ItemStack.EMPTY;
        boolean hasSalt = false;
        boolean hasSugar = false;
        boolean hasChrysanthemum = false;

        for (int i = 0; i < craftingInput.size(); i++) {
            ItemStack stack = craftingInput.getItem(i);
            if (stack.isEmpty()) {
                continue;
            }

            if (isSalt(stack)) {
                hasSalt = true;
            } else if (isSugar(stack)) {
                hasSugar = true;
            } else if (isChrysanthemum(stack)) {
                hasChrysanthemum = true;
            } else if (isSeasonableFood(stack) && food.isEmpty()) {
                food = stack;
            }
        }

        if (food.isEmpty()) {
            return ItemStack.EMPTY;
        }

        ItemStack result = food.copyWithCount(1);
        if (hasSalt) {
            Salting.setSalted(result);
        }
        if (hasSugar) {
            Sweetening.setSweetened(result);
        }
        if (hasChrysanthemum) {
            Satiating.setSatiating(result);
        }
        return result;
    }

    @Override
    public @NotNull NonNullList<ItemStack> getRemainingItems(CraftingInput pContainer) {
        return NonNullList.withSize(pContainer.size(), ItemStack.EMPTY);
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 3;
    }

    @Override
    public @NotNull RecipeSerializer<?> getSerializer() {
        return SaltRecipeSerializers.FOOD_SEASONING.get();
    }

    public static class Serializer implements RecipeSerializer<FoodSeasoningRecipe> {
        @Override
        public @NotNull MapCodec<FoodSeasoningRecipe> codec() {
            return FoodSeasoningRecipe.CODEC;
        }

        @Override
        public @NotNull StreamCodec<RegistryFriendlyByteBuf, FoodSeasoningRecipe> streamCodec() {
            return FoodSeasoningRecipe.STREAM_CODEC;
        }
    }
}

abstract class SingleSeasoningRecipe extends CustomRecipe {
    protected final NonNullList<Ingredient> ingredients;

    public SingleSeasoningRecipe(CraftingBookCategory category, NonNullList<Ingredient> ingredients) {
        super(category);
        this.ingredients = ingredients;
    }

    protected abstract boolean isSeasoning(ItemStack stack);

    protected abstract boolean isSeasonable(ItemStack stack);

    protected abstract ItemStack applySeasoning(ItemStack stack);

    protected boolean testIngredients(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
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
        if (!ContentModule.isSaltEnabled()) {
            return false;
        }

        ItemStack food = ItemStack.EMPTY;
        int seasoningCount = 0;
        int totalItems = 0;

        for (int i = 0; i < craftingInput.size(); i++) {
            ItemStack stack = craftingInput.getItem(i);
            if (stack.isEmpty()) {
                continue;
            }

            totalItems++;

            if (isSeasoning(stack)) {
                seasoningCount++;
            } else if (isSeasonable(stack) && food.isEmpty()) {
                food = stack;
            } else {
                return false;
            }
        }

        int requiredSeasoning = Math.max(1, this.ingredients.size());
        return !food.isEmpty() && seasoningCount == requiredSeasoning && totalItems == requiredSeasoning + 1;
    }

    @Override
    public @NotNull ItemStack assemble(CraftingInput craftingInput, @NotNull HolderLookup.Provider registryAccess) {
        for (int index = 0; index < craftingInput.size(); index++) {
            ItemStack itemStack = craftingInput.getItem(index);

            if (!isSeasoning(itemStack) && isSeasonable(itemStack)) {
                ItemStack resultStack = itemStack.copyWithCount(1);
                return applySeasoning(resultStack);
            }
        }

        return ItemStack.EMPTY;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= Math.max(2, this.ingredients.size() + 1);
    }

    @Override
    public @NotNull NonNullList<Ingredient> getIngredients() {
        return ingredients;
    }

    public static <T extends SingleSeasoningRecipe> MapCodec<T> makeCodec(Ingredient defaultIngredient, BiFunction<CraftingBookCategory, NonNullList<Ingredient>, T> factory) {
        return RecordCodecBuilder.mapCodec(inst -> inst.group(
                CraftingBookCategory.CODEC.optionalFieldOf("category", CraftingBookCategory.MISC).forGetter(CustomRecipe::category),
                Ingredient.CODEC_NONEMPTY.listOf().optionalFieldOf("ingredients", List.of(defaultIngredient)).forGetter(SingleSeasoningRecipe::getIngredients)
        ).apply(inst, (category, ingredients) -> factory.apply(category, NonNullList.copyOf(ingredients))));
    }

    public static <T extends SingleSeasoningRecipe> StreamCodec<RegistryFriendlyByteBuf, T> makeStreamCodec(BiFunction<CraftingBookCategory, NonNullList<Ingredient>, T> factory) {
        return StreamCodec.of(
                (buf, recipe) -> {
                    CraftingBookCategory.STREAM_CODEC.encode(buf, recipe.category());
                    buf.writeVarInt(recipe.getIngredients().size());
                    for (Ingredient ing : recipe.getIngredients()) {
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
                    return factory.apply(cat, list);
                }
        );
    }
}
