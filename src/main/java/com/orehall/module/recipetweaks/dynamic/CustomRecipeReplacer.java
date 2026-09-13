package com.orehall.module.recipetweaks.dynamic;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

import com.orehall.Orehall;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderSet;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapedRecipePattern;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.neoforged.neoforge.common.conditions.ICondition;

public class CustomRecipeReplacer {

    private static final Map<String, Pattern> WILDCARD_PATTERN_CACHE = new ConcurrentHashMap<>();

    public static class ReplacementResult {
        public final Set<ResourceLocation> recipesToRemove = new HashSet<>();
        public final List<RecipeHolder<?>> recipesToAdd = new ArrayList<>();
    }

    /**
     * Processes user-defined custom recipe replacement rules against current server recipes.
     *
     * @param recipes          All loaded recipes.
     * @param alreadyRemoved   Set of recipe IDs already scheduled for removal by other replacers.
     * @param rawRules         List of raw configuration strings.
     * @param registries       Holder lookup provider for registry inspection.
     * @param conditionContext Condition context for tag resolution during datapack reload.
     * @return A ReplacementResult containing IDs to remove and new RecipeHolders to register.
     */
    public static ReplacementResult process(Iterable<RecipeHolder<?>> recipes,
                                            Set<ResourceLocation> alreadyRemoved,
                                            List<String> rawRules,
                                            HolderLookup.Provider registries,
                                            ICondition.IContext conditionContext) {
        ReplacementResult result = new ReplacementResult();
        if (rawRules == null || rawRules.isEmpty()) {
            return result;
        }

        List<CustomReplacerRule> parsedRules = new ArrayList<>();
        for (int i = 0; i < rawRules.size(); i++) {
            Optional<CustomReplacerRule> ruleOpt = CustomReplacerParser.parse(rawRules.get(i), i);
            ruleOpt.ifPresent(parsedRules::add);
        }

        if (parsedRules.isEmpty()) {
            return result;
        }

        for (CustomReplacerRule rule : parsedRules) {
            if (!rule.enabled()) {
                Orehall.LOGGER.debug("Custom Recipe Replacer: Rule '{}' is disabled, skipping.", rule.id());
                continue;
            }

            int replacedForRule = 0;

            for (RecipeHolder<?> holder : recipes) {
                ResourceLocation id = holder.id();

                // Skip recipes already removed by previous rules or hardcoded replacers
                if (alreadyRemoved.contains(id) || result.recipesToRemove.contains(id)) {
                    continue;
                }

                // Prevent recursive loops: never match Orehall dynamic recipes
                if (id.getNamespace().equals(Orehall.MODID)) {
                    continue;
                }

                if (!(holder.value() instanceof CraftingRecipe craftingRecipe)) {
                    continue;
                }

                // 1. Match recipe ID if specified
                if (rule.recipeId() != null && !rule.recipeId().isBlank()) {
                    if (!matchesPattern(id.toString(), rule.recipeId()) && !matchesPattern(id.getPath(), rule.recipeId())) {
                        continue;
                    }
                }

                ItemStack resultStack = craftingRecipe.getResultItem(registries);

                // 2. Match target item/tag if specified
                if (rule.target() != null && !rule.target().isBlank()) {
                    if (resultStack.isEmpty() || !matchesTarget(resultStack, rule.target(), conditionContext)) {
                        continue;
                    }
                }

                // 3. Filter non-empty ingredients
                List<Ingredient> nonEmpties = craftingRecipe.getIngredients().stream()
                        .filter(ing -> !ing.isEmpty())
                        .toList();
                if (nonEmpties.isEmpty()) {
                    continue;
                }

                // 4. Match required input ingredient (has_ingredient)
                if (rule.hasIngredient() != null && !rule.hasIngredient().isBlank()) {
                    if (!containsMatchingIngredient(nonEmpties, rule.hasIngredient(), conditionContext, registries)) {
                        continue;
                    }
                }

                // 5. Match input count constraint
                if (rule.inputCount() != null && nonEmpties.size() != rule.inputCount()) {
                    continue;
                }

                // 6. Match exclusion constraint
                if (rule.exclude() != null && !rule.exclude().isBlank()) {
                    if (containsMatchingIngredient(nonEmpties, rule.exclude(), conditionContext, registries)) {
                        continue;
                    }
                    if (!resultStack.isEmpty() && matchesTarget(resultStack, rule.exclude(), conditionContext)) {
                        continue;
                    }
                }

                // 7. Calculate output stack
                int outputCount = rule.calculateOutputCount(resultStack.isEmpty() ? 1 : resultStack.getCount());
                ItemStack newOutput = resultStack.isEmpty() ? ItemStack.EMPTY : resultStack.copyWithCount(outputCount);

                String group = rule.group() != null ? rule.group() : (craftingRecipe.getGroup().isEmpty() ? "" : craftingRecipe.getGroup());
                CraftingBookCategory category = craftingRecipe.category();

                CraftingRecipe replacementRecipe = null;

                try {
                    if (rule.isSwap()) {
                        replacementRecipe = createSwappedRecipe(craftingRecipe, rule, newOutput, group, category, registries, conditionContext);
                    } else if (rule.isShaped()) {
                        List<Ingredient> distinctIngredients = extractDistinctIngredients(nonEmpties, rule, conditionContext, registries);
                        replacementRecipe = createShapedRecipe(rule, distinctIngredients, newOutput, group, category, registries, conditionContext);
                    } else if (rule.isShapeless()) {
                        List<Ingredient> distinctIngredients = extractDistinctIngredients(nonEmpties, rule, conditionContext, registries);
                        replacementRecipe = createShapelessRecipe(rule, distinctIngredients, newOutput, group, category, registries, conditionContext);
                    } else {
                        // Output-only tweak mode
                        replacementRecipe = createOutputOnlyRecipe(craftingRecipe, newOutput, group, category);
                    }
                } catch (Exception e) {
                    Orehall.LOGGER.warn("Custom Recipe Replacer: Failed to create replacement recipe for rule '{}' on recipe '{}': {}",
                            rule.id(), id, e.getMessage());
                    continue;
                }

                if (replacementRecipe == null) {
                    continue;
                }

                // 8. Generate safe, deterministic recipe ID
                String safeRuleId = rule.id().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9/._-]", "_");
                String safeRecipeNs = id.getNamespace().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9/._-]", "_");
                String safeRecipePath = id.getPath().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9/._-]", "_");

                ResourceLocation newId = ResourceLocation.fromNamespaceAndPath(
                        Orehall.MODID,
                        "dynamic/" + safeRuleId + "_" + safeRecipeNs + "_" + safeRecipePath
                );

                result.recipesToRemove.add(id);
                result.recipesToAdd.add(new RecipeHolder<>(newId, replacementRecipe));
                replacedForRule++;
            }

            Orehall.LOGGER.info("Custom Recipe Replacer: Rule '{}' matched and replaced {} recipe(s).", rule.id(), replacedForRule);
        }

        return result;
    }

    private static CraftingRecipe createSwappedRecipe(CraftingRecipe craftingRecipe,
                                                      CustomReplacerRule rule,
                                                      ItemStack output,
                                                      String group,
                                                      CraftingBookCategory category,
                                                      HolderLookup.Provider registries,
                                                      ICondition.IContext context) {
        Ingredient replacement = resolveIngredient(rule.swapTo(), registries, context);
        if (replacement == null || replacement.isEmpty()) {
            Orehall.LOGGER.warn("Custom Recipe Replacer: Failed to resolve swap replacement '{}' in rule '{}'", rule.swapTo(), rule.id());
            return null;
        }
        replacement = normalizeIngredient(replacement, registries, context);

        if (craftingRecipe instanceof ShapedRecipe shaped) {
            NonNullList<Ingredient> origs = shaped.pattern.ingredients();
            NonNullList<Ingredient> newIngredients = NonNullList.withSize(origs.size(), Ingredient.EMPTY);
            boolean matchedAny = false;

            for (int i = 0; i < origs.size(); i++) {
                Ingredient ing = origs.get(i);
                if (!ing.isEmpty() && ingredientMatches(ing, rule.swapFrom(), context, registries)) {
                    newIngredients.set(i, replacement);
                    matchedAny = true;
                } else {
                    newIngredients.set(i, ing);
                }
            }

            if (!matchedAny) {
                return null;
            }

            ShapedRecipePattern newPattern = new ShapedRecipePattern(shaped.getWidth(), shaped.getHeight(), newIngredients, Optional.empty());
            return new ShapedRecipe(group, category, newPattern, output, shaped.showNotification());
        } else if (craftingRecipe instanceof ShapelessRecipe shapeless) {
            NonNullList<Ingredient> origs = shapeless.getIngredients();
            NonNullList<Ingredient> newIngredients = NonNullList.withSize(origs.size(), Ingredient.EMPTY);
            boolean matchedAny = false;

            for (int i = 0; i < origs.size(); i++) {
                Ingredient ing = origs.get(i);
                if (!ing.isEmpty() && ingredientMatches(ing, rule.swapFrom(), context, registries)) {
                    newIngredients.set(i, replacement);
                    matchedAny = true;
                } else {
                    newIngredients.set(i, ing);
                }
            }

            if (!matchedAny) {
                return null;
            }

            return new ShapelessRecipe(group, category, output, newIngredients);
        }

        return null;
    }

    private static CraftingRecipe createOutputOnlyRecipe(CraftingRecipe original,
                                                         ItemStack output,
                                                         String group,
                                                         CraftingBookCategory category) {
        if (original instanceof ShapedRecipe shaped) {
            return new ShapedRecipe(group, category, shaped.pattern, output, shaped.showNotification());
        } else if (original instanceof ShapelessRecipe shapeless) {
            return new ShapelessRecipe(group, category, output, shapeless.getIngredients());
        }
        return null;
    }

    private static ShapedRecipe createShapedRecipe(CustomReplacerRule rule,
                                                   List<Ingredient> distinctIngredients,
                                                   ItemStack output,
                                                   String group,
                                                   CraftingBookCategory category,
                                                   HolderLookup.Provider registries,
                                                   ICondition.IContext context) {
        Map<Character, Ingredient> keyMap = new HashMap<>();
        Set<Character> usedSymbols = new HashSet<>();

        for (String row : rule.pattern()) {
            for (int i = 0; i < row.length(); i++) {
                char c = row.charAt(i);
                if (c != ' ') {
                    usedSymbols.add(c);
                }
            }
        }

        for (Character c : usedSymbols) {
            Ingredient resolved = null;

            // Dynamic ingredient captures
            if (c == '#' || c == '1') {
                if (!distinctIngredients.isEmpty()) {
                    resolved = distinctIngredients.get(0);
                }
            } else if (c == '2') {
                if (distinctIngredients.size() > 1) {
                    resolved = distinctIngredients.get(1);
                }
            } else if (c == '3') {
                if (distinctIngredients.size() > 2) {
                    resolved = distinctIngredients.get(2);
                }
            } else if (c == '4') {
                if (distinctIngredients.size() > 3) {
                    resolved = distinctIngredients.get(3);
                }
            }

            // Check explicit key mapping
            if (resolved == null && rule.keys().containsKey(c)) {
                String mapping = rule.keys().get(c);
                resolved = resolveIngredient(mapping, registries, context);
            }

            if (resolved == null || resolved.isEmpty()) {
                Orehall.LOGGER.debug("Custom Recipe Replacer: Pattern symbol '{}' could not be resolved for rule '{}'", c, rule.id());
                return null;
            }

            keyMap.put(c, normalizeIngredient(resolved, registries, context));
        }

        ShapedRecipePattern pattern = ShapedRecipePattern.of(keyMap, rule.pattern());
        return new ShapedRecipe(group, category, pattern, output);
    }

    private static ShapelessRecipe createShapelessRecipe(CustomReplacerRule rule,
                                                         List<Ingredient> distinctIngredients,
                                                         ItemStack output,
                                                         String group,
                                                         CraftingBookCategory category,
                                                         HolderLookup.Provider registries,
                                                         ICondition.IContext context) {
        NonNullList<Ingredient> ingredients = NonNullList.create();

        for (String token : rule.shapelessIngredients()) {
            Ingredient resolved = null;
            if (token.equals("#") || token.equals("1") || token.equals("#1")) {
                if (!distinctIngredients.isEmpty()) {
                    resolved = distinctIngredients.get(0);
                }
            } else if (token.equals("2") || token.equals("#2")) {
                if (distinctIngredients.size() > 1) {
                    resolved = distinctIngredients.get(1);
                }
            } else if (token.equals("3") || token.equals("#3")) {
                if (distinctIngredients.size() > 2) {
                    resolved = distinctIngredients.get(2);
                }
            } else if (token.equals("4") || token.equals("#4")) {
                if (distinctIngredients.size() > 3) {
                    resolved = distinctIngredients.get(3);
                }
            } else {
                resolved = resolveIngredient(token, registries, context);
            }

            if (resolved == null || resolved.isEmpty()) {
                Orehall.LOGGER.debug("Custom Recipe Replacer: Shapeless token '{}' could not be resolved for rule '{}'", token, rule.id());
                return null;
            }

            ingredients.add(normalizeIngredient(resolved, registries, context));
        }

        return new ShapelessRecipe(group, category, output, ingredients);
    }

    public static List<Ingredient> extractDistinctIngredients(List<Ingredient> ingredients,
                                                             CustomReplacerRule rule,
                                                             ICondition.IContext context,
                                                             HolderLookup.Provider registries) {
        Set<Item> secondaryItems = new HashSet<>();
        List<TagKey<Item>> secondaryTags = new ArrayList<>();

        if (rule.keys() != null) {
            for (Map.Entry<Character, String> entry : rule.keys().entrySet()) {
                if (entry.getKey() == '#' || Character.isDigit(entry.getKey())) continue;
                String mapping = entry.getValue();
                if (mapping.startsWith("#")) {
                    ResourceLocation tagLoc = ResourceLocation.tryParse(mapping.substring(1));
                    if (tagLoc != null) {
                        secondaryTags.add(TagKey.create(Registries.ITEM, tagLoc));
                    }
                } else {
                    ResourceLocation loc = ResourceLocation.tryParse(mapping);
                    if (loc != null && BuiltInRegistries.ITEM.containsKey(loc)) {
                        secondaryItems.add(BuiltInRegistries.ITEM.get(loc));
                    }
                }
            }
        }

        List<Ingredient> distinct = new ArrayList<>();
        for (Ingredient ing : ingredients) {
            if (isSecondaryIngredient(ing, secondaryItems, secondaryTags, context, registries)) {
                continue;
            }

            boolean alreadyPresent = false;
            for (Ingredient existing : distinct) {
                if (areIngredientsEquivalent(ing, existing, context, registries)) {
                    alreadyPresent = true;
                    break;
                }
            }

            if (!alreadyPresent) {
                distinct.add(ing);
            }
        }

        if (distinct.isEmpty() && !ingredients.isEmpty()) {
            distinct.add(ingredients.get(0));
        }

        return distinct;
    }

    public static boolean areIngredientsEquivalent(Ingredient a, Ingredient b, ICondition.IContext context, HolderLookup.Provider registries) {
        if (a == b) return true;
        if (a == null || b == null || a.isEmpty() || b.isEmpty()) return false;

        // Compare single values directly
        if (!a.isCustom() && !b.isCustom()) {
            Ingredient.Value[] aVals = a.getValues();
            Ingredient.Value[] bVals = b.getValues();
            if (aVals.length == 1 && bVals.length == 1) {
                Ingredient.Value av = aVals[0];
                Ingredient.Value bv = bVals[0];
                if (av instanceof Ingredient.ItemValue aItem && bv instanceof Ingredient.ItemValue bItem) {
                    return aItem.item().getItem() == bItem.item().getItem();
                }
                if (av instanceof Ingredient.TagValue aTag && bv instanceof Ingredient.TagValue bTag) {
                    return aTag.tag().equals(bTag.tag());
                }
            }
        }

        // Test sample item matches
        ItemStack[] aItems = a.getItems();
        ItemStack[] bItems = b.getItems();
        if (aItems.length > 0 && bItems.length > 0 && aItems.length == bItems.length) {
            boolean allMatch = true;
            for (ItemStack stack : aItems) {
                if (!b.test(stack)) {
                    allMatch = false;
                    break;
                }
            }
            if (allMatch) return true;
        }

        return false;
    }

    private static boolean isSecondaryIngredient(Ingredient ing,
                                                 Set<Item> secondaryItems,
                                                 List<TagKey<Item>> secondaryTags,
                                                 ICondition.IContext context,
                                                 HolderLookup.Provider registries) {
        // 1. Direct item testing against secondary items
        for (Item sec : secondaryItems) {
            try {
                if (ing.test(sec.getDefaultInstance())) {
                    return true;
                }
            } catch (Throwable ignored) {
            }
        }

        // 2. Inspect underlying values of the ingredient if available
        if (!ing.isCustom()) {
            for (Ingredient.Value value : ing.getValues()) {
                if (value instanceof Ingredient.ItemValue itemValue) {
                    Item item = itemValue.item().getItem();
                    if (secondaryItems.contains(item)) {
                        return true;
                    }
                    for (TagKey<Item> tag : secondaryTags) {
                        if (itemValue.item().is(tag)) {
                            return true;
                        }
                    }
                } else if (value instanceof Ingredient.TagValue tagValue) {
                    TagKey<Item> tag = tagValue.tag();
                    ResourceLocation tagLoc = tag.location();

                    // Direct match against explicitly mapped secondary tags
                    if (secondaryTags.contains(tag)) {
                        return true;
                    }

                    // Known conventional/semantic tag heuristics for common secondary items
                    for (Item sec : secondaryItems) {
                        if (isKnownTagForItem(sec, tagLoc)) {
                            return true;
                        }
                    }

                    // Check active ConditionContext tag contents
                    if (context != null) {
                        try {
                            Collection<Holder<Item>> holders = context.getTag(tag);
                            if (holders != null && !holders.isEmpty()) {
                                for (Holder<Item> h : holders) {
                                    if (secondaryItems.contains(h.value())) {
                                        return true;
                                    }
                                }
                            }
                        } catch (Throwable ignored) {
                        }
                    }

                    // Check registries lookup
                    if (registries != null) {
                        try {
                            Optional<HolderLookup.RegistryLookup<Item>> itemLookup = registries.lookup(Registries.ITEM);
                            if (itemLookup.isPresent()) {
                                Optional<HolderSet.Named<Item>> tagSet = itemLookup.get().get(tag);
                                if (tagSet.isPresent()) {
                                    for (Holder<Item> h : tagSet.get()) {
                                        if (secondaryItems.contains(h.value())) {
                                            return true;
                                        }
                                    }
                                }
                            }
                        } catch (Throwable ignored) {
                        }
                    }
                }
            }
        }

        return false;
    }

    private static boolean isKnownTagForItem(Item sec, ResourceLocation tagLoc) {
        String path = tagLoc.getPath().toLowerCase(Locale.ROOT);
        String full = tagLoc.toString().toLowerCase(Locale.ROOT);

        // Sticks (e.g. c:rods/wooden, forge:rods/wooden, c:rods, forge:rods, minecraft:sticks)
        if (sec == Items.STICK) {
            return path.equals("rods/wooden")
                    || path.equals("rods")
                    || path.contains("stick")
                    || full.equals("c:rods/wooden")
                    || full.equals("c:rods")
                    || full.equals("forge:rods/wooden")
                    || full.equals("forge:rods");
        }

        // Redstone Dust
        if (sec == Items.REDSTONE) {
            return path.equals("dusts/redstone")
                    || path.contains("redstone")
                    || full.contains("dusts/redstone");
        }

        // Chains
        if (sec == Items.CHAIN) {
            return path.contains("chain");
        }

        // Iron Ingot
        if (sec == Items.IRON_INGOT) {
            return path.equals("ingots/iron") || full.contains("ingots/iron");
        }

        // Gold Ingot
        if (sec == Items.GOLD_INGOT) {
            return path.equals("ingots/gold") || full.contains("ingots/gold");
        }

        // Copper Ingot
        if (sec == Items.COPPER_INGOT) {
            return path.equals("ingots/copper") || full.contains("ingots/copper");
        }

        return false;
    }

    public static boolean containsMatchingIngredient(List<Ingredient> ingredients, String target, ICondition.IContext context, HolderLookup.Provider registries) {
        if (ingredients == null || ingredients.isEmpty() || target == null || target.isBlank()) {
            return false;
        }
        for (Ingredient ing : ingredients) {
            if (ingredientMatches(ing, target, context, registries)) {
                return true;
            }
        }
        return false;
    }

    public static boolean ingredientMatches(Ingredient ing, String target, ICondition.IContext context, HolderLookup.Provider registries) {
        if (ing == null || ing.isEmpty() || target == null || target.isBlank()) {
            return false;
        }

        if (target.contains(",")) {
            String[] tokens = target.split(",");
            for (String token : tokens) {
                if (ingredientMatchesSingle(ing, token.trim(), context, registries)) {
                    return true;
                }
            }
            return false;
        }

        return ingredientMatchesSingle(ing, target.trim(), context, registries);
    }

    private static boolean ingredientMatchesSingle(Ingredient ing, String target, ICondition.IContext context, HolderLookup.Provider registries) {
        if (target.isEmpty()) return false;

        // 1. Tag target (#tag:name)
        if (target.startsWith("#")) {
            ResourceLocation tagLoc = ResourceLocation.tryParse(target.substring(1));
            if (tagLoc == null) return false;
            TagKey<Item> tag = TagKey.create(Registries.ITEM, tagLoc);

            if (!ing.isCustom()) {
                for (Ingredient.Value val : ing.getValues()) {
                    if (val instanceof Ingredient.TagValue tv && tv.tag().equals(tag)) {
                        return true;
                    }
                    if (val instanceof Ingredient.ItemValue iv && iv.item().is(tag)) {
                        return true;
                    }
                }
            }

            if (context != null) {
                try {
                    Collection<Holder<Item>> holders = context.getTag(tag);
                    if (holders != null) {
                        for (Holder<Item> h : holders) {
                            if (ing.test(new ItemStack(h.value()))) {
                                return true;
                            }
                        }
                    }
                } catch (Throwable ignored) {}
            }

            if (registries != null) {
                try {
                    Optional<HolderLookup.RegistryLookup<Item>> lookup = registries.lookup(Registries.ITEM);
                    if (lookup.isPresent()) {
                        Optional<HolderSet.Named<Item>> tagSet = lookup.get().get(tag);
                        if (tagSet.isPresent()) {
                            for (Holder<Item> h : tagSet.get()) {
                                if (ing.test(new ItemStack(h.value()))) {
                                    return true;
                                }
                            }
                        }
                    }
                } catch (Throwable ignored) {}
            }

            return false;
        }

        // 2. Exact item target (modid:item)
        ResourceLocation itemLoc = ResourceLocation.tryParse(target);
        if (itemLoc != null && BuiltInRegistries.ITEM.containsKey(itemLoc)) {
            Item item = BuiltInRegistries.ITEM.get(itemLoc);
            if (ing.test(item.getDefaultInstance())) {
                return true;
            }
            if (!ing.isCustom()) {
                for (Ingredient.Value val : ing.getValues()) {
                    if (val instanceof Ingredient.ItemValue iv && iv.item().getItem() == item) {
                        return true;
                    }
                }
            }
            return false;
        }

        // 3. Wildcard target (*item*)
        if (target.contains("*") || target.contains("?")) {
            ItemStack[] items = ing.getItems();
            for (ItemStack stack : items) {
                ResourceLocation key = BuiltInRegistries.ITEM.getKey(stack.getItem());
                if (matchesPattern(key.toString(), target) || matchesPattern(key.getPath(), target)) {
                    return true;
                }
            }
        }

        return false;
    }

    public static boolean matchesTarget(ItemStack stack, String target, ICondition.IContext context) {
        if (target == null || target.isBlank()) {
            return false;
        }

        if (target.contains(",")) {
            String[] tokens = target.split(",");
            for (String token : tokens) {
                if (matchesSingleTarget(stack, token.trim(), context)) {
                    return true;
                }
            }
            return false;
        }

        return matchesSingleTarget(stack, target.trim(), context);
    }

    private static boolean matchesSingleTarget(ItemStack stack, String target, ICondition.IContext context) {
        if (target.isEmpty()) {
            return false;
        }

        Item item = stack.getItem();
        ResourceLocation itemKey = BuiltInRegistries.ITEM.getKey(item);
        String path = itemKey.getPath().toLowerCase(Locale.ROOT);
        String fullId = itemKey.toString().toLowerCase(Locale.ROOT);

        if (target.startsWith("#")) {
            ResourceLocation tagLoc = ResourceLocation.tryParse(target.substring(1));
            if (tagLoc == null) {
                return false;
            }

            TagKey<Item> tag = TagKey.create(Registries.ITEM, tagLoc);

            if (context != null) {
                try {
                    Collection<Holder<Item>> holders = context.getTag(tag);
                    if (holders != null) {
                        for (Holder<Item> h : holders) {
                            if (h.value() == item) {
                                return true;
                            }
                        }
                    }
                } catch (Throwable ignored) {
                }
            }

            if (stack.is(tag)) {
                return true;
            }

            return false;
        }

        if (target.contains("*") || target.contains("?")) {
            return matchesPattern(fullId, target) || matchesPattern(path, target);
        }

        ResourceLocation targetLoc = ResourceLocation.tryParse(target);
        if (targetLoc != null) {
            return itemKey.equals(targetLoc);
        }

        return fullId.equals(target.toLowerCase(Locale.ROOT)) || path.equals(target.toLowerCase(Locale.ROOT));
    }

    private static boolean matchesPattern(String text, String wildcardPattern) {
        Pattern pattern = WILDCARD_PATTERN_CACHE.computeIfAbsent(wildcardPattern, p -> {
            StringBuilder regex = new StringBuilder("^");
            for (int i = 0; i < p.length(); i++) {
                char c = p.charAt(i);
                if (c == '*') {
                    regex.append(".*");
                } else if (c == '?') {
                    regex.append(".");
                } else if ("\\.[]{}()+-^$|".indexOf(c) != -1) {
                    regex.append('\\').append(c);
                } else {
                    regex.append(c);
                }
            }
            regex.append("$");
            return Pattern.compile(regex.toString(), Pattern.CASE_INSENSITIVE);
        });

        return pattern.matcher(text).matches();
    }

    private static Ingredient resolveIngredient(String idOrTag, HolderLookup.Provider registries, ICondition.IContext context) {
        if (idOrTag == null || idOrTag.isBlank()) {
            return null;
        }

        if (idOrTag.startsWith("#")) {
            ResourceLocation tagLoc = ResourceLocation.tryParse(idOrTag.substring(1));
            if (tagLoc == null) {
                return null;
            }
            TagKey<Item> tag = TagKey.create(Registries.ITEM, tagLoc);
            Ingredient raw = Ingredient.of(tag);
            return normalizeIngredient(raw, registries, context);
        } else {
            ResourceLocation itemLoc = ResourceLocation.tryParse(idOrTag);
            if (itemLoc == null || !BuiltInRegistries.ITEM.containsKey(itemLoc)) {
                return null;
            }
            Item item = BuiltInRegistries.ITEM.get(itemLoc);
            return Ingredient.of(item);
        }
    }

    public static Ingredient normalizeIngredient(Ingredient ingredient, HolderLookup.Provider registries, ICondition.IContext context) {
        if (ingredient == null || ingredient.isEmpty() || ingredient.isCustom()) {
            return ingredient;
        }

        boolean hasTagValue = false;
        for (Ingredient.Value value : ingredient.getValues()) {
            if (value instanceof Ingredient.TagValue) {
                hasTagValue = true;
                break;
            }
        }

        if (!hasTagValue) {
            return ingredient;
        }

        Set<Item> addedItems = new HashSet<>();
        List<ItemStack> resolvedStacks = new ArrayList<>();
        for (Ingredient.Value value : ingredient.getValues()) {
            if (value instanceof Ingredient.ItemValue itemValue) {
                if (addedItems.add(itemValue.item().getItem())) {
                    resolvedStacks.add(itemValue.item());
                }
            } else if (value instanceof Ingredient.TagValue tagValue) {
                TagKey<Item> tag = tagValue.tag();
                boolean resolved = false;

                // 1. Try registry lookup
                if (registries != null) {
                    try {
                        Optional<HolderLookup.RegistryLookup<Item>> lookup = registries.lookup(Registries.ITEM);
                        if (lookup.isPresent()) {
                            Optional<HolderSet.Named<Item>> tagSet = lookup.get().get(tag);
                            if (tagSet.isPresent() && tagSet.get().size() > 0) {
                                for (Holder<Item> holder : tagSet.get()) {
                                    if (addedItems.add(holder.value())) {
                                        resolvedStacks.add(new ItemStack(holder.value()));
                                    }
                                }
                                resolved = true;
                            }
                        }
                    } catch (Throwable ignored) {
                    }
                }

                // 2. Try ConditionContext lookup
                if (!resolved && context != null) {
                    try {
                        Collection<Holder<Item>> holders = context.getTag(tag);
                        if (holders != null && !holders.isEmpty()) {
                            for (Holder<Item> h : holders) {
                                if (addedItems.add(h.value())) {
                                    resolvedStacks.add(new ItemStack(h.value()));
                                }
                            }
                            resolved = true;
                        }
                    } catch (Throwable ignored) {
                    }
                }

                // 3. Fallback to known items for conventional tags
                if (!resolved) {
                    List<Item> knownItems = getKnownItemsForTag(tag.location());
                    if (!knownItems.isEmpty()) {
                        for (Item item : knownItems) {
                            if (addedItems.add(item)) {
                                resolvedStacks.add(new ItemStack(item));
                            }
                        }
                        resolved = true;
                    }
                }
            }
        }

        if (!resolvedStacks.isEmpty()) {
            return Ingredient.of(resolvedStacks.stream());
        }

        return ingredient;
    }

    private static List<Item> getKnownItemsForTag(ResourceLocation tagLoc) {
        String path = tagLoc.getPath().toLowerCase(Locale.ROOT);
        String full = tagLoc.toString().toLowerCase(Locale.ROOT);

        // Gold ingots
        if (path.equals("ingots/gold") || path.equals("gold_ingots") || path.contains("gold_ingot")
                || full.equals("c:ingots/gold") || full.equals("forge:ingots/gold") || full.equals("minecraft:gold_ingots")) {
            return List.of(Items.GOLD_INGOT);
        }

        // Iron ingots
        if (path.equals("ingots/iron") || path.equals("iron_ingots") || path.contains("iron_ingot")
                || full.equals("c:ingots/iron") || full.equals("forge:ingots/iron") || full.equals("minecraft:iron_ingots")) {
            return List.of(Items.IRON_INGOT);
        }

        // Copper ingots
        if (path.equals("ingots/copper") || path.equals("copper_ingots") || path.contains("copper_ingot")
                || full.equals("c:ingots/copper") || full.equals("forge:ingots/copper")) {
            return List.of(Items.COPPER_INGOT);
        }

        // Netherite ingots
        if (path.equals("ingots/netherite") || path.equals("netherite_ingots") || path.contains("netherite_ingot")
                || full.equals("c:ingots/netherite") || full.equals("forge:ingots/netherite")) {
            return List.of(Items.NETHERITE_INGOT);
        }

        // Gold nuggets
        if (path.equals("nuggets/gold") || path.equals("gold_nuggets") || path.contains("gold_nugget")
                || full.equals("c:nuggets/gold") || full.equals("forge:nuggets/gold")) {
            return List.of(Items.GOLD_NUGGET);
        }

        // Iron nuggets
        if (path.equals("nuggets/iron") || path.equals("iron_nuggets") || path.contains("iron_nugget")
                || full.equals("c:nuggets/iron") || full.equals("forge:nuggets/iron")) {
            return List.of(Items.IRON_NUGGET);
        }

        // Sticks / rods
        if (path.equals("rods/wooden") || path.equals("rods") || full.equals("c:rods/wooden") || full.equals("c:rods")
                || full.equals("forge:rods/wooden") || full.equals("forge:rods") || path.contains("stick")) {
            return List.of(Items.STICK);
        }

        // Redstone dust
        if (path.equals("dusts/redstone") || full.equals("c:dusts/redstone") || full.equals("forge:dusts/redstone")
                || path.contains("redstone")) {
            return List.of(Items.REDSTONE);
        }

        // Chains
        if (path.contains("chain")) {
            return List.of(Items.CHAIN);
        }

        // Diamonds
        if (path.equals("gems/diamond") || path.equals("diamonds") || full.equals("c:gems/diamond") || full.equals("forge:gems/diamond")) {
            return List.of(Items.DIAMOND);
        }

        // Emeralds
        if (path.equals("gems/emerald") || path.equals("emeralds") || full.equals("c:gems/emerald") || full.equals("forge:gems/emerald")) {
            return List.of(Items.EMERALD);
        }

        // Amethyst
        if (path.equals("gems/amethyst") || path.equals("amethyst_shards") || full.equals("c:gems/amethyst") || full.equals("forge:gems/amethyst")) {
            return List.of(Items.AMETHYST_SHARD);
        }

        // Lapis
        if (path.equals("gems/lapis") || full.equals("c:gems/lapis") || path.contains("lapis")) {
            return List.of(Items.LAPIS_LAZULI);
        }

        // Quartz
        if (path.equals("gems/quartz") || full.equals("c:gems/quartz") || path.contains("quartz")) {
            return List.of(Items.QUARTZ);
        }

        // Coal
        if (path.equals("coal") || full.equals("c:coal") || path.contains("coal")) {
            return List.of(Items.COAL);
        }

        return List.of();
    }
}
