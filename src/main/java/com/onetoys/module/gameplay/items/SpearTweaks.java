package com.onetoys.module.gameplay.items;

import com.onetoys.registry.OnetoysAttachments;
import com.onetoys.registry.OnetoysItems;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.AbstractSkeleton;
import net.minecraft.world.item.ItemStack;
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
                .comment("Chance (0.0 - 1.0) for humanoid monsters (zombies, skeletons) to spawn with spears.")
                .translation("onetoys.configuration.gameplay_tweaks.item_tweaks.spear.mob_spawn_chance")
                .defineInRange("mob_spawn_chance", 0.15D, 0.0D, 1.0D);

        builder.pop();
    }

    public boolean isEnabled() {
        return (parent == null || parent.isEnabled()) && (enabledConfig == null || enabledConfig.get());
    }

    public double getMobSpawnChance() {
        if (!isEnabled()) {
            return 0.0D;
        }
        return mobSpawnChanceConfig != null ? mobSpawnChanceConfig.get() : 0.15D;
    }

    public void onConstruct(IEventBus modBus) {
        NeoForge.EVENT_BUS.addListener(this::onEntityJoinLevel);
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
        EntityType<?> type = mob.getType();
        return type == EntityType.ZOMBIE
                || type == EntityType.HUSK
                || type == EntityType.ZOMBIE_VILLAGER
                || type == EntityType.SKELETON
                || type == EntityType.STRAY
                || type == EntityType.BOGGED;
    }

    public static void equipSpear(Mob mob) {
        mob.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(OnetoysItems.SPEAR.get()));
        mob.setDropChance(EquipmentSlot.MAINHAND, 0.085F);
        if (mob instanceof AbstractSkeleton skeleton) {
            skeleton.reassessWeaponGoal();
        }
    }
}
