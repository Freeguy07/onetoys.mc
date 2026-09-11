package com.orehall.client.gui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;

import com.electronwill.nightconfig.core.UnmodifiableConfig;
import com.electronwill.nightconfig.core.UnmodifiableConfig.Entry;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.client.gui.ConfigurationScreen.ConfigurationSectionScreen;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.common.ModConfigSpec.ValueSpec;

/**
 * Custom configuration section screen for Orehall.
 * Provides interactive UI behavior for durability tweaks:
 * - When 'enable_global' switch is toggled ON:
 *   - 'global_durability' field is active and highlighted with white (outline + text/label).
 *   - All individual item durability options are grayed out (disabled/uneditable).
 * - When 'enable_global' switch is toggled OFF (vice versa):
 *   - 'global_durability' field is grayed out (disabled/uneditable).
 *   - All individual item durability options are active and highlighted with white.
 */
@OnlyIn(Dist.CLIENT)
public class OrehallConfigurationSectionScreen extends ConfigurationSectionScreen {

    private EditBox globalDurabilityBox;
    private StringWidget globalDurabilityLabel;
    private final Map<String, EditBox> itemBoxes = new HashMap<>();
    private final Map<String, StringWidget> itemLabels = new HashMap<>();

    private ModConfigSpec.BooleanValue globalEnabledConfig;
    private boolean isTierSection = false;
    private boolean globalEnabled = false;

    public OrehallConfigurationSectionScreen(Screen parent, ModConfig.Type type, ModConfig modConfig, Component title) {
        super(parent, type, modConfig, title);
    }

    public OrehallConfigurationSectionScreen(Screen parent, ModConfig.Type type, ModConfig modConfig, Component title, Filter filter) {
        super(parent, type, modConfig, title, filter);
    }

    public OrehallConfigurationSectionScreen(Context parentContext, Screen parent, Map<String, Object> valueSpecs, String key, Set<? extends Entry> entrySet, Component title) {
        super(parentContext, parent, valueSpecs, key, entrySet, title);
    }

    protected OrehallConfigurationSectionScreen(Context context, Component title) {
        super(context, title);
    }

    @Override
    public OrehallConfigurationSectionScreen rebuild() {
        globalDurabilityBox = null;
        globalDurabilityLabel = null;
        itemBoxes.clear();
        itemLabels.clear();
        globalEnabledConfig = null;

        this.isTierSection = context.valueSpecs().containsKey("enable_global") && context.valueSpecs().containsKey("global_durability");
        if (this.isTierSection) {
            findGlobalConfig();
            this.globalEnabled = getGlobalEnabledValue();
        }

        super.rebuild();

        if (this.isTierSection && this.list != null) {
            for (GuiEventListener row : this.list.children()) {
                if (row instanceof net.minecraft.client.gui.components.events.ContainerEventHandler containerRow) {
                    List<? extends GuiEventListener> rowWidgets = containerRow.children();
                    if (rowWidgets.size() >= 2) {
                        GuiEventListener left = rowWidgets.get(0);
                        GuiEventListener right = rowWidgets.get(1);
                        if (left instanceof StringWidget stringWidget && right instanceof EditBox editBox) {
                            if (editBox == globalDurabilityBox) {
                                globalDurabilityLabel = stringWidget;
                            } else {
                                for (Map.Entry<String, EditBox> entry : itemBoxes.entrySet()) {
                                    if (entry.getValue() == editBox) {
                                        itemLabels.put(entry.getKey(), stringWidget);
                                        break;
                                    }
                                }
                            }
                        }
                    }
                }
            }
            updateDurabilityWidgets();
        }

        return this;
    }

    @Override
    protected Element createBooleanValue(String key, ValueSpec spec, Supplier<Boolean> source, Consumer<Boolean> target) {
        if (this.isTierSection && "enable_global".equals(key)) {
            this.globalEnabled = source.get();
            return super.createBooleanValue(key, spec, source, newValue -> {
                this.globalEnabled = newValue;
                target.accept(newValue);
                updateDurabilityWidgets();
            });
        }
        return super.createBooleanValue(key, spec, source, target);
    }

    @Override
    protected Element createIntegerValue(String key, ValueSpec spec, Supplier<Integer> source, Consumer<Integer> target) {
        Element element = super.createIntegerValue(key, spec, source, target);
        if (this.isTierSection && element != null && element.widget() instanceof EditBox editBox) {
            if ("global_durability".equals(key)) {
                globalDurabilityBox = editBox;
            } else {
                itemBoxes.put(key, editBox);
            }
        }
        return element;
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
    protected Element createSection(String key, UnmodifiableConfig subconfig, UnmodifiableConfig subsection) {
        if (subconfig.isEmpty()) return null;

        String buttonTextKey = getTranslationKey(key) + ".button";
        Component buttonText = I18n.exists(buttonTextKey)
                ? Component.translatable(buttonTextKey)
                : Component.translatable("neoforge.configuration.uitext.sectiontext");

        Component buttonLabel = Component.translatable("neoforge.configuration.uitext.section", buttonText);
        Component sectionTitle = Component.translatable(getTranslationKey(key));

        return new Element(
                Component.translatable("neoforge.configuration.uitext.section", getTranslationComponent(key)),
                getTooltipComponent(key, null),
                Button.builder(buttonLabel, button -> {
                    if (minecraft != null) {
                        OrehallConfigurationSectionScreen screen = (OrehallConfigurationSectionScreen) sectionCache.computeIfAbsent(key,
                                k -> new OrehallConfigurationSectionScreen(context, this, subconfig.valueMap(), key, subsection.entrySet(), sectionTitle));
                        minecraft.setScreen(screen.rebuild());
                    }
                })
                .tooltip(Tooltip.create(getTooltipComponent(key, null)))
                .width(Button.DEFAULT_WIDTH)
                .build(),
                false
        );
    }

    @Override
    protected void onChanged(String key) {
        super.onChanged(key);
        if (this.isTierSection && "enable_global".equals(key)) {
            this.globalEnabled = getGlobalEnabledValue();
            updateDurabilityWidgets();
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (this.isTierSection) {
            boolean current = getGlobalEnabledValue();
            if (current != this.globalEnabled) {
                this.globalEnabled = current;
                updateDurabilityWidgets();
            }
        }

        super.render(graphics, mouseX, mouseY, partialTick);

        if (this.isTierSection && this.list != null) {
            graphics.enableScissor(this.list.getX(), this.list.getY(), this.list.getRight(), this.list.getBottom());
            if (this.globalEnabled) {
                if (globalDurabilityBox != null && globalDurabilityBox.visible) {
                    renderWhiteHighlight(graphics, globalDurabilityBox);
                }
            } else {
                for (EditBox itemBox : itemBoxes.values()) {
                    if (itemBox != null && itemBox.visible) {
                        renderWhiteHighlight(graphics, itemBox);
                    }
                }
            }
            graphics.disableScissor();
        }
    }

    private void renderWhiteHighlight(GuiGraphics graphics, EditBox box) {
        int top = box.getY();
        int bottom = top + box.getHeight();
        if (this.list != null && (bottom <= this.list.getY() || top >= this.list.getBottom())) {
            return;
        }
        graphics.renderOutline(box.getX() - 1, top - 1, box.getWidth() + 2, box.getHeight() + 2, 0xFFFFFFFF);
    }

    private void findGlobalConfig() {
        for (Entry entry : context.entries()) {
            if ("enable_global".equals(entry.getKey())) {
                if (entry.getRawValue() instanceof ModConfigSpec.BooleanValue bv) {
                    this.globalEnabledConfig = bv;
                    return;
                }
            }
        }
    }

    private boolean getGlobalEnabledValue() {
        return this.globalEnabledConfig != null ? this.globalEnabledConfig.get() : false;
    }

    private void updateDurabilityWidgets() {
        if (!this.isTierSection) return;

        if (this.globalEnabled) {
            // Global enabled: highlight global setting with white, gray out other options
            if (globalDurabilityBox != null) {
                globalDurabilityBox.active = true;
                globalDurabilityBox.setEditable(true);
                globalDurabilityBox.setTextColor(0xFFFFFFFF);
            }
            if (globalDurabilityLabel != null) {
                globalDurabilityLabel.setColor(0xFFFFFFFF);
            }

            for (Map.Entry<String, EditBox> entry : itemBoxes.entrySet()) {
                EditBox itemBox = entry.getValue();
                if (itemBox != null) {
                    itemBox.active = false;
                    itemBox.setEditable(false);
                    itemBox.setTextColor(0x707070);
                }
                StringWidget itemLabel = itemLabels.get(entry.getKey());
                if (itemLabel != null) {
                    itemLabel.setColor(0x707070);
                }
            }
        } else {
            // Global disabled (vice versa): gray out global setting, highlight individual options with white
            if (globalDurabilityBox != null) {
                globalDurabilityBox.active = false;
                globalDurabilityBox.setEditable(false);
                globalDurabilityBox.setTextColor(0x707070);
            }
            if (globalDurabilityLabel != null) {
                globalDurabilityLabel.setColor(0x707070);
            }

            for (Map.Entry<String, EditBox> entry : itemBoxes.entrySet()) {
                EditBox itemBox = entry.getValue();
                if (itemBox != null) {
                    itemBox.active = true;
                    itemBox.setEditable(true);
                    itemBox.setTextColor(0xFFFFFFFF);
                }
                StringWidget itemLabel = itemLabels.get(entry.getKey());
                if (itemLabel != null) {
                    itemLabel.setColor(0xFFFFFFFF);
                }
            }
        }
    }
}
