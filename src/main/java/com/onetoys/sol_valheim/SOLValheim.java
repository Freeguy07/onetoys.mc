package com.onetoys.sol_valheim;

import com.onetoys.salt.Salting;
import com.onetoys.salt.Sweetening;
import com.onetoys.salt.Satiating;
import com.onetoys.sol_valheim.ModConfig.Common.FoodConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;

public class SOLValheim {
    public static final String MODID = com.onetoys.Onetoys.MODID;

    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES = DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, com.onetoys.Onetoys.MODID);
    public static final Supplier<AttachmentType<ValheimFoodData>> FOOD_DATA = ATTACHMENT_TYPES.register(
            "valheim_food_data", () -> AttachmentType.serializable(ValheimFoodData::new)
                    .copyOnDeath()
                    .sync(ValheimFoodData.STREAM_CODEC)
                    .build()
    );

    public static class Config {
        public static ModConfig.Common common = ModConfig.COMMON;
        public static ModConfig.Client client = ModConfig.CLIENT;
    }

    public static final ResourceLocation SPEED_BUFF_ID = ResourceLocation.fromNamespaceAndPath(com.onetoys.Onetoys.MODID, "speed_buff");
    private static AttributeModifier speedBuff;
    private static double lastSpeedConfig = -1;

    public static AttributeModifier getSpeedBuffModifier() {
        double currentSpeed = (com.onetoys.Config.SPEC != null && com.onetoys.Config.SPEC.isLoaded())
                ? Config.common.speedBoost.get()
                : 0.20;
        if (speedBuff == null || Double.compare(lastSpeedConfig, currentSpeed) != 0) {
            lastSpeedConfig = currentSpeed;
            speedBuff = new AttributeModifier(SPEED_BUFF_ID, currentSpeed, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
        }
        return speedBuff;
    }

    @Deprecated
    public static void init(ModContainer modContainer) {
    }

    public static void addTooltip(ItemStack item, TooltipFlag flag, List<Component> list, boolean shiftDown) {
        if (item.is(Items.ROTTEN_FLESH)) {
            list.add(Component.translatable("onetoys.tooltip.food.empties_stomach").withStyle(ChatFormatting.GREEN));
            addSeasoningTooltips(item, list, shiftDown);
            return;
        }

        var config = ModConfig.getFoodConfig(item);
        if (config == null) {
            if (item.getUseAnimation() == UseAnim.DRINK) {
                list.add(Component.translatable("onetoys.tooltip.food.refreshing").withStyle(ChatFormatting.AQUA));
            }
            addSeasoningTooltips(item, list, shiftDown);
            return;
        }

        float healthVal = config.getHeartsBonus();
        String healthStr;
        if (Math.abs(healthVal - Math.round(healthVal)) < 0.005f) {
            healthStr = Integer.toString(Math.round(healthVal));
        } else {
            healthStr = String.format(Locale.ROOT, "%.1f", healthVal);
        }
        list.add(Component.translatable("onetoys.tooltip.food.health", healthStr).withStyle(ChatFormatting.RED));

        float regenVal = config.getHealthRegen();
        String regenStr;
        if (Math.abs(regenVal - Math.round(regenVal)) < 0.005f) {
            regenStr = Integer.toString(Math.round(regenVal));
        } else if (Math.abs(regenVal * 10.0f - Math.round(regenVal * 10.0f)) < 0.005f) {
            regenStr = String.format(Locale.ROOT, "%.1f", regenVal);
        } else {
            regenStr = String.format(Locale.ROOT, "%.2f", regenVal);
        }
        list.add(Component.translatable("onetoys.tooltip.food.regen", regenStr).withStyle(ChatFormatting.DARK_RED));

        int totalTicks = config.getTime();
        int totalSeconds = totalTicks / 20;
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;
        String timeStr = String.format(Locale.ROOT, "%d:%02d", minutes, seconds);
        list.add(Component.translatable("onetoys.tooltip.food.duration", timeStr).withStyle(ChatFormatting.GOLD));

        boolean displayEffects = Config.common.displayEffects != null && ModConfig.isLoaded()
                ? Config.common.displayEffects.get()
                : true;

        if (item.is(Items.GLOW_LICHEN) && displayEffects) {
            var effectName = Component.translatable("effect.minecraft.glowing");
            var text = Component.literal("  \u2726 ").append(effectName).append(" (0:15)");
            list.add(text.withStyle(ChatFormatting.YELLOW));
        }
        if (!config.extraEffects.isEmpty() && displayEffects) {
            for (var effectCfg : config.extraEffects) {
                var effectHolder = effectCfg.getEffect();
                if (effectHolder != null) {
                    var effect = effectHolder.value();
                    var effectName = Component.translatable(effect.getDescriptionId());
                    int effectDurationSec = (int) (totalSeconds * effectCfg.duration);
                    int eMin = effectDurationSec / 60;
                    int eSec = effectDurationSec % 60;
                    var text = Component.literal("  \u2726 ").append(effectName).append(String.format(Locale.ROOT, " (%d:%02d)", eMin, eSec));
                    list.add(text.withStyle(ChatFormatting.BLUE));
                }
            }
        }

        if (item.getUseAnimation() == UseAnim.DRINK) {
            list.add(Component.translatable("onetoys.tooltip.food.refreshing").withStyle(ChatFormatting.AQUA));
        }

        addSeasoningTooltips(item, list, shiftDown);
    }

    private static void addSeasoningTooltips(ItemStack item, List<Component> list, boolean shiftDown) {
        boolean salted = Salting.isSalted(item);
        boolean sweetened = Sweetening.isSweetened(item);
        boolean satiating = Satiating.isSatiating(item);

        if (!salted && !sweetened && !satiating) {
            return;
        }

        if (shiftDown) {
            if (salted) {
                list.add(Component.translatable("onetoys.tooltip.food.salted_expanded").withStyle(ChatFormatting.WHITE));
            }
            if (sweetened) {
                list.add(Component.translatable("onetoys.tooltip.food.sweetened_expanded").withStyle(ChatFormatting.LIGHT_PURPLE));
            }
            if (satiating) {
                list.add(Component.translatable("onetoys.tooltip.food.satiating_expanded").withStyle(ChatFormatting.YELLOW));
            }
        } else {
            if (salted) {
                list.add(Component.translatable("onetoys.tooltip.food.salted").withStyle(ChatFormatting.WHITE));
            }
            if (sweetened) {
                list.add(Component.translatable("onetoys.tooltip.food.sweetened").withStyle(ChatFormatting.LIGHT_PURPLE));
            }
            if (satiating) {
                list.add(Component.translatable("onetoys.tooltip.food.satiating").withStyle(ChatFormatting.YELLOW));
            }
            list.add(Component.translatable("onetoys.tooltip.food.seasoning.hold_shift").withStyle(ChatFormatting.DARK_GRAY));
        }
    }
}
