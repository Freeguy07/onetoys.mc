package com.onetoys.mixin;

import com.onetoys.Onetoys;
import com.onetoys.module.content.ContentModule;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.HoneycombItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.WeatheringCopper;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.DataMapHooks;
import net.neoforged.neoforge.common.ItemAbilities;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AxeItem.class)
public abstract class AxeItemMixin {

    @Inject(
            method = "useOn",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/Level;setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Z"
            )
    )
    private void Onetoys$dropAxeByproducts(UseOnContext context, CallbackInfoReturnable<InteractionResult> cir) {
        if (!ContentModule.isContentEnabled()) {
            return;
        }

        Level level = context.getLevel();
        if (level.isClientSide) {
            return;
        }

        BlockPos pos = context.getClickedPos();
        BlockState oldState = level.getBlockState(pos);

        ItemStack drop = null;

        // 1. Stripping logs drops Bark
        BlockState stripped = oldState.getToolModifiedState(context, ItemAbilities.AXE_STRIP, true);
        if (stripped != null) {
            drop = new ItemStack(Onetoys.BARK.get());
        } else {
            // 2. Scraping oxidized copper drops Copper Patina
            BlockState scraped = oldState.getToolModifiedState(context, ItemAbilities.AXE_SCRAPE, true);
            if (scraped == null) {
                scraped = WeatheringCopper.getPrevious(oldState).orElse(null);
            }
            if (scraped != null) {
                drop = new ItemStack(Onetoys.COPPER_PATINA.get());
            } else {
                // 3. Removing wax drops Honeycomb
                BlockState waxedOff = oldState.getToolModifiedState(context, ItemAbilities.AXE_WAX_OFF, true);
                if (waxedOff == null) {
                    Block waxOffBlock = DataMapHooks.getBlockUnwaxed(oldState.getBlock());
                    if (waxOffBlock == null) {
                        waxOffBlock = HoneycombItem.WAX_OFF_BY_BLOCK.get().get(oldState.getBlock());
                    }
                    if (waxOffBlock != null) {
                        waxedOff = waxOffBlock.withPropertiesOf(oldState);
                    }
                }
                if (waxedOff != null) {
                    drop = new ItemStack(Items.HONEYCOMB);
                }
            }
        }

        if (drop != null) {
            Block.popResourceFromFace(level, pos, context.getClickedFace(), drop);
        }
    }
}
