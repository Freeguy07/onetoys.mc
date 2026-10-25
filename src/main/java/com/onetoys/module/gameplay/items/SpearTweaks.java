package com.onetoys.module.gameplay.items;

import com.onetoys.registry.OnetoysAttachments;
import com.onetoys.registry.OnetoysItems;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.AbstractSkeleton;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.providers.VanillaEnchantmentProviders;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;

public final class SpearTweaks {
    private final ItemTweaks parent;
    private ModConfigSpec.BooleanValue enabledConfig;
    private ModConfigSpec.DoubleValue mobSpawnChanceConfig;

    public SpearTweaks(ItemTweaks parent) {
        this.parent = parent;
    }

    public SpearTweaks() {
        this(null);
    }

    public void buildConfig(ModConfigSpec.Builder builder) {
        builder.comment("Spear item: an early game alternative to the trident.")
                .translation("onetoys.configuration.gameplay_tweaks.item_tweaks.spear")
                .push("spear");

        enabledConfig = builder
                .comment("Enable the spear item.")
                .translation("onetoys.configuration.gameplay_tweaks.item_tweaks.spear.enabled")
                .define("enabled", true);

        mobSpawnChanceConfig = builder
                .comment("Chance (0.0 - 1.0) for skeletons (skeleton, stray, bogged) to spawn with a spear (0.05 = 5%).")
                .translation("onetoys.configuration.gameplay_tweaks.item_tweaks.spear.mob_spawn_chance")
                .defineInRange("mob_spawn_chance", 0.05D, 0.0D, 1.0D);

        builder.pop();
    }

    public boolean isEnabled() {
        return (parent == null || parent.isEnabled()) && (enabledConfig == null || enabledConfig.get());
    }

    public double getMobSpawnChance() {
        if (!isEnabled()) {
            return 0.0D;
        }
        return mobSpawnChanceConfig != null ? mobSpawnChanceConfig.get() : 0.05D;
    }

    public void onConstruct(IEventBus modBus) {
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGH, this::onEntityJoinLevel);
    }

    private void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide() || !(event.getEntity() instanceof Mob mob)) {
            return;
        }
        if (!isEnabled()) {
            return;
        }

        if (!mob.getData(OnetoysAttachments.SPEAR_SPAWN_ROLLED)) {
            mob.setData(OnetoysAttachments.SPEAR_SPAWN_ROLLED, true);
            if (isEligibleMob(mob)) {
                double chance = getMobSpawnChance();
                if (chance > 0.0D && mob.getRandom().nextDouble() < chance) {
                    equipSpear(mob);
                }
            }
        }
    }

    public static boolean isEligibleMob(Mob mob) {
        if (mob.getMainHandItem().getItem() instanceof CrossbowItem) {
            return false;
        }
        EntityType<?> type = mob.getType();
        return type == EntityType.SKELETON
                || type == EntityType.STRAY
                || type == EntityType.BOGGED;
    }

    public static void equipSpear(Mob mob) {
        ItemStack spear = new ItemStack(OnetoysItems.SPEAR.get());
        if (mob.level() instanceof ServerLevel serverLevel) {
            DifficultyInstance difficulty = serverLevel.getCurrentDifficultyAt(mob.blockPosition());
            RandomSource random = mob.getRandom();
            if (random.nextFloat() < 0.25F * difficulty.getSpecialMultiplier()) {
                EnchantmentHelper.enchantItemFromProvider(
                        spear,
                        serverLevel.registryAccess(),
                        VanillaEnchantmentProviders.MOB_SPAWN_EQUIPMENT,
                        difficulty,
                        random
                );
            }
        }
        mob.setItemSlot(EquipmentSlot.MAINHAND, spear);
        mob.setDropChance(EquipmentSlot.MAINHAND, 0.085F);
        if (mob instanceof AbstractSkeleton skeleton) {
            skeleton.reassessWeaponGoal();
        }
    }
}
