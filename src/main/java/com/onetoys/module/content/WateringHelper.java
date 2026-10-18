package com.onetoys.module.content;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;

import com.onetoys.module.gameplay.wetness.WetnessTweaks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.CandleBlock;
import net.minecraft.world.level.block.CandleCakeBlock;
import net.minecraft.world.level.block.ConcretePowderBlock;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Shared utility for applying watering, rain, and hydration effects to blocks, crops,
 * entities, and fluids (used by WateringCanItem).
 */
public final class WateringHelper {

    private static final Map<Block, BlockState> CONCRETE_CACHE = new ConcurrentHashMap<>();

    private WateringHelper() {
    }

    public static void applyWatering(Level level, BlockPos pos, Direction clickedFace, Vec3 hitVec, @Nullable Player player) {
        BlockState state = level.getBlockState(pos);
        applyRainEffects(level, pos, state, player);
        playWateringEffects(level, pos, clickedFace, hitVec);
    }

    public static void applyRainEffects(Level level, BlockPos pos, BlockState state, @Nullable Player player) {
        // A. Extinguish fire at targeted position
        if (state.is(BlockTags.FIRE) || state.getBlock() instanceof BaseFireBlock) {
            level.removeBlock(pos, false);
            level.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.5F, 2.6F);
            return;
        }

        // B. Extinguish fire directly above targeted position
        BlockPos abovePos = pos.above();
        BlockState aboveState = level.getBlockState(abovePos);
        if (aboveState.is(BlockTags.FIRE) || aboveState.getBlock() instanceof BaseFireBlock) {
            level.removeBlock(abovePos, false);
            level.playSound(null, abovePos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.5F, 2.6F);
        }

        // C. Extinguish lit campfires
        if (state.getBlock() instanceof CampfireBlock && state.getValue(CampfireBlock.LIT)) {
            CampfireBlock.dowse(player, level, pos, state);
            level.setBlockAndUpdate(pos, state.setValue(CampfireBlock.LIT, false));
            return;
        }

        // D. Extinguish lit candles and candle cakes
        if (state.getBlock() instanceof CandleBlock && state.getValue(CandleBlock.LIT)) {
            CandleBlock.extinguish(player, state, level, pos);
            return;
        }
        if (state.getBlock() instanceof CandleCakeBlock && state.getValue(CandleCakeBlock.LIT)) {
            CandleCakeBlock.extinguish(player, state, level, pos);
            return;
        }

        // E. Farmland hydration:
        if (state.getBlock() instanceof FarmBlock) {
            if (state.getValue(FarmBlock.MOISTURE) < 7) {
                level.setBlock(pos, state.setValue(FarmBlock.MOISTURE, 7), 2);
            }
        }

        // If block is a crop/plant sitting on farmland, hydrate the farmland beneath:
        BlockPos belowPos = pos.below();
        BlockState belowState = level.getBlockState(belowPos);
        if (belowState.getBlock() instanceof FarmBlock) {
            if (belowState.getValue(FarmBlock.MOISTURE) < 7) {
                level.setBlock(belowPos, belowState.setValue(FarmBlock.MOISTURE, 7), 2);
            }
        }

        // F. Concrete Powder solidification:
        if (state.getBlock() instanceof ConcretePowderBlock) {
            BlockState concreteState = CONCRETE_CACHE.computeIfAbsent(state.getBlock(), powderBlock -> {
                ResourceLocation id = BuiltInRegistries.BLOCK.getKey(powderBlock);
                String path = id.getPath();
                if (path.endsWith("_powder")) {
                    ResourceLocation concreteId = ResourceLocation.fromNamespaceAndPath(
                            id.getNamespace(),
                            path.substring(0, path.length() - 7)
                    );
                    Block concrete = BuiltInRegistries.BLOCK.get(concreteId);
                    if (concrete != null && concrete != Blocks.AIR) {
                        return concrete.defaultBlockState();
                    }
                }
                return Blocks.AIR.defaultBlockState();
            });

            if (!concreteState.isAir()) {
                level.setBlockAndUpdate(pos, concreteState);
                return;
            }
        }

        // G. Dry sponge to wet sponge:
        if (state.is(Blocks.SPONGE)) {
            level.setBlockAndUpdate(pos, Blocks.WET_SPONGE.defaultBlockState());
            return;
        }

        if (level instanceof ServerLevel serverLevel && state.getBlock() instanceof com.onetoys.salt.block.ISaltBlock) {
            if (com.onetoys.salt.Dissolving.maybeDissolveFromWatering(state, serverLevel, pos)) {
                return;
            }
        }

        // H. Call native block precipitation handler (e.g. cauldrons):
        state.getBlock().handlePrecipitation(state, level, pos, Biome.Precipitation.RAIN);

        // I. Extinguish burning entities and apply wetness to living entities standing on or in the watered block
        if (!level.isClientSide()) {
            AABB box = new AABB(pos).inflate(0.2);
            List<LivingEntity> livingEntities = level.getEntitiesOfClass(LivingEntity.class, box,
                    living -> living.isOnFire() || living.isSensitiveToWater() || WetnessTweaks.isTweakEnabled());
            for (LivingEntity living : livingEntities) {
                if (living.isOnFire()) {
                    living.clearFire();
                }
                if (living.isSensitiveToWater()) {
                    living.hurt(living.damageSources().drown(), 1.0F);
                }
                WetnessTweaks.setWet(living, WetnessTweaks.getWetDurationTicks());
            }
        }

        // J. Natural growth / random tick simulation (exclude FarmBlock so farmland never dries out or turns to dirt):
        if (level instanceof ServerLevel serverLevel) {
            boolean shouldGrow = player != null;
            if (shouldGrow) {
                if (!(state.getBlock() instanceof FarmBlock) && state.isRandomlyTicking()) {
                    state.randomTick(serverLevel, pos, serverLevel.random);
                }
                if (state.getBlock() instanceof FarmBlock) {
                    BlockState cropState = level.getBlockState(abovePos);
                    if (cropState.isRandomlyTicking()) {
                        cropState.randomTick(serverLevel, abovePos, serverLevel.random);
                    }
                }
            }
        }
    }

    public static void playWateringEffects(Level level, BlockPos pos, Direction clickedFace, Vec3 hitVec) {
        float pitchDrip = 1.0F + level.random.nextFloat() * 0.3F;
        float pitchSplash = 0.9F + level.random.nextFloat() * 0.2F;
        level.playSound(null, pos, SoundEvents.POINTED_DRIPSTONE_DRIP_WATER, SoundSource.BLOCKS, 0.4F, pitchDrip);
        level.playSound(null, pos, com.onetoys.registry.OnetoysSoundEvents.TINY_SPLASH.get(), SoundSource.BLOCKS, 0.5F, pitchSplash);

        if (level instanceof ServerLevel serverLevel) {
            double x = hitVec.x;
            double y = hitVec.y + (clickedFace == Direction.DOWN ? -0.05 : 0.05);
            double z = hitVec.z;
            serverLevel.sendParticles(ParticleTypes.RAIN, x, y + 0.08, z, 6, 0.15, 0.03, 0.15, 0.0);
            serverLevel.sendParticles(ParticleTypes.SPLASH, x, y, z, 5, 0.15, 0.05, 0.15, 0.05);
            if (serverLevel.random.nextFloat() < 0.25F) {
                serverLevel.sendParticles(ParticleTypes.BUBBLE_POP, x, y + 0.05, z, 1, 0.05, 0.05, 0.05, 0.01);
            }
        }
    }
}
