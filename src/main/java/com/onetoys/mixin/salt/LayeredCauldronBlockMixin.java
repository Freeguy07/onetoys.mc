package com.onetoys.mixin.salt;

import com.onetoys.module.content.ContentModule;
import com.onetoys.salt.Evaporation;
import com.onetoys.salt.Heater;
import com.onetoys.salt.SaltConfiguration;
import net.minecraft.core.BlockPos;
import net.minecraft.core.cauldron.CauldronInteraction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AbstractCauldronBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LayeredCauldronBlock.class)
public abstract class LayeredCauldronBlockMixin extends AbstractCauldronBlock {
    public LayeredCauldronBlockMixin(Properties properties, CauldronInteraction.InteractionMap interactions) {
        super(properties, interactions);
    }

    @Override
    protected boolean isRandomlyTicking(BlockState state) {
        if (state.is(Blocks.WATER_CAULDRON) && ContentModule.isSaltEnabled() && (!SaltConfiguration.isLoaded() || SaltConfiguration.EVAPORATION_ENABLED.get())) {
            return true;
        }
        return super.isRandomlyTicking(state);
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel serverLevel, BlockPos pos, RandomSource random) {
        if (state.is(Blocks.WATER_CAULDRON)) {
            if (ContentModule.isSaltEnabled() && SaltConfiguration.isLoaded() && SaltConfiguration.EVAPORATION_ENABLED.get()) {
                if (state.getValue(LayeredCauldronBlock.LEVEL) > 0
                        && Heater.isHeatSource(serverLevel.getBlockState(pos.below()))
                        && random.nextDouble() < SaltConfiguration.EVAPORATION_CHANCE.get()) {
                    Evaporation.evaporateWaterAndFormSalt(state, serverLevel, pos, random);
                }
            }
            return;
        }
        super.randomTick(state, serverLevel, pos, random);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (ContentModule.isSaltEnabled()
                && state.is(Blocks.WATER_CAULDRON)
                && SaltConfiguration.isLoaded()
                && SaltConfiguration.EVAPORATION_ENABLED.get()
                && Heater.isHeatSource(level.getBlockState(pos.below()))) {
            Evaporation.onWaterCauldronAnimateTick(state, level, pos, random);
        }
        super.animateTick(state, level, pos, random);
    }

    @Inject(method = "entityInside", at = @At("HEAD"))
    private void onEntityInside(BlockState state, Level level, BlockPos pos, Entity entity, CallbackInfo ci) {
        if (ContentModule.isSaltEnabled()
                && state.is(Blocks.WATER_CAULDRON)
                && SaltConfiguration.isLoaded()
                && SaltConfiguration.EVAPORATION_ENABLED.get()
                && Heater.isHeatSource(level.getBlockState(pos.below()))) {
            if (!entity.isSteppingCarefully() && entity instanceof LivingEntity) {
                entity.hurt(level.damageSources().hotFloor(), 1f);
            }
        }
    }
}
