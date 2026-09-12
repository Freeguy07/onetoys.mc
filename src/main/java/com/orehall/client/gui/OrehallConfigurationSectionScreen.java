package com.orehall.client.gui;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.electronwill.nightconfig.core.UnmodifiableConfig;
import com.electronwill.nightconfig.core.UnmodifiableConfig.Entry;

import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.client.gui.ConfigurationScreen.ConfigurationSectionScreen;
import net.neoforged.neoforge.common.ModConfigSpec.ValueSpec;

/**
 * Custom configuration section screen for Orehall.
 * Provides hierarchical configuration translation key resolution and customized section buttons.
 */
@OnlyIn(Dist.CLIENT)
public class OrehallConfigurationSectionScreen extends ConfigurationSectionScreen {

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
}
