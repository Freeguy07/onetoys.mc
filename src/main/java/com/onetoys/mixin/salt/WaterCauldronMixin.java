package com.onetoys.mixin.salt;

import com.onetoys.module.content.ContentModule;
import com.onetoys.salt.Heater;
import com.onetoys.salt.SaltConfiguration;
import com.onetoys.salt.SaltSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.AbstractCauldronBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.PointedDripstoneBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractCauldronBlock.class)
public abstract class WaterCauldronMixin extends Block {

    public WaterCauldronMixin(Properties pProperties) {
        super(pProperties);
    }

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random, CallbackInfo ci) {
        if (ContentModule.isSaltEnabled()
                && state.is(Blocks.CAULDRON)
                && SaltConfiguration.isLoaded()
                && SaltConfiguration.EVAPORATION_ENABLED.get()
                && Heater.isHeatSource(level.getBlockState(pos.below()))) {
            BlockPos stalactitePos = PointedDripstoneBlock.findStalactiteTipAboveCauldron(level, pos);
            if (stalactitePos == null)
                return;

            Fluid fluid = PointedDripstoneBlock.getCauldronFillFluidType(level, stalactitePos);
            if (fluid != Fluids.LAVA) {
                Vec3 center = Vec3.atCenterOf(pos);
                level.playSound(null, center.x, center.y, center.z, SaltSounds.CAULDRON_EVAPORATE.get(),
                        SoundSource.BLOCKS, 0.6f, level.getRandom().nextFloat() * 0.2f + 1f);
                level.sendParticles(ParticleTypes.CLOUD, center.x, center.y + 0.2, center.z, 1, 0.0, 0.02, 0.0, 0.0);
                ci.cancel();
            }
        }
    }
}
