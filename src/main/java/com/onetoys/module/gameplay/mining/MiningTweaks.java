package com.onetoys.module.gameplay.mining;

import com.onetoys.module.ModuleManager;
import com.onetoys.module.gameplay.GameplayTweaksModule;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TieredItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

public class MiningTweaks {
    public record TierInfo(double targetSpeed, float baseSpeed) {}

    private final GameplayTweaksModule parent;

    private ModConfigSpec.BooleanValue enabledConfig;
    private ModConfigSpec.BooleanValue disableMiningDelayConfig;
    private ModConfigSpec.BooleanValue applyToCreativeConfig;

    private ModConfigSpec.BooleanValue rebalanceTierSpeedsConfig;
    private ModConfigSpec.DoubleValue woodSpeedConfig;
    private ModConfigSpec.DoubleValue stoneSpeedConfig;
    private ModConfigSpec.DoubleValue copperSpeedConfig;
    private ModConfigSpec.DoubleValue ironSpeedConfig;
    private ModConfigSpec.DoubleValue silverSpeedConfig;
    private ModConfigSpec.DoubleValue diamondSpeedConfig;
    private ModConfigSpec.DoubleValue necromiumSpeedConfig;
    private ModConfigSpec.DoubleValue netheriteSpeedConfig;
    private ModConfigSpec.DoubleValue goldSpeedConfig;

    public MiningTweaks(GameplayTweaksModule parent) {
        this.parent = parent;
    }

    public MiningTweaks() {
        this(null);
    }

    public void buildConfig(ModConfigSpec.Builder builder) {
        builder.comment("Tweaks and adjustments to block breaking and mining mechanics.")
                .translation("onetoys.configuration.gameplay_tweaks.mining_tweaks")
                .push("mining_tweaks");

        enabledConfig = builder
                .comment("Master switch to enable or disable the mining tweaks sub-category.")
                .translation("onetoys.configuration.gameplay_tweaks.mining_tweaks.enabled")
                .define("enabled", true);

        disableMiningDelayConfig = builder
                .comment("When enabled, removes the artificial 5-tick (0.25s) cooldown delay between breaking consecutive blocks in Survival mode.")
                .translation("onetoys.configuration.gameplay_tweaks.mining_tweaks.disable_mining_delay")
                .define("disable_mining_delay", true);

        applyToCreativeConfig = builder
                .comment("When enabled, also removes the 5-tick cooldown delay between breaking consecutive blocks in Creative mode (allowing continuous 20 blocks/sec breaking when holding left-click).")
                .translation("onetoys.configuration.gameplay_tweaks.mining_tweaks.apply_to_creative")
                .define("apply_to_creative", false);

        rebalanceTierSpeedsConfig = builder
                .comment("When enabled, rebalances mining speed across tool tiers to maintain a distinct sense of progression after removing the mining delay.")
                .translation("onetoys.configuration.gameplay_tweaks.mining_tweaks.rebalance_tier_speeds")
                .define("rebalance_tier_speeds", true);

        woodSpeedConfig = builder
                .comment("Mining speed multiplier for Wood tier tools (vanilla: 2.0, default: 1.0).")
                .translation("onetoys.configuration.gameplay_tweaks.mining_tweaks.wood_speed")
                .defineInRange("wood_speed", 1.0D, 0.1D, 100.0D);

        stoneSpeedConfig = builder
                .comment("Mining speed multiplier for Stone tier tools (vanilla: 4.0, default: 2.0).")
                .translation("onetoys.configuration.gameplay_tweaks.mining_tweaks.stone_speed")
                .defineInRange("stone_speed", 2.0D, 0.1D, 100.0D);

        copperSpeedConfig = builder
                .comment("Mining speed multiplier for Copper tier tools, such as Caverns and Chasms (base: 5.0, default: 4.0).")
                .translation("onetoys.configuration.gameplay_tweaks.mining_tweaks.copper_speed")
                .defineInRange("copper_speed", 4.0D, 0.1D, 100.0D);

        ironSpeedConfig = builder
                .comment("Mining speed multiplier for Iron tier tools (vanilla: 6.0, default: 4.5).")
                .translation("onetoys.configuration.gameplay_tweaks.mining_tweaks.iron_speed")
                .defineInRange("iron_speed", 4.5D, 0.1D, 100.0D);

        silverSpeedConfig = builder
                .comment("Mining speed multiplier for Silver tier tools, such as Caverns and Chasms (base: 9.0, default: 6.5).")
                .translation("onetoys.configuration.gameplay_tweaks.mining_tweaks.silver_speed")
                .defineInRange("silver_speed", 6.5D, 0.1D, 100.0D);

        diamondSpeedConfig = builder
                .comment("Mining speed multiplier for Diamond tier tools (vanilla: 8.0, default: 7.0).")
                .translation("onetoys.configuration.gameplay_tweaks.mining_tweaks.diamond_speed")
                .defineInRange("diamond_speed", 7.0D, 0.1D, 100.0D);

        necromiumSpeedConfig = builder
                .comment("Mining speed multiplier for Necromium tier tools, such as Caverns and Chasms (base: 9.0, default: 8.5).")
                .translation("onetoys.configuration.gameplay_tweaks.mining_tweaks.necromium_speed")
                .defineInRange("necromium_speed", 8.5D, 0.1D, 100.0D);

        netheriteSpeedConfig = builder
                .comment("Mining speed multiplier for Netherite tier tools (vanilla: 9.0, default: 9.0).")
                .translation("onetoys.configuration.gameplay_tweaks.mining_tweaks.netherite_speed")
                .defineInRange("netherite_speed", 9.0D, 0.1D, 100.0D);

        goldSpeedConfig = builder
                .comment("Mining speed multiplier for Gold tier tools (vanilla: 12.0, default: 10.0).")
                .translation("onetoys.configuration.gameplay_tweaks.mining_tweaks.gold_speed")
                .defineInRange("gold_speed", 10.0D, 0.1D, 100.0D);

        builder.pop();
    }

    public void onConstruct(IEventBus modBus) {
        NeoForge.EVENT_BUS.addListener(this::onBreakSpeed);
    }

    public void onBreakSpeed(PlayerEvent.BreakSpeed event) {
        if (!isEnabled() || !isRebalanceTierSpeeds()) {
            return;
        }
        Player player = event.getEntity();
        if (player.isCreative()) {
            return;
        }
        ItemStack stack = player.getMainHandItem();
        if (stack.isEmpty() || !(stack.getItem() instanceof TieredItem tieredItem)) {
            return;
        }
        BlockState state = event.getState();
        // Only adjust if the tool provides a digging speed boost for this block (base tool speed > 1.0F)
        if (stack.getDestroySpeed(state) <= 1.0F) {
            return;
        }
        Tier tier = tieredItem.getTier();
        TierInfo info = getTierInfo(tier, stack);
        if (info == null || info.baseSpeed() <= 0.0F) {
            return;
        }
        float ratio = (float) (info.targetSpeed() / info.baseSpeed());
        event.setNewSpeed(event.getNewSpeed() * ratio);
    }

    public TierInfo getTierInfo(Tier tier, ItemStack stack) {
        // 1. Standard Vanilla tiers
        if (tier == Tiers.WOOD) return new TierInfo(getWoodSpeed(), 2.0F);
        if (tier == Tiers.STONE) return new TierInfo(getStoneSpeed(), 4.0F);
        if (tier == Tiers.IRON) return new TierInfo(getIronSpeed(), 6.0F);
        if (tier == Tiers.DIAMOND) return new TierInfo(getDiamondSpeed(), 8.0F);
        if (tier == Tiers.NETHERITE) return new TierInfo(getNetheriteSpeed(), 9.0F);
        if (tier == Tiers.GOLD) return new TierInfo(getGoldSpeed(), 12.0F);

        // 2. Check enum name if enum
        if (tier instanceof Enum<?> enumTier) {
            String name = enumTier.name();
            if ("WOOD".equalsIgnoreCase(name)) return new TierInfo(getWoodSpeed(), 2.0F);
            if ("STONE".equalsIgnoreCase(name)) return new TierInfo(getStoneSpeed(), 4.0F);
            if ("IRON".equalsIgnoreCase(name)) return new TierInfo(getIronSpeed(), 6.0F);
            if ("DIAMOND".equalsIgnoreCase(name)) return new TierInfo(getDiamondSpeed(), 8.0F);
            if ("NETHERITE".equalsIgnoreCase(name)) return new TierInfo(getNetheriteSpeed(), 9.0F);
            if ("GOLD".equalsIgnoreCase(name)) return new TierInfo(getGoldSpeed(), 12.0F);
            if ("COPPER".equalsIgnoreCase(name)) return new TierInfo(getCopperSpeed(), tier.getSpeed() > 0 ? tier.getSpeed() : 5.0F);
            if ("SILVER".equalsIgnoreCase(name)) return new TierInfo(getSilverSpeed(), tier.getSpeed() > 0 ? tier.getSpeed() : 9.0F);
            if ("NECROMIUM".equalsIgnoreCase(name)) return new TierInfo(getNecromiumSpeed(), tier.getSpeed() > 0 ? tier.getSpeed() : 9.0F);
        }

        // 3. Check tier incorrectBlocks tag location (e.g. caverns_and_chasms:incorrect_for_copper_tool)
        TagKey<Block> incorrectTag = tier.getIncorrectBlocksForDrops();
        if (incorrectTag != null) {
            String tagPath = incorrectTag.location().getPath().toLowerCase();
            if (tagPath.contains("copper")) return new TierInfo(getCopperSpeed(), tier.getSpeed() > 0 ? tier.getSpeed() : 5.0F);
            if (tagPath.contains("silver")) return new TierInfo(getSilverSpeed(), tier.getSpeed() > 0 ? tier.getSpeed() : 9.0F);
            if (tagPath.contains("necromium")) return new TierInfo(getNecromiumSpeed(), tier.getSpeed() > 0 ? tier.getSpeed() : 9.0F);
        }

        // 4. Check item ID / namespace (e.g. caverns_and_chasms:weathered_copper_pickaxe)
        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
        String itemNamespace = itemId.getNamespace().toLowerCase();
        String itemPath = itemId.getPath().toLowerCase();

        if (itemNamespace.equals("caverns_and_chasms") || itemNamespace.equals("c")) {
            if (itemPath.contains("copper")) return new TierInfo(getCopperSpeed(), tier.getSpeed() > 0 ? tier.getSpeed() : 5.0F);
            if (itemPath.contains("silver") || itemPath.contains("kunai")) return new TierInfo(getSilverSpeed(), tier.getSpeed() > 0 ? tier.getSpeed() : 9.0F);
            if (itemPath.contains("necromium")) return new TierInfo(getNecromiumSpeed(), tier.getSpeed() > 0 ? tier.getSpeed() : 9.0F);
        } else {
            if (itemPath.contains("copper_pickaxe") || itemPath.contains("copper_axe") || itemPath.contains("copper_shovel") || itemPath.contains("copper_hoe") || itemPath.contains("copper_sword")) {
                return new TierInfo(getCopperSpeed(), tier.getSpeed() > 0 ? tier.getSpeed() : 5.0F);
            }
            if (itemPath.contains("silver_pickaxe") || itemPath.contains("silver_axe") || itemPath.contains("silver_shovel") || itemPath.contains("silver_hoe") || itemPath.contains("silver_sword")) {
                return new TierInfo(getSilverSpeed(), tier.getSpeed() > 0 ? tier.getSpeed() : 9.0F);
            }
            if (itemPath.contains("necromium_pickaxe") || itemPath.contains("necromium_axe") || itemPath.contains("necromium_shovel") || itemPath.contains("necromium_hoe") || itemPath.contains("necromium_sword")) {
                return new TierInfo(getNecromiumSpeed(), tier.getSpeed() > 0 ? tier.getSpeed() : 9.0F);
            }
        }

        return null;
    }

    public boolean isEnabled() {
        if (parent != null && !parent.isEnabled()) {
            return false;
        }
        return enabledConfig == null || enabledConfig.get();
    }

    public boolean isDisableMiningDelay() {
        return isEnabled() && (disableMiningDelayConfig == null || disableMiningDelayConfig.get());
    }

    public boolean isApplyToCreative() {
        return isEnabled() && (applyToCreativeConfig != null && applyToCreativeConfig.get());
    }

    public boolean isRebalanceTierSpeeds() {
        return isEnabled() && (rebalanceTierSpeedsConfig == null || rebalanceTierSpeedsConfig.get());
    }

    public double getWoodSpeed() {
        return woodSpeedConfig != null ? woodSpeedConfig.get() : 1.0D;
    }

    public double getStoneSpeed() {
        return stoneSpeedConfig != null ? stoneSpeedConfig.get() : 2.0D;
    }

    public double getCopperSpeed() {
        return copperSpeedConfig != null ? copperSpeedConfig.get() : 4.0D;
    }

    public double getIronSpeed() {
        return ironSpeedConfig != null ? ironSpeedConfig.get() : 4.5D;
    }

    public double getSilverSpeed() {
        return silverSpeedConfig != null ? silverSpeedConfig.get() : 6.5D;
    }

    public double getDiamondSpeed() {
        return diamondSpeedConfig != null ? diamondSpeedConfig.get() : 7.0D;
    }

    public double getNecromiumSpeed() {
        return necromiumSpeedConfig != null ? necromiumSpeedConfig.get() : 8.5D;
    }

    public double getNetheriteSpeed() {
        return netheriteSpeedConfig != null ? netheriteSpeedConfig.get() : 9.0D;
    }

    public double getGoldSpeed() {
        return goldSpeedConfig != null ? goldSpeedConfig.get() : 10.0D;
    }

    public static boolean shouldDisableMiningDelay(GameType mode) {
        if (!ModuleManager.get().isModuleEnabled(GameplayTweaksModule.ID)) {
            return false;
        }
        GameplayTweaksModule module = GameplayTweaksModule.getInstance();
        if (module == null || !module.isEnabled()) {
            return false;
        }
        MiningTweaks tweaks = module.getMiningTweaks();
        if (tweaks == null || !tweaks.isEnabled()) {
            return false;
        }
        if (mode == GameType.CREATIVE) {
            return tweaks.isApplyToCreative();
        }
        return tweaks.isDisableMiningDelay();
    }
}
