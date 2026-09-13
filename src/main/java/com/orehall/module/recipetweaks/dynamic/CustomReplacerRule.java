package com.orehall.module.recipetweaks.dynamic;

import java.util.List;
import java.util.Map;

/**
 * Immutable data structure representing a parsed user-defined dynamic recipe replacer rule.
 */
public record CustomReplacerRule(
        String id,
        boolean enabled,
        String target,
        String hasIngredient,
        String recipeId,
        Integer inputCount,
        String exclude,
        String swapFrom,
        String swapTo,
        boolean isShaped,
        boolean isShapeless,
        List<String> pattern,
        List<String> shapelessIngredients,
        Map<Character, String> keys,
        Integer outputFixed,
        Integer outputMultiplier,
        String group,
        String rawRule
) {
    public boolean isSwap() {
        return swapFrom != null && !swapFrom.isBlank() && swapTo != null && !swapTo.isBlank();
    }

    public int calculateOutputCount(int originalCount) {
        if (outputFixed != null) {
            return Math.max(1, Math.min(64, outputFixed));
        }
        if (outputMultiplier != null) {
            return Math.max(1, Math.min(64, originalCount * outputMultiplier));
        }
        return originalCount;
    }
}
