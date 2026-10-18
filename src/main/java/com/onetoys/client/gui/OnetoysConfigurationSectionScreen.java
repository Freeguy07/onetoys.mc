package com.onetoys.client.gui;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;

import com.electronwill.nightconfig.core.UnmodifiableConfig;
import com.electronwill.nightconfig.core.UnmodifiableConfig.Entry;
import com.onetoys.Onetoys;
import com.onetoys.module.ModuleManager;
import com.onetoys.module.gameplay.GameplayTweaksModule;
import com.onetoys.module.gameplay.stack.StackTweaks;
import com.onetoys.registry.OnetoysBlocks;
import com.onetoys.registry.OnetoysItems;
import com.onetoys.salt.SaltItems;

import net.minecraft.Util;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.ConfigurationScreen.ConfigurationSectionScreen;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.common.ModConfigSpec.ConfigValue;
import net.neoforged.neoforge.common.ModConfigSpec.ListValueSpec;
import net.neoforged.neoforge.common.ModConfigSpec.ValueSpec;

import org.jetbrains.annotations.Nullable;

/**
 * Custom configuration section screen for Onetoys.
 * Provides hierarchical configuration translation key resolution, customized section buttons,
 * full-width big toggle buttons for module master switches, automatic dependent option graying out,
 * real-time search filtering, and contextual vanilla category item icons.
 */
public class OnetoysConfigurationSectionScreen extends ConfigurationSectionScreen {

    private final List<AbstractWidget> dependentWidgets = new ArrayList<>();
    private final @Nullable String sectionKey;
    private String searchQuery = "";
    private @Nullable EditBox searchBox;

    public OnetoysConfigurationSectionScreen(Screen parent, ModConfig.Type type, ModConfig modConfig, Component title) {
        super(parent, type, modConfig, title);
        this.sectionKey = null;
    }

    public OnetoysConfigurationSectionScreen(Screen parent, ModConfig.Type type, ModConfig modConfig, Component title, Filter filter) {
        super(parent, type, modConfig, title, filter);
        this.sectionKey = null;
    }

    public OnetoysConfigurationSectionScreen(Context parentContext, Screen parent, Map<String, Object> valueSpecs, String key, Set<? extends Entry> entrySet, Component title) {
        super(parentContext, parent, valueSpecs, key, entrySet, title);
        this.sectionKey = key;
    }

    protected OnetoysConfigurationSectionScreen(Context context, Component title) {
        super(context, title);
        this.sectionKey = null;
    }

    private String getEffectiveSectionKey() {
        if (sectionKey != null) {
            return sectionKey;
        }
        if (context != null && !context.keylist().isEmpty()) {
            return context.keylist().get(context.keylist().size() - 1);
        }
        return "onetoys";
    }

    @Override
    protected void addTitle() {
        layout.setHeaderHeight(58);
        net.minecraft.client.gui.layouts.LinearLayout headerLayout = net.minecraft.client.gui.layouts.LinearLayout.vertical().spacing(4);
        headerLayout.defaultCellSetting().alignHorizontallyCenter();

        ItemStack headerIcon = getIconForKey(getEffectiveSectionKey());
        if (!headerIcon.isEmpty()) {
            net.minecraft.client.gui.layouts.LinearLayout titleRow = net.minecraft.client.gui.layouts.LinearLayout.horizontal().spacing(6);
            titleRow.defaultCellSetting().alignVerticallyMiddle();
            titleRow.addChild(new IconWidget(16, 16, headerIcon));
            titleRow.addChild(new StringWidget(this.title, this.font));
            headerLayout.addChild(titleRow);
        } else {
            headerLayout.addChild(new StringWidget(this.title, this.font));
        }

        // Search bar row with clear button (310px unified geometry: 286px edit box + 4px gap + 20px clear button = 310px)
        net.minecraft.client.gui.layouts.LinearLayout searchRow = net.minecraft.client.gui.layouts.LinearLayout.horizontal().spacing(4);
        this.searchBox = new EditBox(this.font, 0, 0, 286, 18, Component.translatable("onetoys.configuration.search"));
        this.searchBox.setHint(Component.translatable("onetoys.configuration.search.hint").withColor(0x888888));
        this.searchBox.setValue(this.searchQuery);
        this.searchBox.setResponder(this::onSearchChanged);
        searchRow.addChild(this.searchBox);

        Button clearBtn = Button.builder(Component.literal("\u2715"), btn -> {
            if (this.searchBox != null) {
                this.searchBox.setValue("");
            }
        }).width(20).tooltip(Tooltip.create(Component.translatable("onetoys.configuration.search.clear"))).build();
        searchRow.addChild(clearBtn);

        headerLayout.addChild(searchRow);
        this.layout.addToHeader(headerLayout);
    }

    private void onSearchChanged(String query) {
        this.searchQuery = query != null ? query.trim().toLowerCase() : "";
        rebuild();
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256 && !searchQuery.isEmpty()) { // 256 = GLFW_KEY_ESCAPE
            if (searchBox != null) {
                searchBox.setValue("");
            }
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    public static ItemStack getIconForKey(@Nullable String key) {
        if (key == null) {
            return ItemStack.EMPTY;
        }
        return switch (key) {
            case "onetoys" -> new ItemStack(Items.RAW_IRON);

            case "recipe_tweaks" -> new ItemStack(Items.CRAFTING_TABLE);
            case "replacement_recipes" -> new ItemStack(Items.ANVIL);
            case "utility" -> new ItemStack(Items.CHEST);

            case "gameplay_tweaks" -> new ItemStack(Items.GOLDEN_APPLE);
            case "durability_tweaks" -> new ItemStack(Items.DIAMOND_PICKAXE);
            case "mob_tweaks" -> new ItemStack(Items.LEAD);
            case "ai_tweaks" -> new ItemStack(Items.ZOMBIE_HEAD);
            case "special_mobs" -> new ItemStack(Items.WITHER_SKELETON_SKULL);
            case "cow" -> new ItemStack(Items.MILK_BUCKET);
            case "goat" -> new ItemStack(Items.GOAT_HORN);
            case "mooshroom" -> new ItemStack(Items.MUSHROOM_STEW);
            case "item_tweaks" -> new ItemStack(Items.COMPASS);
            case "recovery_compass" -> new ItemStack(Items.RECOVERY_COMPASS);
            case "stopwatch" -> new ItemStack(OnetoysItems.STOPWATCH.get());
            case "sword_blocking" -> new ItemStack(Items.IRON_SWORD);
            case "sleeping_tweaks" -> new ItemStack(Items.RED_BED);
            case "respawn_anchor_required" -> new ItemStack(Items.RESPAWN_ANCHOR);
            case "stack_tweaks" -> new ItemStack(Items.BUNDLE);
            case "wetness", "wet", "wet_tweaks" -> new ItemStack(Items.WATER_BUCKET);

            case "worldgen", "worldgen_tweaks" -> new ItemStack(Items.GRASS_BLOCK);
            case "terrain_tweaks" -> new ItemStack(Items.STONE);
            case "height_weather" -> new ItemStack(Items.SNOWBALL);
            case "ore_tweaks" -> new ItemStack(Items.DIAMOND_ORE);
            case "giant_ore_veins_disabler" -> new ItemStack(Items.RAW_COPPER);

            case "content" -> new ItemStack(OnetoysBlocks.TUNGSTEN_ORE.get());
            case "tungsten_tweaks" -> new ItemStack(OnetoysItems.TUNGSTEN_INGOT.get());
            case "nuggets_and_shards" -> new ItemStack(OnetoysItems.COPPER_NUGGET.get());

            case "food_overhaul" -> new ItemStack(Items.COOKED_BEEF);
            case "General" -> new ItemStack(Items.BREAD);
            case "salt", "Salting" -> new ItemStack(SaltItems.SALT.get());
            case "Sweetening" -> new ItemStack(Items.SUGAR);
            case "Satiating" -> new ItemStack(Onetoys.CHRYSANTHEMUM.get());
            case "Dissolving" -> new ItemStack(Items.WATER_BUCKET);
            case "Melting" -> new ItemStack(Items.ICE);
            case "Evaporation" -> new ItemStack(Items.CAULDRON);
            case "GrowingSaltClusters" -> new ItemStack(SaltItems.SALT_CLUSTER.get());
            case "RockSalt" -> new ItemStack(SaltItems.ROCK_SALT_ORE.get());
            case "VillagerTrades" -> new ItemStack(Items.EMERALD);
            case "Compatibility", "HUD" -> new ItemStack(Items.APPLE);

            default -> ItemStack.EMPTY;
        };
    }

    @Override
    protected String getTranslationKey(final String key) {
        final ValueSpec valueSpec = getValueSpec(key);
        if (valueSpec != null && valueSpec.getTranslationKey() != null) {
            return valueSpec.getTranslationKey();
        }

        List<String> keylist = context.keylist();
        List<String> fullPath = new ArrayList<>(keylist);
        fullPath.add(key);

        String levelKey = context.modSpec().getLevelTranslationKey(fullPath);
        if (levelKey != null && I18n.exists(levelKey)) {
            return levelKey;
        }

        StringBuilder hierarchical = new StringBuilder(context.modId()).append(".configuration");
        for (String seg : keylist) {
            hierarchical.append('.').append(seg);
        }
        hierarchical.append('.').append(key);
        String hierarchicalKey = hierarchical.toString();

        if (I18n.exists(hierarchicalKey)) {
            return hierarchicalKey;
        }

        String flatKey = context.modId() + ".configuration." + key;
        if (I18n.exists(flatKey)) {
            return flatKey;
        }

        if (levelKey != null) {
            return levelKey;
        }

        return hierarchicalKey;
    }

    @Override
    @SuppressWarnings("deprecation")
    protected Element createSection(String key, UnmodifiableConfig subconfig, UnmodifiableConfig subsection) {
        if (subconfig.isEmpty()) return null;

        Component buttonLabel = Component.translatable("neoforge.configuration.uitext.section",
                Component.translatable("neoforge.configuration.uitext.sectiontext"));
        Component sectionTitle = Component.translatable(getTranslationKey(key));

        return new Element(
                getTranslationComponent(key),
                getTooltipComponent(key, null),
                Button.builder(buttonLabel, button -> {
                    if (minecraft != null) {
                        OnetoysConfigurationSectionScreen screen = (OnetoysConfigurationSectionScreen) sectionCache.computeIfAbsent(key,
                                k -> new OnetoysConfigurationSectionScreen(context, this, subconfig.valueMap(), key, subsection.entrySet(), sectionTitle));
                        minecraft.setScreen(screen.rebuild());
                    }
                })
                .tooltip(Tooltip.create(getTooltipComponent(key, null)))
                .width(Button.DEFAULT_WIDTH)
                .build(),
                false
        );
    }

    @Nullable
    @SuppressWarnings("unchecked")
    private CycleButton<Boolean> createBigEnabledButton(String key, Entry entry) {
        Supplier<Boolean> source;
        Consumer<Boolean> target;

        if (entry.getRawValue() instanceof ModConfigSpec.BooleanValue bv) {
            source = bv::getRaw;
            target = bv::set;
        } else if (entry.getRawValue() instanceof ConfigValue<?> cv && cv.getRaw() instanceof Boolean) {
            source = () -> (Boolean) cv.getRaw();
            target = val -> ((ConfigValue<Boolean>) cv).set(val);
        } else {
            return null;
        }

        boolean initialValue = Boolean.TRUE.equals(source.get());
        Component buttonCaption = getTranslationComponent(key);

        return CycleButton.<Boolean>onOffBuilder()
                .withInitialValue(initialValue)
                .withTooltip(val -> Tooltip.create(getTooltipComponent(key, null)))
                .create(0, 0, ConfigurationScreen.BIG_BUTTON_WIDTH, Button.DEFAULT_HEIGHT,
                        buttonCaption,
                        (cycleButton, newValue) -> {
                            boolean previousValue = Boolean.TRUE.equals(source.get());
                            undoManager.add(v -> {
                                target.accept(v);
                                onChanged(key);
                                setUndoButtonstate(true);
                                updateDependentWidgets(v);
                            }, newValue, v -> {
                                target.accept(v);
                                onChanged(key);
                                setUndoButtonstate(true);
                                updateDependentWidgets(v);
                            }, previousValue);

                            target.accept(newValue);
                            onChanged(key);
                            setUndoButtonstate(true);
                            updateDependentWidgets(newValue);
                        });
    }

    private void updateDependentWidgets(boolean active) {
        for (AbstractWidget widget : dependentWidgets) {
            widget.active = active;
        }
    }

    @Override
    protected void createUndoButton() {
        super.createUndoButton();
        if (undoButton != null) {
            undoButton.setWidth(98);
        }
    }

    @Override
    protected void addFooter() {
        if (undoButton == null) {
            createUndoButton();
        } else {
            undoButton.setWidth(98);
        }
        if (resetButton == null) {
            createResetButton();
        } else {
            resetButton.setWidth(98);
        }
        if (doneButton != null) {
            doneButton.setWidth(98);
        }
        net.minecraft.client.gui.layouts.LinearLayout linearlayout = layout.addToFooter(net.minecraft.client.gui.layouts.LinearLayout.horizontal().spacing(8));
        linearlayout.addChild(undoButton);
        linearlayout.addChild(resetButton);
        linearlayout.addChild(doneButton);
    }

    @Override
    protected boolean isAnyNondefault() {
        return isNonDefaultRecursive(context.entries());
    }

    private boolean isNonDefaultRecursive(Set<? extends Entry> entries) {
        for (Entry entry : entries) {
            Object rawValue = entry.getRawValue();
            if (rawValue instanceof ConfigValue<?> cv) {
                if (isNonDefault(cv)) {
                    return true;
                }
            } else if (rawValue instanceof UnmodifiableConfig subconfig) {
                if (isNonDefaultRecursive(subconfig.entrySet())) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    @SuppressWarnings({ "unchecked", "rawtypes" })
    protected void createResetButton() {
        Component resetLabel = Component.translatable("onetoys.configuration.reset");
        Component resetTooltip = Component.translatable("onetoys.configuration.reset.tooltip");

        resetButton = Button.builder(resetLabel, button -> {
            List<ConfigurationScreen.UndoManager.Step<?>> steps = new ArrayList<>();
            collectResetSteps(context.entries(), steps);
            if (!steps.isEmpty()) {
                undoManager.add(steps);
                changed = true;
                rebuild();
                setUndoButtonstate(true);
            }
        }).tooltip(Tooltip.create(resetTooltip)).width(98).build();
        resetButton.active = isAnyNondefault();
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private void collectResetSteps(Set<? extends Entry> entries, List<ConfigurationScreen.UndoManager.Step<?>> steps) {
        for (Entry entry : entries) {
            Object rawValue = entry.getRawValue();
            if (rawValue instanceof ConfigValue cv) {
                if (isNonDefault(cv)) {
                    ValueSpec spec = cv.getSpec();
                    Object defaultVal = spec != null ? spec.correct(null) : cv.getDefault();
                    Object currentVal = cv.getRaw();
                    String key = entry.getKey();

                    Object targetDefault = defaultVal instanceof List<?> list ? new ArrayList<>(list) : defaultVal;
                    Object targetCurrent = currentVal instanceof List<?> list ? new ArrayList<>(list) : currentVal;

                    steps.add(undoManager.step(v -> {
                        cv.set(v);
                        onChanged(key);
                    }, targetDefault, v -> {
                        cv.set(v);
                        onChanged(key);
                    }, targetCurrent));
                }
            } else if (rawValue instanceof UnmodifiableConfig subconfig) {
                collectResetSteps(subconfig.entrySet(), steps);
            }
        }
    }

    private boolean matchesSearch(@Nullable String key, @Nullable Element element, String query) {
        if (query == null || query.isBlank()) {
            return true;
        }
        if (key != null && key.toLowerCase().contains(query)) {
            return true;
        }
        if (element != null) {
            if (element.name() != null && element.name().getString().toLowerCase().contains(query)) {
                return true;
            }
            if (element.tooltip() != null && element.tooltip().getString().toLowerCase().contains(query)) {
                return true;
            }
        }
        return false;
    }

    private record ConfigItem(@Nullable String key, @Nullable Element element) {}

    @Override
    @SuppressWarnings({ "unchecked", "rawtypes" })
    protected ConfigurationSectionScreen rebuild() {
        if (list == null) {
            return this;
        }

        list.children().clear();
        dependentWidgets.clear();

        Entry enabledEntry = null;
        for (final Entry entry : context.entries()) {
            if ("enabled".equals(entry.getKey())) {
                enabledEntry = entry;
                break;
            }
        }

        boolean isSectionEnabled = true;
        boolean masterMatches = false;
        if (enabledEntry != null) {
            CycleButton<Boolean> bigButton = createBigEnabledButton("enabled", enabledEntry);
            if (bigButton != null) {
                if (enabledEntry.getRawValue() instanceof ConfigValue<?> cv && cv.getRaw() instanceof Boolean b) {
                    isSectionEnabled = b;
                }
                masterMatches = searchQuery.isEmpty()
                        || "enabled".contains(searchQuery)
                        || bigButton.getMessage().getString().toLowerCase().contains(searchQuery);
                if (masterMatches) {
                    list.addSmall(bigButton, null);
                }
            }
        }

        // Dedicated external configuration file button (durability.toml, stacks.toml)
        String effectiveSection = getEffectiveSectionKey();
        Path openFilePath = null;
        Component openFileLabel = null;
        Component openFileTooltip = null;

        if ("durability_tweaks".equals(effectiveSection)) {
            openFilePath = FMLPaths.CONFIGDIR.get().resolve("onetoys").resolve("durability.toml");
            openFileLabel = Component.translatable("onetoys.configuration.gameplay_tweaks.durability_tweaks.open_file");
            openFileTooltip = Component.translatable("onetoys.configuration.gameplay_tweaks.durability_tweaks.open_file.tooltip");
        } else if ("stack_tweaks".equals(effectiveSection)) {
            openFilePath = FMLPaths.CONFIGDIR.get().resolve("onetoys").resolve("stacks.toml");
            openFileLabel = Component.translatable("onetoys.configuration.gameplay_tweaks.stack_tweaks.open_file");
            openFileTooltip = Component.translatable("onetoys.configuration.gameplay_tweaks.stack_tweaks.open_file.tooltip");
        }

        boolean openBtnMatches = false;
        if (openFilePath != null) {
            openBtnMatches = searchQuery.isEmpty()
                    || openFileLabel.getString().toLowerCase().contains(searchQuery)
                    || openFilePath.getFileName().toString().toLowerCase().contains(searchQuery)
                    || "open".contains(searchQuery)
                    || "toml".contains(searchQuery)
                    || "config".contains(searchQuery);

            if (openBtnMatches) {
                Path targetPath = openFilePath;
                ConfigFileButton openBtn = new ConfigFileButton(
                        ConfigurationScreen.BIG_BUTTON_WIDTH,
                        Button.DEFAULT_HEIGHT,
                        openFileLabel,
                        Tooltip.create(openFileTooltip),
                        () -> openInExternalEditor(targetPath)
                );
                openBtn.active = isSectionEnabled;
                dependentWidgets.add(openBtn);
                list.addSmall(openBtn, null);
            }
        }

        final List<ConfigItem> items = new ArrayList<>();
        for (final Entry entry : context.entries()) {
            final String key = entry.getKey();
            if ("enabled".equals(key) && enabledEntry != null) {
                // Already rendered as the big master button at the top
                continue;
            }

            final Object rawValue = entry.getRawValue();
            switch (entry.getRawValue()) {
                case ConfigValue cv -> {
                    var valueSpec = getValueSpec(key);
                    var element = switch (valueSpec) {
                        case ListValueSpec listValueSpec -> createList(key, listValueSpec, cv);
                        case ValueSpec spec when cv.getClass() == ConfigValue.class && spec.getDefault() instanceof String -> createStringValue(key, valueSpec::test, () -> (String) cv.getRaw(), cv::set);
                        case ValueSpec spec when cv.getClass() == ConfigValue.class && spec.getDefault() instanceof Integer -> createIntegerValue(key, valueSpec, () -> (Integer) cv.getRaw(), cv::set);
                        case ValueSpec spec when cv.getClass() == ConfigValue.class && spec.getDefault() instanceof Long -> createLongValue(key, valueSpec, () -> (Long) cv.getRaw(), cv::set);
                        case ValueSpec spec when cv.getClass() == ConfigValue.class && spec.getDefault() instanceof Double -> createDoubleValue(key, valueSpec, () -> (Double) cv.getRaw(), cv::set);
                        case ValueSpec spec when cv.getClass() == ConfigValue.class && spec.getDefault() instanceof Enum<?> -> createEnumValue(key, valueSpec, (Supplier) cv::getRaw, (Consumer) cv::set);
                        case null -> null;

                        default -> switch (cv) {
                            case ModConfigSpec.BooleanValue value -> createBooleanValue(key, valueSpec, value::getRaw, value::set);
                            case ModConfigSpec.IntValue value -> createIntegerValue(key, valueSpec, value::getRaw, value::set);
                            case ModConfigSpec.LongValue value -> createLongValue(key, valueSpec, value::getRaw, value::set);
                            case ModConfigSpec.DoubleValue value -> createDoubleValue(key, valueSpec, value::getRaw, value::set);
                            case ModConfigSpec.EnumValue value -> createEnumValue(key, valueSpec, (Supplier) value::getRaw, (Consumer) value::set);
                            default -> createOtherValue(key, cv);
                        };
                    };
                    if (context.filter() != null) {
                        element = context.filter().filterEntry(context, key, element);
                    }
                    items.add(new ConfigItem(key, element));
                }
                case UnmodifiableConfig subsection when context.valueSpecs().get(key) instanceof UnmodifiableConfig subconfig ->
                    items.add(new ConfigItem(key, createSection(key, subconfig, subsection)));
                default -> {
                    Element other = createOtherSection(key, rawValue);
                    if (context.filter() != null) {
                        other = context.filter().filterEntry(context, key, other);
                    }
                    items.add(new ConfigItem(key, other));
                }
            }
        }
        for (final Element synth : createSyntheticValues()) {
            items.add(new ConfigItem(null, synth));
        }

        boolean sectionHasIcons = false;
        for (final ConfigItem item : items) {
            if (item.element() != null && !getIconForKey(item.key()).isEmpty()) {
                sectionHasIcons = true;
                break;
            }
        }

        int matchedCount = 0;
        for (final ConfigItem item : items) {
            final Element element = item.element();
            final String key = item.key();
            if (element != null) {
                if (!matchesSearch(key, element, searchQuery)) {
                    continue;
                }
                matchedCount++;

                AbstractWidget widget = element.getWidget(options);
                widget.active = isSectionEnabled;
                dependentWidgets.add(widget);

                if (element.name() == null) {
                    list.addSmall(new StringWidget(Button.DEFAULT_WIDTH, Button.DEFAULT_HEIGHT, Component.empty(), font), widget);
                } else {
                    ItemStack icon = getIconForKey(key);
                    final AbstractWidget label = new IconLabelWidget(Button.DEFAULT_WIDTH, Button.DEFAULT_HEIGHT, element.name(), font, icon, sectionHasIcons);
                    int availableTextWidth = Button.DEFAULT_WIDTH - (sectionHasIcons ? 20 : 0);
                    if (font.width(element.name()) > availableTextWidth) {
                        if (element.tooltip() != null) {
                            label.setTooltip(Tooltip.create(
                                    Component.empty().append(element.name()).append("\n\n").append(element.tooltip())
                            ));
                        } else {
                            label.setTooltip(Tooltip.create(element.name()));
                        }
                    } else if (element.tooltip() != null) {
                        label.setTooltip(Tooltip.create(element.tooltip()));
                    }
                    label.active = isSectionEnabled;
                    dependentWidgets.add(label);
                    list.addSmall(label, widget);
                }
            }
        }

        if (matchedCount == 0 && !masterMatches && !openBtnMatches && !searchQuery.isEmpty()) {
            StringWidget noResultsWidget = new StringWidget(
                    ConfigurationScreen.BIG_BUTTON_WIDTH,
                    Button.DEFAULT_HEIGHT,
                    Component.translatable("onetoys.configuration.search.no_results", searchQuery),
                    font
            );
            list.addSmall(noResultsWidget, null);
        }

        if (undoButton == null) {
            createUndoButton();
        }
        if (resetButton == null) {
            createResetButton();
        }

        setResetButtonstate(isAnyNondefault());
        setUndoButtonstate(undoManager.canUndo());

        return this;
    }

    public static class IconLabelWidget extends AbstractWidget {
        private static final int ICON_SIZE = 16;
        private static final int ICON_SPACING = 4;
        private static final int ICON_OFFSET = ICON_SIZE + ICON_SPACING;

        private final Font font;
        private final ItemStack icon;
        private final boolean preserveIconSpace;

        public IconLabelWidget(int width, int height, Component message, Font font, ItemStack icon, boolean preserveIconSpace) {
            super(0, 0, width, height, message);
            this.font = font;
            this.icon = icon != null ? icon : ItemStack.EMPTY;
            this.preserveIconSpace = preserveIconSpace;
        }

        @Override
        protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
            int currentX = getX();
            int currentY = getY();

            if (!icon.isEmpty()) {
                int iconY = currentY + (height - ICON_SIZE) / 2;
                guiGraphics.renderFakeItem(icon, currentX, iconY);
                currentX += ICON_OFFSET;
            } else if (preserveIconSpace) {
                currentX += ICON_OFFSET;
            }

            int availableWidth = Math.max(0, getX() + width - currentX);
            int textY = currentY + (height - font.lineHeight) / 2;
            int color = active ? 0xFFFFFF : 0xA0A0A0;

            Component message = getMessage();
            if (font.width(message) <= availableWidth) {
                guiGraphics.drawString(font, message, currentX, textY, color);
            } else {
                renderScrollingString(guiGraphics, font, message, currentX, currentY, getX() + width, currentY + height, color);
            }
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
            defaultButtonNarrationText(narrationElementOutput);
        }
    }

    public static class IconWidget extends AbstractWidget {
        private final ItemStack icon;

        public IconWidget(int width, int height, ItemStack icon) {
            super(0, 0, width, height, Component.empty());
            this.icon = icon != null ? icon : ItemStack.EMPTY;
            this.active = false;
        }

        @Override
        protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
            if (!icon.isEmpty()) {
                guiGraphics.renderFakeItem(icon, getX(), getY());
            }
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {}
    }

    public static void openInExternalEditor(Path path) {
        try {
            if (!Files.exists(path)) {
                Files.createDirectories(path.getParent());
                if (path.getFileName().toString().equals("durability.toml")) {
                    Files.writeString(path, "# Onetoys - Durability Configuration\n[overrides]\n", StandardCharsets.UTF_8);
                } else if (path.getFileName().toString().equals("stacks.toml")) {
                    GameplayTweaksModule module = ModuleManager.get().getModule(GameplayTweaksModule.class).orElse(null);
                    if (module != null) {
                        module.getStackTweaks().ensureConfigFile();
                    } else {
                        Files.writeString(path, StackTweaks.DEFAULT_CONFIG_TEMPLATE, StandardCharsets.UTF_8);
                    }
                } else {
                    Files.writeString(path, "", StandardCharsets.UTF_8);
                }
            }

            if (Files.exists(path)) {
                Util.getPlatform().openFile(path.toFile());
            } else {
                Util.getPlatform().openPath(path.getParent());
            }
        } catch (Exception e) {
            Onetoys.LOGGER.error("Failed to open configuration file '{}' in external editor: {}", path, e.getMessage());
            try {
                Util.getPlatform().openPath(path.getParent());
            } catch (Exception ignored) {}
        }
    }

    public static class ConfigFileButton extends Button {
        private final ItemStack icon = new ItemStack(Items.WRITABLE_BOOK);

        public ConfigFileButton(int width, int height, Component message, Tooltip tooltip, Runnable onPress) {
            super(0, 0, width, height, message, btn -> onPress.run(), DEFAULT_NARRATION);
            setTooltip(tooltip);
        }

        @Override
        protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
            super.renderWidget(guiGraphics, mouseX, mouseY, partialTick);
            if (!icon.isEmpty()) {
                int iconX = getX() + 6;
                int iconY = getY() + (height - 16) / 2;
                guiGraphics.renderFakeItem(icon, iconX, iconY);
            }
        }

        @Override
        public void renderString(GuiGraphics guiGraphics, Font font, int color) {
            int iconPadding = 26;
            int minX = getX() + iconPadding;
            int maxX = getX() + width - 6;
            renderScrollingString(guiGraphics, font, getMessage(), (minX + maxX) / 2, minX, getY(), maxX, getY() + height, color);
        }
    }
}
