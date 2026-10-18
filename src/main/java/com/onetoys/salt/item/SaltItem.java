package com.onetoys.salt.item;

import com.onetoys.module.content.ContentModule;
import com.onetoys.salt.Melting;
import com.onetoys.salt.SaltConfiguration;
import com.onetoys.salt.SaltTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

public class SaltItem extends Item {
    public SaltItem(Properties properties) {
        super(properties);
    }

    @Override
    public @NotNull InteractionResult useOn(UseOnContext context) {
        if (!ContentModule.isSaltEnabled()) {
            return super.useOn(context);
        }

        Level level = context.getLevel();
        BlockPos clickedPos = context.getClickedPos();
        BlockState clickedBlockState = level.getBlockState(clickedPos);

        if (clickedBlockState.is(SaltTags.Blocks.MELTABLES)
                && (SaltConfiguration.isLoaded() ? SaltConfiguration.MELTING_ITEM_ENABLED.get() : true)) {
            if (level instanceof ServerLevel serverLevel) {
                Melting.meltBlock(clickedPos, serverLevel);
                level.gameEvent(GameEvent.BLOCK_CHANGE, clickedPos, GameEvent.Context.of(context.getPlayer(), level.getBlockState(clickedPos)));
                context.getItemInHand().consume(1, context.getPlayer());
            } else {
                Vec3 center = Vec3.atCenterOf(clickedPos);
                RandomSource random = level.getRandom();
                for (int i = 0; i < 6; i++) {
                    level.addParticle(ParticleTypes.SPIT,
                            center.x + random.nextGaussian() * 0.35f,
                            center.y + 0.35f + random.nextGaussian() * 0.35f,
                            center.z + random.nextGaussian() * 0.35f,
                            0f, 0f, 0f);
                }
            }

            return InteractionResult.sidedSuccess(context.getLevel().isClientSide);
        }

        return super.useOn(context);
    }
}
