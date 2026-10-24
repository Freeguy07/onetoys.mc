package com.onetoys.module.gameplay.mobs;

import com.onetoys.Onetoys;
import com.onetoys.module.gameplay.GameplayTweaksModule;
import com.onetoys.module.gameplay.mobs.goal.SlipperySlimeDashGoal;
import com.onetoys.module.gameplay.mobs.goal.SmartSkeletonGoal;
import com.onetoys.module.gameplay.mobs.goal.SpittingSpiderGoal;
import com.onetoys.module.gameplay.mobs.goal.WitherSkeletonWarlockGoal;
import com.onetoys.registry.OnetoysAttachments;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.LeapAtTargetGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.monster.AbstractSkeleton;
import net.minecraft.world.entity.monster.MagmaCube;
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.entity.monster.Spider;
import net.minecraft.world.entity.monster.WitherSkeleton;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;

/**
 * Subsystem and configuration controller for Special Mobs mechanics.
 */
public class SpecialMobTweaks {
    private static SpecialMobTweaks instance;

    public static final String TYPE_NONE = "";
    public static final String TYPE_RUNNING_ZOMBIE = "running_zombie";
    public static final String TYPE_SLOW_BABY_ZOMBIE = "slow_baby_zombie";
    public static final String TYPE_SMART_SKELETON = "smart_skeleton";
    public static final String TYPE_SLIPPERY_SLIME = "slippery_slime";
    public static final String TYPE_SPITTING_SPIDER = "spitting_spider";
    public static final String TYPE_WARLOCK_WITHER_SKELETON = "warlock_wither_skeleton";

    public static final ResourceLocation RUNNING_ZOMBIE_SPEED_ID =
            ResourceLocation.fromNamespaceAndPath(Onetoys.MODID, "running_zombie_speed");
    public static final ResourceLocation SLOW_BABY_ZOMBIE_SPEED_ID =
            ResourceLocation.fromNamespaceAndPath(Onetoys.MODID, "slow_baby_zombie_speed");

    // Vanilla zombie base speed is 0.23; adding +0.35 multiplied total gives ~0.3105 (player sprint speed)
    private static final AttributeModifier RUNNING_ZOMBIE_SPEED_MODIFIER = new AttributeModifier(
            RUNNING_ZOMBIE_SPEED_ID,
            0.35D,
            AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
    );

    // Vanilla baby zombie has +0.50 multiplied base (~0.345); multiplying total by -0.333 brings it to ~0.23 (player walk speed)
    private static final AttributeModifier SLOW_BABY_ZOMBIE_SPEED_MODIFIER = new AttributeModifier(
            SLOW_BABY_ZOMBIE_SPEED_ID,
            -0.3333D,
            AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
    );

    private ModConfigSpec.BooleanValue enabledConfig;
    private ModConfigSpec.DoubleValue specialMobChanceConfig;
    private ModConfigSpec.BooleanValue runningZombieConfig;
    private ModConfigSpec.BooleanValue slowBabyZombieConfig;
    private ModConfigSpec.BooleanValue smartSkeletonConfig;
    private ModConfigSpec.BooleanValue slipperySlimeConfig;
    private ModConfigSpec.BooleanValue spittingSpiderConfig;
    private ModConfigSpec.BooleanValue warlockWitherSkeletonConfig;

    public SpecialMobTweaks() {
        instance = this;
    }

    public static SpecialMobTweaks get() {
        return instance;
    }

    public void buildConfig(ModConfigSpec.Builder builder) {
        builder.comment("Special monster variants and combat behaviors.")
                .translation("onetoys.configuration.gameplay_tweaks.mob_tweaks.special_mobs")
                .push("special_mobs");

        enabledConfig = builder
                .comment("Master switch for special monster behaviors and variants.")
                .translation("onetoys.configuration.gameplay_tweaks.mob_tweaks.special_mobs.enabled")
                .define("enabled", true);

        specialMobChanceConfig = builder
                .comment("Chance (0.0 - 1.0) for a newly spawned monster to gain a special ability (0.25 = 25%).")
                .translation("onetoys.configuration.gameplay_tweaks.mob_tweaks.special_mobs.special_mob_chance")
                .defineInRange("special_mob_chance", 0.25D, 0.0D, 1.0D);

        runningZombieConfig = builder
                .comment("Running Zombie: adult zombies that move at player sprinting speed.")
                .translation("onetoys.configuration.gameplay_tweaks.mob_tweaks.special_mobs.running_zombie")
                .define("running_zombie", true);

        slowBabyZombieConfig = builder
                .comment("Slow Baby Zombie: baby zombies that walk at normal player walking speed instead of sprinting.")
                .translation("onetoys.configuration.gameplay_tweaks.mob_tweaks.special_mobs.slow_baby_zombie")
                .define("slow_baby_zombie", true);

        smartSkeletonConfig = builder
                .comment("Smart Skeleton: switches its bow for a bone at close range (dealing stone sword damage) and switches back to its bow when the target retreats.")
                .translation("onetoys.configuration.gameplay_tweaks.mob_tweaks.special_mobs.smart_skeleton")
                .define("smart_skeleton", true);

        slipperySlimeConfig = builder
                .comment("Slippery Slime: moves in short ground slides instead of jumping, and stops to swell up before launching a strong forward ground dash that can step up 1 block.")
                .translation("onetoys.configuration.gameplay_tweaks.mob_tweaks.special_mobs.slippery_slime")
                .define("slippery_slime", true);

        spittingSpiderConfig = builder
                .comment("Spitting Spider: keeps its distance instead of melee attacking and spits Tangled Thread that deals 0.1 damage and places cobwebs.")
                .translation("onetoys.configuration.gameplay_tweaks.mob_tweaks.special_mobs.spitting_spider")
                .define("spitting_spider", true);

        warlockWitherSkeletonConfig = builder
                .comment("Warlock Wither Skeleton: spawns unarmed, keeps its distance from targets, shoots normal wither skulls every few seconds and a dangerous blue wither skull every 30 seconds.")
                .translation("onetoys.configuration.gameplay_tweaks.mob_tweaks.special_mobs.warlock_wither_skeleton")
                .define("warlock_wither_skeleton", true);

        builder.pop();
    }

    public boolean isEnabled() {
        return enabledConfig == null || enabledConfig.get();
    }

    public static boolean isTweakEnabled() {
        return GameplayTweaksModule.getInstance() != null
                && GameplayTweaksModule.getInstance().isEnabled()
                && GameplayTweaksModule.getInstance().getMobTweaks().isEnabled()
                && instance != null
                && instance.isEnabled();
    }

    public static double getSpecialMobChance() {
        if (!isTweakEnabled()) {
            return 0.0D;
        }
        return instance.specialMobChanceConfig != null ? instance.specialMobChanceConfig.get() : 0.25D;
    }

    public static boolean isRunningZombieEnabled() {
        return isTweakEnabled() && (instance.runningZombieConfig == null || instance.runningZombieConfig.get());
    }

    public static boolean isSlowBabyZombieEnabled() {
        return isTweakEnabled() && (instance.slowBabyZombieConfig == null || instance.slowBabyZombieConfig.get());
    }

    public static boolean isSmartSkeletonEnabled() {
        return isTweakEnabled() && (instance.smartSkeletonConfig == null || instance.smartSkeletonConfig.get());
    }

    public static boolean isSlipperySlimeEnabled() {
        return isTweakEnabled() && (instance.slipperySlimeConfig == null || instance.slipperySlimeConfig.get());
    }

    public static boolean isSpittingSpiderEnabled() {
        return isTweakEnabled() && (instance.spittingSpiderConfig == null || instance.spittingSpiderConfig.get());
    }

    public static boolean isWarlockWitherSkeletonEnabled() {
        return isTweakEnabled() && (instance.warlockWitherSkeletonConfig == null || instance.warlockWitherSkeletonConfig.get());
    }

    public static boolean isTypeEnabled(String type) {
        return switch (type) {
            case TYPE_RUNNING_ZOMBIE -> isRunningZombieEnabled();
            case TYPE_SLOW_BABY_ZOMBIE -> isSlowBabyZombieEnabled();
            case TYPE_SMART_SKELETON -> isSmartSkeletonEnabled();
            case TYPE_SLIPPERY_SLIME -> isSlipperySlimeEnabled();
            case TYPE_SPITTING_SPIDER -> isSpittingSpiderEnabled();
            case TYPE_WARLOCK_WITHER_SKELETON -> isWarlockWitherSkeletonEnabled();
            default -> false;
        };
    }

    public static boolean isSpecialMob(Entity entity, String expectedType) {
        if (entity == null) {
            return false;
        }
        return expectedType.equals(entity.getData(OnetoysAttachments.SPECIAL_MOB_TYPE));
    }

    public void onConstruct(IEventBus modBus) {
        NeoForge.EVENT_BUS.addListener(this::onEntityJoinLevel);
        NeoForge.EVENT_BUS.addListener(this::onLivingDeath);
    }

    private void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide() || !(event.getEntity() instanceof Mob mob)) {
            return;
        }
        if (!isTweakEnabled()) {
            return;
        }

        if (!mob.getData(OnetoysAttachments.SPECIAL_MOB_ROLLED)) {
            mob.setData(OnetoysAttachments.SPECIAL_MOB_ROLLED, true);
            double chance = getSpecialMobChance();
            if (chance > 0.0D && mob.getRandom().nextDouble() < chance) {
                String assignedType = determineSpecialTypeForMob(mob);
                if (!assignedType.isEmpty()) {
                    mob.setData(OnetoysAttachments.SPECIAL_MOB_TYPE, assignedType);
                }
            }
        }

        String currentType = mob.getData(OnetoysAttachments.SPECIAL_MOB_TYPE);
        if (!currentType.isEmpty()) {
            applySpecialBehaviorToMob(mob, currentType);
        }
    }

    private String determineSpecialTypeForMob(Mob mob) {
        if (mob instanceof Zombie zombie) {
            if (zombie.isBaby()) {
                return isSlowBabyZombieEnabled() ? TYPE_SLOW_BABY_ZOMBIE : TYPE_NONE;
            } else {
                return isRunningZombieEnabled() ? TYPE_RUNNING_ZOMBIE : TYPE_NONE;
            }
        }
        if (mob instanceof WitherSkeleton) {
            return isWarlockWitherSkeletonEnabled() ? TYPE_WARLOCK_WITHER_SKELETON : TYPE_NONE;
        }
        if (mob instanceof AbstractSkeleton skeleton) {
            boolean hasBow = skeleton.getMainHandItem().getItem() instanceof BowItem
                    || skeleton.getOffhandItem().getItem() instanceof BowItem;
            return (hasBow && isSmartSkeletonEnabled()) ? TYPE_SMART_SKELETON : TYPE_NONE;
        }
        if (mob instanceof Slime && !(mob instanceof MagmaCube)) {
            return isSlipperySlimeEnabled() ? TYPE_SLIPPERY_SLIME : TYPE_NONE;
        }
        if (mob instanceof Spider) {
            return isSpittingSpiderEnabled() ? TYPE_SPITTING_SPIDER : TYPE_NONE;
        }
        return TYPE_NONE;
    }

    private void applySpecialBehaviorToMob(Mob mob, String type) {
        switch (type) {
            case TYPE_RUNNING_ZOMBIE -> {
                if (isRunningZombieEnabled() && mob instanceof Zombie zombie && !zombie.isBaby()) {
                    AttributeInstance speed = zombie.getAttribute(Attributes.MOVEMENT_SPEED);
                    if (speed != null && !speed.hasModifier(RUNNING_ZOMBIE_SPEED_ID)) {
                        speed.addTransientModifier(RUNNING_ZOMBIE_SPEED_MODIFIER);
                    }
                }
            }
            case TYPE_SLOW_BABY_ZOMBIE -> {
                if (isSlowBabyZombieEnabled() && mob instanceof Zombie zombie && zombie.isBaby()) {
                    AttributeInstance speed = zombie.getAttribute(Attributes.MOVEMENT_SPEED);
                    if (speed != null && !speed.hasModifier(SLOW_BABY_ZOMBIE_SPEED_ID)) {
                        speed.addTransientModifier(SLOW_BABY_ZOMBIE_SPEED_MODIFIER);
                    }
                }
            }
            case TYPE_SMART_SKELETON -> {
                if (isSmartSkeletonEnabled() && mob instanceof AbstractSkeleton skeleton) {
                    skeleton.removeAllGoals(goal -> goal instanceof SmartSkeletonGoal);
                    skeleton.goalSelector.addGoal(1, new SmartSkeletonGoal(skeleton));
                }
            }
            case TYPE_SLIPPERY_SLIME -> {
                if (isSlipperySlimeEnabled() && mob instanceof Slime slime) {
                    slime.removeAllGoals(goal -> goal instanceof SlipperySlimeDashGoal);
                    slime.goalSelector.addGoal(1, new SlipperySlimeDashGoal(slime));
                }
            }
            case TYPE_SPITTING_SPIDER -> {
                if (isSpittingSpiderEnabled() && mob instanceof Spider spider) {
                    spider.removeAllGoals(goal -> goal instanceof MeleeAttackGoal || goal instanceof LeapAtTargetGoal || goal instanceof SpittingSpiderGoal);
                    spider.goalSelector.addGoal(1, new SpittingSpiderGoal(spider));
                }
            }
            case TYPE_WARLOCK_WITHER_SKELETON -> {
                if (isWarlockWitherSkeletonEnabled() && mob instanceof WitherSkeleton witherSkeleton) {
                    witherSkeleton.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
                    witherSkeleton.setItemSlot(EquipmentSlot.OFFHAND, ItemStack.EMPTY);
                    witherSkeleton.removeAllGoals(goal -> goal instanceof MeleeAttackGoal || goal instanceof WitherSkeletonWarlockGoal);
                    witherSkeleton.goalSelector.addGoal(1, new WitherSkeletonWarlockGoal(witherSkeleton));
                }
            }
        }
    }

    private void onLivingDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof AbstractSkeleton skeleton && !skeleton.level().isClientSide()) {
            ItemStack storedBow = skeleton.getData(OnetoysAttachments.SMART_SKELETON_STORED_BOW);
            if (!storedBow.isEmpty()) {
                skeleton.setData(OnetoysAttachments.SMART_SKELETON_STORED_BOW, ItemStack.EMPTY);
                skeleton.setItemSlot(EquipmentSlot.MAINHAND, storedBow);
            }
        }
    }
}
