package com.onetoys.module.gameplay.items;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

import com.onetoys.module.ModuleManager;
import com.onetoys.module.gameplay.GameplayTweaksModule;

import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.damagesource.DamageContainer;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

public final class SwordBlockingTweaks {
    public static final int SWORD_DISABLE_COOLDOWN_TICKS = 50;

    private static SwordBlockingTweaks instance;

    private final ItemTweaks parent;
    private ModConfigSpec.BooleanValue enabledConfig;
    private ModConfigSpec.DoubleValue damageReductionConfig;
    private ModConfigSpec.DoubleValue durabilityDamageRatioConfig;

    private final Map<DamageContainer, Float> pendingBlockedDamage = Collections.synchronizedMap(new WeakHashMap<>());

    public SwordBlockingTweaks(ItemTweaks parent) {
        this.parent = parent;
        instance = this;
    }

    public static SwordBlockingTweaks get() {
        return instance;
    }

    public void buildConfig(ModConfigSpec.Builder builder) {
        builder.comment("Restores classic pre-1.9 sword blocking mechanics and adjusts block durability loss.")
                .translation("onetoys.configuration.gameplay_tweaks.item_tweaks.sword_blocking")
                .push("sword_blocking");

        enabledConfig = builder
                .comment("Enable classic sword blocking on right-click and unified block durability formula.")
                .translation("onetoys.configuration.gameplay_tweaks.item_tweaks.sword_blocking.enabled")
                .define("enabled", true);

        damageReductionConfig = builder
                .comment("Fraction of incoming frontal damage blocked when blocking with a sword (0.5 = 50% damage reduction).")
                .translation("onetoys.configuration.gameplay_tweaks.item_tweaks.sword_blocking.damage_reduction")
                .defineInRange("damage_reduction", 0.5, 0.0, 1.0);

        durabilityDamageRatioConfig = builder
                .comment("Fraction of blocked damage deducted from the blocking sword or shield durability, rounded up (0.5 = half of blocked damage, min 1).")
                .translation("onetoys.configuration.gameplay_tweaks.item_tweaks.sword_blocking.durability_damage_ratio")
                .defineInRange("durability_damage_ratio", 0.5, 0.0, 10.0);

        builder.pop();
    }

    public boolean isEnabled() {
        return enabledConfig == null || enabledConfig.get();
    }

    public double getDamageReduction() {
        return damageReductionConfig == null ? 0.5 : damageReductionConfig.get();
    }

    public double getDurabilityDamageRatio() {
        return durabilityDamageRatioConfig == null ? 0.5 : durabilityDamageRatioConfig.get();
    }

    public static boolean isTweakEnabled() {
        if (!ModuleManager.get().isModuleEnabled(GameplayTweaksModule.ID)) {
            return false;
        }
        SwordBlockingTweaks tweaks = instance;
        if (tweaks == null) {
            return false;
        }
        if (tweaks.parent != null && !tweaks.parent.isEnabled()) {
            return false;
        }
        return tweaks.isEnabled();
    }

    public static int calculateBlockDurabilityDamage(float blockedDamage) {
        if (blockedDamage <= 0.0F) {
            return 0;
        }
        SwordBlockingTweaks tweaks = instance;
        double ratio = tweaks != null ? tweaks.getDurabilityDamageRatio() : 0.5D;
        if (ratio <= 0.0D) {
            return 0;
        }
        return Math.max(1, Mth.ceil(blockedDamage * (float) ratio));
    }

    public static boolean isSword(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() instanceof SwordItem;
    }

    public static boolean isBlockingWithSword(LivingEntity entity) {
        if (!isTweakEnabled() || !entity.isUsingItem() || entity.getUseItemRemainingTicks() <= 0) {
            return false;
        }
        ItemStack useItem = entity.getUseItem();
        return isSword(useItem) && useItem.getUseAnimation() == UseAnim.BLOCK;
    }

    public static boolean canBlockDamageSource(LivingEntity entity, DamageSource damageSource) {
        if (!isBlockingWithSword(entity)) {
            return false;
        }
        if (damageSource.is(DamageTypeTags.BYPASSES_SHIELD)) {
            return false;
        }
        Entity directEntity = damageSource.getDirectEntity();
        if (directEntity instanceof AbstractArrow arrow && arrow.getPierceLevel() > 0) {
            return false;
        }
        Vec3 sourcePos = damageSource.getSourcePosition();
        if (sourcePos != null) {
            Vec3 viewVec = entity.calculateViewVector(0.0F, entity.getYHeadRot());
            Vec3 toEntity = sourcePos.vectorTo(entity.position());
            toEntity = new Vec3(toEntity.x, 0.0D, toEntity.z).normalize();
            return toEntity.dot(viewVec) < 0.0D;
        }
        return false;
    }

    private static boolean canDisableSwordBlock(LivingEntity attacker, ItemStack blockingSword, LivingEntity blocker) {
        ItemStack weapon = attacker.getMainHandItem();
        if (weapon.isEmpty()) {
            return false;
        }
        return weapon.getItem() instanceof AxeItem
                || attacker.canDisableShield()
                || weapon.canDisableShield(blockingSword, blocker, attacker);
    }

    public void onConstruct(IEventBus modBus) {
        NeoForge.EVENT_BUS.addListener(this::onLivingIncomingDamage);
        NeoForge.EVENT_BUS.addListener(this::onLivingDamagePre);
    }

    private void onLivingIncomingDamage(LivingIncomingDamageEvent event) {
        if (!isTweakEnabled()) {
            return;
        }
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide()) {
            return;
        }
        float currentDamage = event.getAmount();
        if (currentDamage <= 0.0F) {
            return;
        }
        DamageSource source = event.getSource();
        if (!canBlockDamageSource(entity, source)) {
            return;
        }

        float reductionFraction = (float) getDamageReduction();
        if (reductionFraction <= 0.0F) {
            return;
        }

        float blockedDamage = currentDamage * reductionFraction;
        float newDamage = Math.max(0.0F, currentDamage - blockedDamage);
        event.setAmount(newDamage);

        if (blockedDamage > 0.0F) {
            pendingBlockedDamage.put(event.getContainer(), blockedDamage);
        }
    }

    private void onLivingDamagePre(LivingDamageEvent.Pre event) {
        Float blockedDamage = pendingBlockedDamage.remove(event.getContainer());
        if (blockedDamage == null || blockedDamage <= 0.0F) {
            return;
        }

        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide() || !isBlockingWithSword(entity)) {
            return;
        }

        InteractionHand usedHand = entity.getUsedItemHand();
        ItemStack useItem = entity.getUseItem();
        Item swordItem = useItem.getItem();

        int durabilityDamage = calculateBlockDurabilityDamage(blockedDamage);
        if (durabilityDamage > 0) {
            EquipmentSlot slot = LivingEntity.getSlotForHand(usedHand);
            useItem.hurtAndBreak(durabilityDamage, entity, slot);
            if (useItem.isEmpty()) {
                entity.stopUsingItem();
            }
        }

        DamageSource source = event.getSource();
        if (!source.is(DamageTypeTags.IS_PROJECTILE)
                && source.getDirectEntity() instanceof LivingEntity attacker
                && entity instanceof Player player
                && canDisableSwordBlock(attacker, useItem, player)) {
            player.getCooldowns().addCooldown(swordItem, SWORD_DISABLE_COOLDOWN_TICKS);
            player.stopUsingItem();
            player.level().broadcastEntityEvent(player, (byte) 30);
        }
    }
}
