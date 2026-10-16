package com.onetoys.module.gameplay.wetness;

import java.util.List;

import com.onetoys.module.gameplay.GameplayTweaksModule;
import com.onetoys.registry.OnetoysAttachments;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrownPotion;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.block.CropGrowEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

/**
 * Controller and configuration coordinator for wetness tweaks and mechanics.
 * Manages water exposure, drying, heat source acceleration, extinguishing,
 * spider climbing prevention, creeper explosion cancellation, and matted wool shearing.
 */
public class WetnessTweaks {
    public static final TagKey<EntityType<?>> WETNESSPROOF =
            TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.fromNamespaceAndPath("onetoys", "wetnessproof"));
    public static final TagKey<EntityType<?>> C_WETNESSPROOF =
            TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.fromNamespaceAndPath("c", "wetnessproof"));

    public static final TagKey<Block> HEAT_SOURCES =
            TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath("onetoys", "heat_sources"));
    public static final TagKey<Block> C_HEAT_SOURCES =
            TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath("c", "heat_sources"));

    private static final ThreadLocal<BlockPos.MutableBlockPos> MUTABLE_POS = ThreadLocal.withInitial(BlockPos.MutableBlockPos::new);

    private static WetnessTweaks instance;

    private final GameplayTweaksModule parent;
    private ModConfigSpec.BooleanValue enabledConfig;
    private ModConfigSpec.IntValue wetDurationTicksConfig;
    private ModConfigSpec.IntValue heatSourceSpeedupConfig;
    private ModConfigSpec.BooleanValue acceleratedFireExtinguishConfig;
    private ModConfigSpec.DoubleValue fireNegateChanceConfig;
    private ModConfigSpec.BooleanValue wetSpidersCannotClimbConfig;
    private ModConfigSpec.BooleanValue wetCreepersCannotExplodeConfig;
    private ModConfigSpec.BooleanValue mattedWoolFromWetSheepConfig;
    private ModConfigSpec.BooleanValue rainSpeedsUpCropGrowthConfig;
    private ModConfigSpec.BooleanValue stableFarmlandConfig;

    public WetnessTweaks(GameplayTweaksModule parent) {
        this.parent = parent;
        instance = this;
    }

    public WetnessTweaks() {
        this(null);
    }

    public void buildConfig(ModConfigSpec.Builder builder) {
        builder.comment("Tweaks and mechanics related to water exposure, rain, and wetness.")
                .translation("onetoys.configuration.gameplay_tweaks.wetness")
                .push("wetness");

        enabledConfig = builder
                .comment("Master switch to enable or disable all wet tweaks.")
                .translation("onetoys.configuration.gameplay_tweaks.wetness.enabled")
                .define("enabled", true);

        wetDurationTicksConfig = builder
                .comment("Duration in ticks an entity remains wet after leaving water or rain (20 ticks = 1 second).")
                .translation("onetoys.configuration.gameplay_tweaks.wetness.wet_duration_ticks")
                .defineInRange("wet_duration_ticks", 600, 20, 72000);

        heatSourceSpeedupConfig = builder
                .comment("Drying rate multiplier when near an active heat source (campfires, furnaces, lava, fire).")
                .translation("onetoys.configuration.gameplay_tweaks.wetness.heat_source_speedup")
                .defineInRange("heat_source_speedup", 5, 1, 50);

        acceleratedFireExtinguishConfig = builder
                .comment("Whether wet entities extinguish active fire significantly faster.")
                .translation("onetoys.configuration.gameplay_tweaks.wetness.accelerated_fire_extinguish")
                .define("accelerated_fire_extinguish", true);

        fireNegateChanceConfig = builder
                .comment("Chance (0.0 - 1.0) for a wet entity to completely negate catching fire / fire damage.")
                .translation("onetoys.configuration.gameplay_tweaks.wetness.fire_negate_chance")
                .defineInRange("fire_negate_chance", 0.25, 0.0, 1.0);

        wetSpidersCannotClimbConfig = builder
                .comment("Whether wet spiders lose the ability to climb walls.")
                .translation("onetoys.configuration.gameplay_tweaks.wetness.wet_spiders_cannot_climb")
                .define("wet_spiders_cannot_climb", true);

        wetCreepersCannotExplodeConfig = builder
                .comment("Whether wet creepers cannot explode and fizzle out with steam.")
                .translation("onetoys.configuration.gameplay_tweaks.wetness.wet_creepers_cannot_explode")
                .define("wet_creepers_cannot_explode", true);

        mattedWoolFromWetSheepConfig = builder
                .comment("Whether shearing a wet sheep yields matted wool instead of standard colored wool.")
                .translation("onetoys.configuration.gameplay_tweaks.wetness.matted_wool_from_wet_sheep")
                .define("matted_wool_from_wet_sheep", true);

        rainSpeedsUpCropGrowthConfig = builder
                .comment("When enabled, crops exposed to rain grow significantly faster during rainstorms.")
                .translation("onetoys.configuration.gameplay_tweaks.wetness.rain_speeds_up_crop_growth")
                .define("rain_speeds_up_crop_growth", true);

        stableFarmlandConfig = builder
                .comment("When enabled, dry farmland does not revert to dirt on its own over time.",
                        "Mobs and player trampling are unaffected and can still revert farmland to dirt normally.")
                .translation("onetoys.configuration.gameplay_tweaks.wetness.stable_farmland")
                .define("stable_farmland", true);

        builder.pop();
    }

    public boolean isEnabled() {
        if (parent != null && !parent.isEnabled()) {
            return false;
        }
        return enabledConfig == null || enabledConfig.get();
    }

    public boolean isStableFarmland() {
        return isEnabled() && (stableFarmlandConfig == null || stableFarmlandConfig.get());
    }

    public static boolean isStableFarmlandEnabled() {
        return instance != null && instance.isStableFarmland();
    }

    public static boolean isTweakEnabled() {
        return instance != null && instance.isEnabled();
    }

    public static boolean isWet(LivingEntity entity) {
        if (entity == null || !isTweakEnabled()) {
            return false;
        }
        return entity.hasData(OnetoysAttachments.IS_WET) && Boolean.TRUE.equals(entity.getData(OnetoysAttachments.IS_WET));
    }

    public static boolean isWetnessproof(LivingEntity entity) {
        if (entity == null) {
            return true;
        }
        return entity.getType().is(WETNESSPROOF) || entity.getType().is(C_WETNESSPROOF);
    }

    public static void setWet(LivingEntity entity, int durationTicks) {
        if (entity == null || !isTweakEnabled() || isWetnessproof(entity)) {
            return;
        }
        long targetUntil = entity.level().getGameTime() + durationTicks;
        long currentUntil = entity.hasData(OnetoysAttachments.WET_UNTIL) ? entity.getData(OnetoysAttachments.WET_UNTIL) : 0L;
        if (currentUntil < targetUntil) {
            entity.setData(OnetoysAttachments.WET_UNTIL, targetUntil);
        }
        if (!entity.hasData(OnetoysAttachments.IS_WET) || !Boolean.TRUE.equals(entity.getData(OnetoysAttachments.IS_WET))) {
            entity.setData(OnetoysAttachments.IS_WET, true);
        }
    }

    public static void clearWet(LivingEntity entity) {
        if (entity == null) {
            return;
        }
        if (entity.hasData(OnetoysAttachments.WET_UNTIL)) {
            entity.removeData(OnetoysAttachments.WET_UNTIL);
        }
        if (entity.hasData(OnetoysAttachments.IS_WET) && Boolean.TRUE.equals(entity.getData(OnetoysAttachments.IS_WET))) {
            entity.removeData(OnetoysAttachments.IS_WET);
        }
    }

    public static int getWetDurationTicks() {
        return instance != null && instance.wetDurationTicksConfig != null ? instance.wetDurationTicksConfig.get() : 600;
    }

    public static int getHeatSourceSpeedup() {
        return instance != null && instance.heatSourceSpeedupConfig != null ? instance.heatSourceSpeedupConfig.get() : 5;
    }

    public static boolean isAcceleratedFireExtinguish() {
        return instance != null && instance.acceleratedFireExtinguishConfig != null && instance.acceleratedFireExtinguishConfig.get();
    }

    public static double getFireNegateChance() {
        return instance != null && instance.fireNegateChanceConfig != null ? instance.fireNegateChanceConfig.get() : 0.25;
    }

    public static boolean isWetSpidersCannotClimb() {
        return isTweakEnabled() && (instance.wetSpidersCannotClimbConfig == null || instance.wetSpidersCannotClimbConfig.get());
    }

    public static boolean isWetCreepersCannotExplode() {
        return isTweakEnabled() && (instance.wetCreepersCannotExplodeConfig == null || instance.wetCreepersCannotExplodeConfig.get());
    }

    public static boolean isMattedWoolFromWetSheep() {
        return isTweakEnabled() && (instance.mattedWoolFromWetSheepConfig == null || instance.mattedWoolFromWetSheepConfig.get());
    }

    public boolean isRainSpeedsUpCropGrowth() {
        return isEnabled() && (rainSpeedsUpCropGrowthConfig == null || rainSpeedsUpCropGrowthConfig.get());
    }

    public static boolean isRainSpeedsUpCropGrowthActive() {
        return isTweakEnabled() && (instance.rainSpeedsUpCropGrowthConfig == null || instance.rainSpeedsUpCropGrowthConfig.get());
    }

    public static boolean isNearHeatSource(LivingEntity entity) {
        Level level = entity.level();
        int cx = entity.getBlockX();
        int cy = entity.getBlockY();
        int cz = entity.getBlockZ();

        BlockPos.MutableBlockPos pos = MUTABLE_POS.get();
        pos.set(cx, cy, cz);

        if (!level.isLoaded(pos)) {
            return false;
        }

        // Fast path: check directly underneath and at entity's feet (most common: standing on campfire/furnace)
        pos.set(cx, cy - 1, cz);
        if (isHeatSourceBlock(level.getBlockState(pos))) {
            return true;
        }
        pos.set(cx, cy, cz);
        if (isHeatSourceBlock(level.getBlockState(pos))) {
            return true;
        }

        // Check surrounding 3x3x3 proximity (dy = -1, 0, 1)
        for (int dy = -1; dy <= 1; dy++) {
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (dx == 0 && dz == 0 && (dy == -1 || dy == 0)) {
                        continue; // Already checked
                    }
                    pos.set(cx + dx, cy + dy, cz + dz);
                    if (isHeatSourceBlock(level.getBlockState(pos))) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static boolean isHeatSourceBlock(BlockState state) {
        if (state.isAir()) {
            return false;
        }
        if (state.is(HEAT_SOURCES) || state.is(C_HEAT_SOURCES) || state.is(com.onetoys.salt.SaltTags.Blocks.HEATERS)) {
            return !state.hasProperty(BlockStateProperties.LIT) || state.getValue(BlockStateProperties.LIT);
        }
        return false;
    }

    public void onConstruct(IEventBus modBus) {
        NeoForge.EVENT_BUS.addListener(this::onEntityTick);
        NeoForge.EVENT_BUS.addListener(this::onEntityInteract);
        NeoForge.EVENT_BUS.addListener(this::onProjectileImpact);
        NeoForge.EVENT_BUS.addListener(this::onLivingIncomingDamage);
        NeoForge.EVENT_BUS.addListener(this::onCropGrowPre);
    }

    private void onCropGrowPre(CropGrowEvent.Pre event) {
        if (!(event.getLevel() instanceof Level level) || !level.isRaining()) {
            return;
        }
        if (!isRainSpeedsUpCropGrowthActive()) {
            return;
        }
        BlockPos pos = event.getPos();
        BlockPos.MutableBlockPos mpos = MUTABLE_POS.get();
        mpos.set(pos.getX(), pos.getY() + 1, pos.getZ());
        if (level.isRainingAt(mpos)) {
            event.setResult(CropGrowEvent.Pre.Result.GROW);
        }
    }

    public void onCommonSetup(FMLCommonSetupEvent event) {
    }

    public void onClientSetup(FMLClientSetupEvent event) {
    }

    private void onEntityTick(EntityTickEvent.Pre event) {
        // Fast-path 1: Client particle ticking is handled by WetnessClientHelper
        if (event.getEntity().level().isClientSide()) {
            return;
        }

        // Fast-path 2: Master toggle check
        if (!isTweakEnabled()) {
            return;
        }

        // Fast-path 3: Only living entities track wetness
        if (!(event.getEntity() instanceof LivingEntity entity)) {
            return;
        }

        // Stagger checks evenly across 10 ticks using entity ID + tickCount to prevent frame spikes
        int phase = (entity.getId() + entity.tickCount) % 10;
        boolean isWet = isWet(entity);
        boolean inWater = entity.isInWaterOrBubble();
        Level level = entity.level();
        boolean isRaining = level.isRaining();

        // If entity is dry, out of water, and not raining (or not in its staggered check phase), exit in < 1ns
        if (!isWet && !inWater && (!isRaining || phase != 0)) {
            return;
        }

        // Fast-path 4: Entities immune to wetness (blazes, magma cubes, striders, etc.)
        if (isWetnessproof(entity)) {
            if (isWet) {
                clearWet(entity);
            }
            return;
        }

        // Accelerated fire extinguishing for wet entities
        if (isWet && isAcceleratedFireExtinguish() && entity.isOnFire()) {
            int fireTicks = entity.getRemainingFireTicks();
            if (fireTicks > 0) {
                entity.setRemainingFireTicks(Math.max(0, fireTicks - 4));
                if (level instanceof ServerLevel serverLevel && entity.tickCount % 5 == 0) {
                    serverLevel.sendParticles(ParticleTypes.SMOKE, entity.getX(), entity.getY() + entity.getBbHeight() * 0.5, entity.getZ(), 2, 0.1, 0.1, 0.1, 0.02);
                }
            }
        }

        // Staggered periodic phase: only evaluate water/rain exposure and natural drying every 10 ticks (0.5s)
        if (phase != 0) {
            return;
        }

        // Exposure check: in water, or raining and directly exposed to sky/rain
        boolean exposedToWater = inWater;
        if (!exposedToWater && isRaining) {
            BlockPos.MutableBlockPos mpos = MUTABLE_POS.get();
            mpos.set(entity.getBlockX(), entity.getBlockY(), entity.getBlockZ());
            exposedToWater = level.isRainingAt(mpos);
        }

        long gameTime = level.getGameTime();
        if (exposedToWater) {
            setWet(entity, getWetDurationTicks());
        } else if (isWet) {
            // Drying when not exposed to water/rain
            long wetUntil = entity.hasData(OnetoysAttachments.WET_UNTIL) ? entity.getData(OnetoysAttachments.WET_UNTIL) : 0L;
            if (entity.isOnFire() || entity.isInLava() || isNearHeatSource(entity)) {
                wetUntil -= (getHeatSourceSpeedup() - 1) * 10L;
                entity.setData(OnetoysAttachments.WET_UNTIL, wetUntil);
                if (level instanceof ServerLevel serverLevel && entity.getRandom().nextInt(3) == 0) {
                    serverLevel.sendParticles(ParticleTypes.SMOKE, entity.getX(), entity.getY() + entity.getBbHeight() * 0.6, entity.getZ(), 2, 0.15, 0.15, 0.15, 0.02);
                }
            }
            if (gameTime >= wetUntil) {
                clearWet(entity);
            }
        }
    }

    private void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (!isTweakEnabled()) {
            return;
        }

        ItemStack stack = event.getItemStack();
        if (stack.isEmpty()) {
            return;
        }

        // 1. Water bottle interaction makes entity wet
        if (stack.is(Items.POTION)) {
            PotionContents contents = stack.get(DataComponents.POTION_CONTENTS);
            if (contents != null && contents.is(Potions.WATER)) {
                if (event.getTarget() instanceof LivingEntity target && !isWetnessproof(target)) {
                    Player player = event.getEntity();
                    Level level = event.getLevel();

                    if (!level.isClientSide()) {
                        setWet(target, getWetDurationTicks());
                        level.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.BOTTLE_EMPTY, SoundSource.PLAYERS, 1.0F, 1.0F);
                        if (level instanceof ServerLevel serverLevel) {
                            serverLevel.sendParticles(ParticleTypes.SPLASH, target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(), 15, target.getBbWidth() * 0.3, target.getBbHeight() * 0.3, target.getBbWidth() * 0.3, 0.1);
                        }
                        if (!player.getAbilities().instabuild) {
                            stack.shrink(1);
                            if (stack.isEmpty()) {
                                player.setItemInHand(event.getHand(), new ItemStack(Items.GLASS_BOTTLE));
                            } else if (!player.getInventory().add(new ItemStack(Items.GLASS_BOTTLE))) {
                                player.drop(new ItemStack(Items.GLASS_BOTTLE), false);
                            }
                            player.containerMenu.broadcastChanges();
                        }
                    }

                    player.swing(event.getHand(), true);
                    event.setCanceled(true);
                    event.setCancellationResult(InteractionResult.sidedSuccess(level.isClientSide()));
                    return;
                }
            }
        }

        // 2. Modded shears fallback for shearing wet sheep
        if (event.getTarget() instanceof Sheep sheep && sheep.readyForShearing() && !event.getLevel().isClientSide()) {
            if (!stack.is(Items.SHEARS) && stack.canPerformAction(ItemAbilities.SHEARS_HARVEST)) {
                sheep.shear(SoundSource.PLAYERS);
                sheep.gameEvent(GameEvent.SHEAR, event.getEntity());
                stack.hurtAndBreak(1, event.getEntity(), LivingEntity.getSlotForHand(event.getHand()));
                event.setCanceled(true);
                event.setCancellationResult(InteractionResult.sidedSuccess(event.getLevel().isClientSide()));
            }
        }
    }

    private void onProjectileImpact(ProjectileImpactEvent event) {
        if (!isTweakEnabled()) {
            return;
        }

        if (event.getProjectile() instanceof ThrownPotion thrownPotion) {
            if (thrownPotion.level().isClientSide()) {
                return;
            }

            ItemStack stack = thrownPotion.getItem();
            PotionContents contents = stack.get(DataComponents.POTION_CONTENTS);
            if (contents != null && contents.is(Potions.WATER)) {
                Vec3 hitPos = event.getRayTraceResult().getLocation();
                AABB splashArea = new AABB(hitPos.x - 4.0, hitPos.y - 2.0, hitPos.z - 4.0, hitPos.x + 4.0, hitPos.y + 2.0, hitPos.z + 4.0);
                List<LivingEntity> entities = thrownPotion.level().getEntitiesOfClass(LivingEntity.class, splashArea);
                for (LivingEntity living : entities) {
                    if (!isWetnessproof(living) && living.distanceToSqr(hitPos) <= 16.0) {
                        setWet(living, getWetDurationTicks());
                    }
                }
            }
        }
    }

    private void onLivingIncomingDamage(LivingIncomingDamageEvent event) {
        if (!isTweakEnabled()) {
            return;
        }

        // Fast path: ignore non-fire damage before querying entity attachments
        if (!event.getSource().is(DamageTypeTags.IS_FIRE)) {
            return;
        }

        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide()) {
            return;
        }

        if (isWet(entity)) {
            if (entity.getRandom().nextDouble() < getFireNegateChance()) {
                event.setCanceled(true);
                entity.clearFire();
                entity.level().playSound(null, entity.getX(), entity.getY(), entity.getZ(), SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 0.7F, 1.2F);
                if (entity.level() instanceof ServerLevel serverLevel) {
                    serverLevel.sendParticles(ParticleTypes.SMOKE, entity.getX(), entity.getY() + entity.getBbHeight() * 0.5, entity.getZ(), 6, 0.2, 0.2, 0.2, 0.05);
                }
            }
        }
    }
}

