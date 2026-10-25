package com.onetoys.block.entity;

import com.onetoys.registry.OnetoysBlockEntityTypes;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class QuartzGlassBlockEntity extends BlockEntity {
    public static final int PLACEMENT_FADE_TICKS = 40;
    private long placedGameTime = -1L;

    public QuartzGlassBlockEntity(BlockPos pos, BlockState blockState) {
        super(OnetoysBlockEntityTypes.QUARTZ_GLASS.get(), pos, blockState);
    }

    public void setPlacedGameTime(long placedGameTime) {
        this.placedGameTime = placedGameTime;
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    public long getPlacedGameTime() {
        return placedGameTime;
    }

    public boolean isPlacementRevealed() {
        return isPlacementRevealed(this.level);
    }

    public boolean isPlacementRevealed(Level currentLevel) {
        if (placedGameTime <= 0L || currentLevel == null) {
            return false;
        }
        long diff = currentLevel.getGameTime() - placedGameTime;
        return diff >= 0 && diff < PLACEMENT_FADE_TICKS;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putLong("PlacedGameTime", placedGameTime);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("PlacedGameTime")) {
            this.placedGameTime = tag.getLong("PlacedGameTime");
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        tag.putLong("PlacedGameTime", placedGameTime);
        return tag;
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
