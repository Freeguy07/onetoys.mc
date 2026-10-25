package com.onetoys.module.gameplay.items;

import com.onetoys.registry.OnetoysItems;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.providers.VanillaEnchantmentProviders;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.ModConfigSpec;

public final class ClubTweaks {
    private final ItemTweaks parent;
    private ModConfigSpec.BooleanValue enabledConfig;
    private ModConfigSpec.DoubleValue mobSpawnChanceConfig;

    public ClubTweaks(ItemTweaks parent) {
        this.parent = parent;
    }

    public ClubTweaks() {
        this(null);
    }

    public void buildConfig(ModConfigSpec.Builder builder) {
        builder.comment("Club item: a stone-level multitool and heavy shield-breaking weapon.")
                .translation("onetoys.configuration.gameplay_tweaks.item_tweaks.club")
                .push("club");

        enabledConfig = builder
                .comment("Enable the club item.")
                .translation("onetoys.configuration.gameplay_tweaks.item_tweaks.club.enabled")
                .define("enabled", true);

        mobSpawnChanceConfig = builder
                .comment("Chance (0.0 - 1.0) for adult zombies to spawn with a club (0.05 = 5%).")
                .translation("onetoys.configuration.gameplay_tweaks.item_tweaks.club.mob_spawn_chance")
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
        // Coordinated with MacheteTweaks for primitive zombie weapon rolls
    }

    public static boolean isEligibleMob(Mob mob) {
        if (mob.getMainHandItem().getItem() instanceof CrossbowItem) {
            return false;
        }
        if (mob instanceof Zombie zombie && zombie.isBaby()) {
            return false;
        }
        EntityType<?> type = mob.getType();
        return type == EntityType.ZOMBIE
                || type == EntityType.HUSK
                || type == EntityType.DROWNED
                || type == EntityType.ZOMBIE_VILLAGER;
    }

    public static void equipClub(Mob mob) {
        ItemStack club = new ItemStack(OnetoysItems.CLUB.get());
        if (mob.level() instanceof ServerLevel serverLevel) {
            DifficultyInstance difficulty = serverLevel.getCurrentDifficultyAt(mob.blockPosition());
            RandomSource random = mob.getRandom();
            if (random.nextFloat() < 0.25F * difficulty.getSpecialMultiplier()) {
                EnchantmentHelper.enchantItemFromProvider(
                        club,
                        serverLevel.registryAccess(),
                        VanillaEnchantmentProviders.MOB_SPAWN_EQUIPMENT,
                        difficulty,
                        random
                );
            }
        }
        mob.setItemSlot(EquipmentSlot.MAINHAND, club);
        mob.setDropChance(EquipmentSlot.MAINHAND, 0.085F);
    }
}
