package com.orehall.module.recipetweaks.dynamic;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import com.orehall.Orehall;

public class CustomReplacerParser {

    /**
     * Parses a raw config rule string into a validated {@link CustomReplacerRule}.
     *
     * @param rawRule The raw configuration directive string.
     * @param index   Fallback index used if no explicit id is specified.
     * @return An Optional containing the parsed rule, or empty if syntax is invalid.
     */
    public static Optional<CustomReplacerRule> parse(String rawRule, int index) {
        if (rawRule == null || rawRule.isBlank()) {
            return Optional.empty();
        }

        String id = "custom_" + index;
        boolean enabled = true;
        String target = null;
        Integer inputCount = null;
        String exclude = null;
        String patternRaw = null;
        String ingredientsRaw = null;
        String keysRaw = null;
        String outputRaw = null;
        String group = null;

        String[] parts = rawRule.split(";");
        for (String part : parts) {
            String trimmed = part.trim();
            if (trimmed.isEmpty()) {
                continue;
            }

            int eqIndex = trimmed.indexOf('=');
            if (eqIndex == -1) {
                Orehall.LOGGER.warn("Custom Recipe Replacer: Skipping directive with missing '=': '{}' in rule: '{}'", trimmed, rawRule);
                continue;
            }

            String key = trimmed.substring(0, eqIndex).trim().toLowerCase(Locale.ROOT);
            String value = trimmed.substring(eqIndex + 1).trim();

            switch (key) {
                case "id", "name" -> id = value;
                case "enabled", "enable" -> enabled = Boolean.parseBoolean(value);
                case "target" -> target = value;
                case "input_count", "input", "inputs", "count" -> {
                    try {
                        inputCount = Integer.parseInt(value);
                    } catch (NumberFormatException e) {
                        Orehall.LOGGER.warn("Custom Recipe Replacer: Invalid input count '{}' in rule: '{}'", value, rawRule);
                        return Optional.empty();
                    }
                }
                case "exclude" -> exclude = value;
                case "pattern", "shape" -> patternRaw = value;
                case "ingredients", "shapeless" -> ingredientsRaw = value;
                case "keys", "key" -> keysRaw = value;
                case "output", "output_count", "yield", "count_out" -> outputRaw = value;
                case "group" -> group = value;
                default -> Orehall.LOGGER.warn("Custom Recipe Replacer: Unknown directive '{}' in rule: '{}'", key, rawRule);
            }
        }

        if (target == null || target.isBlank()) {
            Orehall.LOGGER.warn("Custom Recipe Replacer: Missing required 'target' directive in rule: '{}'", rawRule);
            return Optional.empty();
        }

        boolean isShaped = patternRaw != null && !patternRaw.isBlank();
        boolean isShapeless = ingredientsRaw != null && !ingredientsRaw.isBlank();

        if (!isShaped && !isShapeless) {
            Orehall.LOGGER.warn("Custom Recipe Replacer: Rule must define either 'pattern' (shaped) or 'ingredients' (shapeless) in rule: '{}'", rawRule);
            return Optional.empty();
        }

        List<String> patternRows = new ArrayList<>();
        Map<Character, String> keys = new HashMap<>();
        List<String> shapelessIngredients = new ArrayList<>();

        if (isShaped) {
            String[] rowTokens = patternRaw.split("/");
            if (rowTokens.length < 1 || rowTokens.length > 3) {
                Orehall.LOGGER.warn("Custom Recipe Replacer: Pattern row count must be between 1 and 3 (found {}): '{}'", rowTokens.length, rawRule);
                return Optional.empty();
            }

            int expectedWidth = -1;
            for (String token : rowTokens) {
                // Normalize empty slot tokens '.' and '_' to standard space ' '
                String cleanRow = token.replace('.', ' ').replace('_', ' ');
                if (cleanRow.length() < 1 || cleanRow.length() > 3) {
                    Orehall.LOGGER.warn("Custom Recipe Replacer: Pattern row width must be between 1 and 3 characters (found {} in '{}'): '{}'",
                            cleanRow.length(), token, rawRule);
                    return Optional.empty();
                }
                if (expectedWidth == -1) {
                    expectedWidth = cleanRow.length();
                } else if (cleanRow.length() != expectedWidth) {
                    Orehall.LOGGER.warn("Custom Recipe Replacer: Inconsistent pattern row widths (expected {}, found {} in '{}'): '{}'",
                            expectedWidth, cleanRow.length(), token, rawRule);
                    return Optional.empty();
                }
                patternRows.add(cleanRow);
            }

            // Parse key bindings (e.g. "R:minecraft:redstone,I:#c:iron_ingots" or "R=minecraft:redstone")
            if (keysRaw != null && !keysRaw.isBlank()) {
                String[] keyPairs = keysRaw.split(",");
                for (String pair : keyPairs) {
                    String pTrimmed = pair.trim();
                    if (pTrimmed.isEmpty()) continue;

                    int sep = pTrimmed.indexOf(':');
                    if (sep == -1) {
                        sep = pTrimmed.indexOf('=');
                    }
                    if (sep == -1 || sep == 0 || sep >= pTrimmed.length() - 1) {
                        Orehall.LOGGER.warn("Custom Recipe Replacer: Invalid key mapping '{}' in rule: '{}'", pTrimmed, rawRule);
                        return Optional.empty();
                    }

                    char symbol = pTrimmed.charAt(0);
                    String itemOrTag = pTrimmed.substring(sep + 1).trim();
                    keys.put(symbol, itemOrTag);
                }
            }
        } else {
            // Parse shapeless ingredients (e.g. "#,minecraft:redstone")
            String[] ingredientTokens = ingredientsRaw.split(",");
            for (String token : ingredientTokens) {
                String t = token.trim();
                if (!t.isEmpty()) {
                    shapelessIngredients.add(t);
                }
            }
            if (shapelessIngredients.isEmpty()) {
                Orehall.LOGGER.warn("Custom Recipe Replacer: Shapeless ingredients list cannot be empty in rule: '{}'", rawRule);
                return Optional.empty();
            }
        }

        // Parse output
        Integer outputFixed = null;
        Integer outputMultiplier = null;
        if (outputRaw != null && !outputRaw.isBlank()) {
            String oTrimmed = outputRaw.trim();
            if (oTrimmed.endsWith("x") || oTrimmed.endsWith("X")) {
                try {
                    outputMultiplier = Integer.parseInt(oTrimmed.substring(0, oTrimmed.length() - 1).trim());
                } catch (NumberFormatException e) {
                    Orehall.LOGGER.warn("Custom Recipe Replacer: Invalid output multiplier '{}' in rule: '{}'", outputRaw, rawRule);
                    return Optional.empty();
                }
            } else {
                try {
                    outputFixed = Integer.parseInt(oTrimmed);
                } catch (NumberFormatException e) {
                    Orehall.LOGGER.warn("Custom Recipe Replacer: Invalid output count '{}' in rule: '{}'", outputRaw, rawRule);
                    return Optional.empty();
                }
            }
        }

        return Optional.of(new CustomReplacerRule(
                id,
                enabled,
                target,
                inputCount,
                exclude,
                isShaped,
                patternRows,
                shapelessIngredients,
                keys,
                outputFixed,
                outputMultiplier,
                group,
                rawRule
        ));
    }
}
