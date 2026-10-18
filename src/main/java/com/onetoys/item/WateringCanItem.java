package com.onetoys.item;

import java.util.List;

import javax.annotation.Nullable;

import com.onetoys.module.content.ContentModule;
import com.onetoys.module.content.WateringHelper;
import com.onetoys.module.gameplay.wetness.WetnessTweaks;
import com.onetoys.registry.OnetoysDataComponents;
import com.onetoys.registry.OnetoysItems;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.CandleBlock;
import net.minecraft.world.level.block.CandleCakeBlock;
import net.minecraft.world.level.block.ConcretePowderBlock;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class WateringCanItem extends Item {
    public static final int MAX_USES = 64;
    public static final int WATER_BAR_COLOR = 0x3F76E4;

    public WateringCanItem(Item.Properties properties) {
        super(properties);
    }

    public static int getWaterLevel(ItemStack stack) {
        return stack.getOrDefault(OnetoysDataComponents.WATERING_CAN_USES.get(), 0);
    }

    public static void setWaterLevel(ItemStack stack, int level) {
        stack.set(OnetoysDataComponents.WATERING_CAN_USES.get(), Math.clamp(level, 0, MAX_USES));
    }

    public static ItemStack createFilledWateringCan() {
        ItemStack stack = new ItemStack(OnetoysItems.WATERING_CAN.get());
        setWaterLevel(stack, MAX_USES);
        return stack;
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return getWaterLevel(stack) > 0;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return Math.round(13.0F * getWaterLevel(stack) / (float) MAX_USES);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return WATER_BAR_COLOR;
    }

    public static InteractionResult waterSelf(ItemStack stack, Player player, Level level, InteractionHand hand) {
        if (!ContentModule.isWateringCanEnabled()) {
            return InteractionResult.PASS;
        }

        int water = getWaterLevel(stack);
        if (water <= 0) {
            return InteractionResult.PASS;
        }

        if (player.getCooldowns().isOnCooldown(stack.getItem())) {
            return InteractionResult.PASS;
        }

        boolean wasOnFire = player.isOnFire();
        if (!level.isClientSide()) {
            if (!player.getAbilities().instabuild) {
                setWaterLevel(stack, water - 1);
            }

            if (wasOnFire) {
                player.clearFire();
            }

            WetnessTweaks.setWet(player, WetnessTweaks.getWetDurationTicks());

            if (level instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(
                        ParticleTypes.SPLASH,
                        player.getX(),
                        player.getY() + player.getBbHeight() * 0.5,
                        player.getZ(),
                        12,
                        player.getBbWidth() * 0.3,
                        player.getBbHeight() * 0.3,
                        player.getBbWidth() * 0.3,
                        0.05
                );
                serverLevel.sendParticles(
                        ParticleTypes.RAIN,
                        player.getX(),
                        player.getY() + player.getBbHeight() * 0.8,
                        player.getZ(),
                        8,
                        player.getBbWidth() * 0.3,
                        0.1,
                        player.getBbWidth() * 0.3,
                        0.0
                );
            }

            if (wasOnFire) {
                level.playSound(null, player.blockPosition(), SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 0.5F, 2.6F);
            } else {
                float pitchDrip = 1.0F + level.random.nextFloat() * 0.3F;
                float pitchSplash = 1.4F + level.random.nextFloat() * 0.3F;
                level.playSound(null, player.blockPosition(), SoundEvents.POINTED_DRIPSTONE_DRIP_WATER, SoundSource.PLAYERS, 0.6F, pitchDrip);
                level.playSound(null, player.blockPosition(), SoundEvents.GENERIC_SPLASH, SoundSource.PLAYERS, 0.25F, pitchSplash);
            }
        }

        player.getCooldowns().addCooldown(stack.getItem(), 10);
        player.swing(hand);
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!ContentModule.isWateringCanEnabled()) {
            return InteractionResultHolder.pass(stack);
        }

        if (player.isSecondaryUseActive() && getWaterLevel(stack) > 0) {
            InteractionResult result = waterSelf(stack, player, level, hand);
            return new InteractionResultHolder<>(result, stack);
        }

        BlockHitResult hitResult = getPlayerPOVHitResult(level, player, ClipContext.Fluid.SOURCE_ONLY);
        if (hitResult.getType() == HitResult.Type.BLOCK) {
            BlockPos pos = hitResult.getBlockPos();
            FluidState fluid = level.getFluidState(pos);
            if (fluid.is(Fluids.WATER) && fluid.isSource()) {
                if (getWaterLevel(stack) < MAX_USES) {
                    if (!level.isClientSide()) {
                        setWaterLevel(stack, MAX_USES);
                    }
                    level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BUCKET_FILL, SoundSource.PLAYERS, 1.0F, 1.0F);
                    player.swing(hand);
                    return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
                }
            }
        }
        return InteractionResultHolder.pass(stack);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (!ContentModule.isWateringCanEnabled()) {
            return InteractionResult.PASS;
        }

        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState state = level.getBlockState(pos);
        Player player = context.getPlayer();
        ItemStack stack = context.getItemInHand();
        int water = getWaterLevel(stack);

        // Sneak + Right Click waters self:
        if (player != null && player.isSecondaryUseActive() && water > 0) {
            return waterSelf(stack, player, level, context.getHand());
        }

        // 1. Refilling from Water Cauldron:
        if (state.is(Blocks.WATER_CAULDRON)) {
            if (water < MAX_USES) {
                if (!level.isClientSide()) {
                    LayeredCauldronBlock.lowerFillLevel(state, level, pos);
                    setWaterLevel(stack, MAX_USES);
                }
                level.playSound(null, pos, SoundEvents.BUCKET_FILL, SoundSource.BLOCKS, 1.0F, 1.0F);
                if (player != null) {
                    player.swing(context.getHand());
                }
                return InteractionResult.sidedSuccess(level.isClientSide());
            } else {
                return InteractionResult.PASS;
            }
        }

        // 2. Refilling from Water Source / Waterlogged Block:
        if (water < MAX_USES) {
            FluidState clickedFluid = level.getFluidState(pos);
            FluidState adjacentFluid = level.getFluidState(pos.relative(context.getClickedFace()));
            if ((clickedFluid.is(Fluids.WATER) && clickedFluid.isSource())
                    || (adjacentFluid.is(Fluids.WATER) && adjacentFluid.isSource())) {
                if (!level.isClientSide()) {
                    setWaterLevel(stack, MAX_USES);
                }
                level.playSound(null, pos, SoundEvents.BUCKET_FILL, SoundSource.PLAYERS, 1.0F, 1.0F);
                if (player != null) {
                    player.swing(context.getHand());
                }
                return InteractionResult.sidedSuccess(level.isClientSide());
            }
        }

        // 3. Cannot water if empty (return PASS so fluid raycasting in use() can still catch water)
        if (water <= 0) {
            return InteractionResult.PASS;
        }

        // 4. Simulate rain on the targeted block:
        WateringHelper.applyRainEffects(level, pos, state, player);
        if (!level.isClientSide()) {
            if (player == null || !player.getAbilities().instabuild) {
                setWaterLevel(stack, water - 1);
            }
        }
        WateringHelper.playWateringEffects(level, pos, context.getClickedFace(), context.getClickLocation());
        if (player != null) {
            player.swing(context.getHand());
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        if (!ContentModule.isWateringCanEnabled()) {
            return InteractionResult.PASS;
        }

        if (player.isSecondaryUseActive() && getWaterLevel(stack) > 0) {
            return waterSelf(stack, player, player.level(), hand);
        }

        int water = getWaterLevel(stack);
        if (water <= 0) {
            return InteractionResult.PASS;
        }

        Level level = target.level();
        boolean wasOnFire = target.isOnFire();
        if (!level.isClientSide()) {
            if (!player.getAbilities().instabuild) {
                setWaterLevel(stack, water - 1);
            }

            if (wasOnFire) {
                target.clearFire();
            }

            if (target.isSensitiveToWater()) {
                target.hurt(target.damageSources().drown(), 1.0F);
            }

            WetnessTweaks.setWet(target, WetnessTweaks.getWetDurationTicks());

            if (level instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(
                        ParticleTypes.SPLASH,
                        target.getX(),
                        target.getY() + target.getBbHeight() * 0.5,
                        target.getZ(),
                        12,
                        target.getBbWidth() * 0.3,
                        target.getBbHeight() * 0.3,
                        target.getBbWidth() * 0.3,
                        0.05
                );
                serverLevel.sendParticles(
                        ParticleTypes.RAIN,
                        target.getX(),
                        target.getY() + target.getBbHeight() * 0.8,
                        target.getZ(),
                        8,
                        target.getBbWidth() * 0.3,
                        0.1,
                        target.getBbWidth() * 0.3,
                        0.0
                );
            }

            if (wasOnFire) {
                level.playSound(null, target.blockPosition(), SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 0.5F, 2.6F);
            } else {
                float pitchDrip = 1.0F + level.random.nextFloat() * 0.3F;
                float pitchSplash = 1.4F + level.random.nextFloat() * 0.3F;
                level.playSound(null, target.blockPosition(), SoundEvents.POINTED_DRIPSTONE_DRIP_WATER, SoundSource.PLAYERS, 0.6F, pitchDrip);
                level.playSound(null, target.blockPosition(), SoundEvents.GENERIC_SPLASH, SoundSource.PLAYERS, 0.25F, pitchSplash);
            }
        }

        player.swing(hand);
        return InteractionResult.sidedSuccess(level.isClientSide());
    }
}
