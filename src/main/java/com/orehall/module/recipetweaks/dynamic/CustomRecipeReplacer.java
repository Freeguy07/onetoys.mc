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
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapedRecipePattern;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.neoforged.neoforge.common.conditions.ICondition;

public class CustomRecipeReplacer {

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

                ItemStack resultStack = craftingRecipe.getResultItem(registries);
                if (resultStack.isEmpty()) {
                    continue;
                }

                // 1. Target item/tag matching
                if (!matchesTarget(resultStack, rule.target(), conditionContext)) {
                    continue;
                }

                // 2. Filter non-empty ingredients
                List<Ingredient> nonEmpties = craftingRecipe.getIngredients().stream()
                        .filter(ing -> !ing.isEmpty())
                        .toList();
                if (nonEmpties.isEmpty()) {
                    continue;
                }

                // 3. Input count constraint
                if (rule.inputCount() != null && nonEmpties.size() != rule.inputCount()) {
                    continue;
                }

                // 4. Exclusion constraint
                if (rule.exclude() != null && !rule.exclude().isBlank()) {
                    if (containsExcludedIngredient(nonEmpties, rule.exclude(), conditionContext)) {
                        continue;
                    }
                }

                // 5. Extract primary base material ingredient (ignoring secondary key ingredients)
                Ingredient baseIngredient = extractBaseIngredient(nonEmpties, rule, conditionContext);

                // 6. Build output stack
                int outputCount = rule.calculateOutputCount(resultStack.getCount());
                ItemStack newOutput = resultStack.copyWithCount(outputCount);

                String group = rule.group() != null ? rule.group() : (craftingRecipe.getGroup().isEmpty() ? "" : craftingRecipe.getGroup());
                CraftingBookCategory category = craftingRecipe.category();

                CraftingRecipe replacementRecipe = null;

                try {
                    if (rule.isShaped()) {
                        replacementRecipe = createShapedRecipe(rule, baseIngredient, newOutput, group, category, registries);
                    } else {
                        replacementRecipe = createShapelessRecipe(rule, baseIngredient, newOutput, group, category, registries);
                    }
                } catch (Exception e) {
                    Orehall.LOGGER.warn("Custom Recipe Replacer: Failed to create replacement recipe for rule '{}' on recipe '{}': {}",
                            rule.id(), id, e.getMessage());
                    continue;
                }

                if (replacementRecipe == null) {
                    continue;
                }

                // 7. Generate safe, deterministic recipe ID
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

    private static ShapedRecipe createShapedRecipe(CustomReplacerRule rule,
                                                   Ingredient baseIngredient,
                                                   ItemStack output,
                                                   String group,
                                                   CraftingBookCategory category,
                                                   HolderLookup.Provider registries) {
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

        if (usedSymbols.contains('#')) {
            keyMap.put('#', baseIngredient);
        }

        for (Character c : usedSymbols) {
            if (c == '#') continue;

            String mapping = rule.keys().get(c);
            if (mapping == null) {
                Orehall.LOGGER.warn("Custom Recipe Replacer: Symbol '{}' used in pattern but missing key definition in rule '{}'", c, rule.id());
                return null;
            }

            Ingredient resolved = resolveIngredient(mapping, registries);
            if (resolved == null || resolved.isEmpty()) {
                Orehall.LOGGER.warn("Custom Recipe Replacer: Could not resolve ingredient '{}' for key '{}' in rule '{}'", mapping, c, rule.id());
                return null;
            }
            keyMap.put(c, resolved);
        }

        ShapedRecipePattern pattern = ShapedRecipePattern.of(keyMap, rule.pattern());
        return new ShapedRecipe(group, category, pattern, output);
    }

    private static ShapelessRecipe createShapelessRecipe(CustomReplacerRule rule,
                                                         Ingredient baseIngredient,
                                                         ItemStack output,
                                                         String group,
                                                         CraftingBookCategory category,
                                                         HolderLookup.Provider registries) {
        NonNullList<Ingredient> ingredients = NonNullList.create();

        for (String token : rule.shapelessIngredients()) {
            if (token.equals("#")) {
                ingredients.add(baseIngredient);
            } else {
                Ingredient resolved = resolveIngredient(token, registries);
                if (resolved == null || resolved.isEmpty()) {
                    Orehall.LOGGER.warn("Custom Recipe Replacer: Could not resolve shapeless ingredient '{}' in rule '{}'", token, rule.id());
                    return null;
                }
                ingredients.add(resolved);
            }
        }

        return new ShapelessRecipe(group, category, output, ingredients);
    }

    private static Ingredient extractBaseIngredient(List<Ingredient> ingredients, CustomReplacerRule rule, ICondition.IContext context) {
        if (rule.keys() == null || rule.keys().isEmpty()) {
            return ingredients.get(0);
        }

        // Collect all secondary items and tags specified in keys (excluding '#')
        Set<Item> secondaryItems = new HashSet<>();
        List<TagKey<Item>> secondaryTags = new ArrayList<>();

        for (Map.Entry<Character, String> entry : rule.keys().entrySet()) {
            if (entry.getKey() == '#') continue;
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

        // Find the first ingredient that does not match any secondary key
        for (Ingredient ing : ingredients) {
            boolean isSecondary = false;
            for (Item sec : secondaryItems) {
                if (ing.test(sec.getDefaultInstance())) {
                    isSecondary = true;
                    break;
                }
            }
            if (!isSecondary && !secondaryTags.isEmpty()) {
                for (TagKey<Item> tag : secondaryTags) {
                    for (ItemStack stack : ing.getItems()) {
                        if (stack.is(tag)) {
                            isSecondary = true;
                            break;
                        }
                    }
                    if (isSecondary) break;
                }
            }
            if (!isSecondary) {
                return ing;
            }
        }

        return ingredients.get(0);
    }

    public static boolean matchesTarget(ItemStack stack, String target, ICondition.IContext context) {
        if (target == null || target.isBlank()) {
            return false;
        }

        // Support multiple comma-separated targets (e.g. "#minecraft:buttons,*_button")
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

            // Strategy 1: ConditionContext tag lookup (active datapack TagManager results)
            if (context != null) {
                try {
                    Collection<Holder<Item>> holders = context.getTag(tag);
                    if (holders != null && !holders.isEmpty()) {
                        Holder<Item> itemHolder = BuiltInRegistries.ITEM.wrapAsHolder(item);
                        if (holders.contains(itemHolder)) {
                            return true;
                        }
                        for (Holder<Item> h : holders) {
                            if (h.value() == item || (h.unwrapKey().isPresent() && h.unwrapKey().get().location().equals(itemKey))) {
                                return true;
                            }
                        }
                    }
                } catch (Throwable ignored) {
                    // TagManager not yet populated or context unavailable
                }
            }

            // Strategy 2: Native ItemStack.is(tag) (if tags are already bound)
            try {
                if (stack.is(tag)) {
                    return true;
                }
            } catch (Throwable ignored) {
            }

            // Strategy 3: Standard naming convention and semantic fallback for tag families
            return matchesTagHeuristic(path, tagLoc.getPath().toLowerCase(Locale.ROOT));
        }

        String targetLower = target.toLowerCase(Locale.ROOT);

        // Wildcard glob matching (e.g. "*_button", "*pressure_plate*", "minecraft:*_door", "*_trapdoors")
        if (target.contains("*")) {
            if (matchesWildcard(targetLower, path, fullId)) {
                return true;
            }
            // Auto-singularize pluralized wildcards (e.g. "*_trapdoors" -> "*_trapdoor", "*_boxes" -> "*_box")
            if (targetLower.endsWith("es") && targetLower.length() > 3) {
                String singularEs = targetLower.substring(0, targetLower.length() - 2);
                if (matchesWildcard(singularEs, path, fullId) || matchesWildcard(singularEs + "e", path, fullId)) {
                    return true;
                }
            }
            if (targetLower.endsWith("s") && targetLower.length() > 2) {
                String singularS = targetLower.substring(0, targetLower.length() - 1);
                if (matchesWildcard(singularS, path, fullId)) {
                    return true;
                }
            }
            return false;
        }

        // Exact match against path or full namespaced ID (e.g. "minecraft:oak_trapdoor" or "oak_trapdoor")
        if (fullId.equals(targetLower) || path.equals(targetLower)) {
            return true;
        }

        // Auto-singularize exact item ID (e.g. "oak_trapdoors" -> "oak_trapdoor")
        if (targetLower.endsWith("s") && targetLower.length() > 2) {
            String singular = targetLower.substring(0, targetLower.length() - 1);
            if (fullId.equals(singular) || path.equals(singular)) {
                return true;
            }
        }

        // Implicit tag fallback if user omitted '#' (e.g. "minecraft:trapdoors" or "trapdoors")
        return matchesSingleTarget(stack, "#" + targetLower, context);
    }

    private static final Map<String, Pattern> WILDCARD_PATTERN_CACHE = new ConcurrentHashMap<>();

    private static Pattern getOrCreateWildcardPattern(String glob) {
        return WILDCARD_PATTERN_CACHE.computeIfAbsent(glob, g -> {
            String regex = "^" + Pattern.quote(g).replace("*", "\\E.*\\Q") + "$";
            return Pattern.compile(regex);
        });
    }

    private static boolean matchesWildcard(String glob, String path, String fullId) {
        Pattern pattern = getOrCreateWildcardPattern(glob);
        return pattern.matcher(path).matches() || pattern.matcher(fullId).matches();
    }

    private static boolean matchesTagHeuristic(String itemPath, String tagPath) {
        // Buttons
        if (tagPath.equals("buttons") || tagPath.endsWith("/buttons")) {
            return itemPath.endsWith("_button") || itemPath.equals("button");
        }
        if (tagPath.equals("stone_buttons")) {
            return itemPath.contains("stone_button") || itemPath.contains("blackstone_button") || itemPath.contains("deepslate_button");
        }
        if (tagPath.equals("wooden_buttons")) {
            return (itemPath.endsWith("_button") || itemPath.equals("button"))
                    && !itemPath.contains("stone") && !itemPath.contains("blackstone") && !itemPath.contains("deepslate");
        }

        // Pressure plates
        if (tagPath.equals("wooden_pressure_plates")) {
            return itemPath.endsWith("_pressure_plate")
                    && !itemPath.contains("iron") && !itemPath.contains("heavy")
                    && !itemPath.contains("gold") && !itemPath.contains("light")
                    && !itemPath.contains("stone") && !itemPath.contains("blackstone");
        }
        if (tagPath.equals("stone_pressure_plates")) {
            return itemPath.contains("stone_pressure_plate") || itemPath.contains("blackstone_pressure_plate");
        }
        if (tagPath.equals("pressure_plates") || tagPath.endsWith("/pressure_plates")) {
            return itemPath.endsWith("_pressure_plate");
        }

        // Slabs
        if (tagPath.equals("slabs") || tagPath.endsWith("/slabs") || tagPath.equals("wooden_slabs")) {
            return itemPath.endsWith("_slab");
        }

        // Stairs
        if (tagPath.equals("stairs") || tagPath.endsWith("/stairs") || tagPath.equals("wooden_stairs")) {
            return itemPath.endsWith("_stairs");
        }

        // Doors & Trapdoors
        if (tagPath.equals("doors") || tagPath.endsWith("/doors") || tagPath.equals("wooden_doors")) {
            return itemPath.endsWith("_door");
        }
        if (tagPath.equals("trapdoors") || tagPath.endsWith("/trapdoors") || tagPath.equals("wooden_trapdoors")) {
            return itemPath.endsWith("_trapdoor");
        }

        // Fences & Gates & Walls
        if (tagPath.equals("fences") || tagPath.endsWith("/fences") || tagPath.equals("wooden_fences")) {
            return itemPath.endsWith("_fence");
        }
        if (tagPath.equals("fence_gates") || tagPath.endsWith("/fence_gates")) {
            return itemPath.endsWith("_fence_gate");
        }
        if (tagPath.equals("walls") || tagPath.endsWith("/walls")) {
            return itemPath.endsWith("_wall");
        }

        // Signs & Hanging Signs
        if (tagPath.equals("signs") || tagPath.endsWith("/signs")) {
            return itemPath.endsWith("_sign") && !itemPath.endsWith("_hanging_sign");
        }
        if (tagPath.equals("hanging_signs") || tagPath.endsWith("/hanging_signs")) {
            return itemPath.endsWith("_hanging_sign");
        }

        // Boats
        if (tagPath.equals("boats") || tagPath.endsWith("/boats")) {
            return itemPath.endsWith("_boat");
        }

        // Generic plural fallback (e.g. tag "torches" -> item ending in "_torch" or "torch")
        if (tagPath.endsWith("es") && tagPath.length() > 3) {
            String root = tagPath.substring(0, tagPath.length() - 2);
            if (itemPath.endsWith("_" + root) || itemPath.equals(root)
                    || itemPath.endsWith("_" + root + "e") || itemPath.equals(root + "e")) {
                return true;
            }
        }
        if (tagPath.endsWith("s") && tagPath.length() > 2) {
            String root = tagPath.substring(0, tagPath.length() - 1);
            if (itemPath.endsWith("_" + root) || itemPath.equals(root)) {
                return true;
            }
        }

        return false;
    }

    private static boolean containsExcludedIngredient(List<Ingredient> ingredients, String exclude, ICondition.IContext context) {
        if (exclude == null || exclude.isBlank()) {
            return false;
        }

        String[] tokens = exclude.split(",");
        for (String token : tokens) {
            String trimmed = token.trim();
            if (trimmed.isEmpty()) continue;

            if (trimmed.startsWith("#")) {
                ResourceLocation tagLoc = ResourceLocation.tryParse(trimmed.substring(1));
                if (tagLoc != null) {
                    TagKey<Item> tag = TagKey.create(Registries.ITEM, tagLoc);
                    Set<Item> tagItems = new HashSet<>();
                    if (context != null) {
                        try {
                            Collection<Holder<Item>> holders = context.getTag(tag);
                            if (holders != null) {
                                for (Holder<Item> h : holders) {
                                    tagItems.add(h.value());
                                }
                            }
                        } catch (Throwable ignored) {
                        }
                    }

                    for (Ingredient ing : ingredients) {
                        for (Item it : tagItems) {
                            if (ing.test(it.getDefaultInstance())) {
                                return true;
                            }
                        }
                        for (ItemStack stack : ing.getItems()) {
                            if (matchesSingleTarget(stack, trimmed, context)) {
                                return true;
                            }
                        }
                    }
                }
            } else {
                ResourceLocation itemLoc = ResourceLocation.tryParse(trimmed);
                if (itemLoc != null && BuiltInRegistries.ITEM.containsKey(itemLoc)) {
                    Item item = BuiltInRegistries.ITEM.get(itemLoc);
                    ItemStack testStack = item.getDefaultInstance();
                    if (ingredients.stream().anyMatch(ing -> ing.test(testStack))) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static Ingredient resolveIngredient(String idOrTag, HolderLookup.Provider registries) {
        if (idOrTag.startsWith("#")) {
            ResourceLocation tagLoc = ResourceLocation.tryParse(idOrTag.substring(1));
            if (tagLoc == null) {
                return null;
            }
            TagKey<Item> tag = TagKey.create(Registries.ITEM, tagLoc);
            return Ingredient.of(tag);
        } else {
            ResourceLocation itemLoc = ResourceLocation.tryParse(idOrTag);
            if (itemLoc == null || !BuiltInRegistries.ITEM.containsKey(itemLoc)) {
                return null;
            }
            Item item = BuiltInRegistries.ITEM.get(itemLoc);
            return Ingredient.of(item);
        }
    }
}
