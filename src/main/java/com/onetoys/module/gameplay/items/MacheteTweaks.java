package com.onetoys.module.gameplay.items;

import com.onetoys.registry.OnetoysAttachments;
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
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;

public final class MacheteTweaks {
    private final ItemTweaks parent;
    private ModConfigSpec.BooleanValue enabledConfig;
    private ModConfigSpec.DoubleValue mobSpawnChanceConfig;

    public MacheteTweaks(ItemTweaks parent) {
        this.parent = parent;
    }

    public MacheteTweaks() {
        this(null);
    }

    public void buildConfig(ModConfigSpec.Builder builder) {
        builder.comment("Machete item: a classic rapid-slashing weapon.")
                .translation("onetoys.configuration.gameplay_tweaks.item_tweaks.machete")
                .push("machete");

        enabledConfig = builder
                .comment("Enable the machete item.")
                .translation("onetoys.configuration.gameplay_tweaks.item_tweaks.machete.enabled")
                .define("enabled", true);

        mobSpawnChanceConfig = builder
                .comment("Chance (0.0 - 1.0) for zombies to spawn with a machete (0.05 = 5%).")
                .translation("onetoys.configuration.gameplay_tweaks.item_tweaks.machete.mob_spawn_chance")
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

        if (!mob.getData(OnetoysAttachments.MACHETE_SPAWN_ROLLED) && !mob.getData(OnetoysAttachments.CLUB_SPAWN_ROLLED)) {
            mob.setData(OnetoysAttachments.MACHETE_SPAWN_ROLLED, true);
            mob.setData(OnetoysAttachments.CLUB_SPAWN_ROLLED, true);

            if (isEligibleMob(mob)) {
                boolean isBaby = mob instanceof Zombie zombie && zombie.isBaby();
                double macheteChance = isEnabled() ? getMobSpawnChance() : 0.0D;
                double clubChance = (parent != null && parent.isClubEnabled() && !isBaby)
                        ? parent.getClubTweaks().getMobSpawnChance()
                        : 0.0D;

                if (macheteChance <= 0.0D && clubChance <= 0.0D) {
                    return;
                }

                double roll = mob.getRandom().nextDouble();
                if (roll < macheteChance) {
                    equipMachete(mob);
                } else if (roll < macheteChance + clubChance) {
                    ClubTweaks.equipClub(mob);
                }
            }
        }
    }

    public static boolean isEligibleMob(Mob mob) {
        if (mob.getMainHandItem().getItem() instanceof CrossbowItem) {
            return false;
        }
        EntityType<?> type = mob.getType();
        return type == EntityType.ZOMBIE
                || type == EntityType.HUSK
                || type == EntityType.DROWNED
                || type == EntityType.ZOMBIE_VILLAGER;
    }

    public static void equipMachete(Mob mob) {
        ItemStack machete = new ItemStack(OnetoysItems.MACHETE.get());
        if (mob.level() instanceof ServerLevel serverLevel) {
            DifficultyInstance difficulty = serverLevel.getCurrentDifficultyAt(mob.blockPosition());
            RandomSource random = mob.getRandom();
            if (random.nextFloat() < 0.25F * difficulty.getSpecialMultiplier()) {
                EnchantmentHelper.enchantItemFromProvider(
                        machete,
                        serverLevel.registryAccess(),
                        VanillaEnchantmentProviders.MOB_SPAWN_EQUIPMENT,
                        difficulty,
                        random
                );
            }
        }
        mob.setItemSlot(EquipmentSlot.MAINHAND, machete);
        mob.setDropChance(EquipmentSlot.MAINHAND, 0.085F);
    }
}
