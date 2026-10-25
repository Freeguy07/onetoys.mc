package com.onetoys.entity;

import com.onetoys.registry.OnetoysEntityTypes;
import com.onetoys.registry.OnetoysItems;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;

public class ThrownSpear extends AbstractArrow {
    private static final EntityDataAccessor<ItemStack> ID_ITEM =
            SynchedEntityData.defineId(ThrownSpear.class, EntityDataSerializers.ITEM_STACK);
    private static final EntityDataAccessor<Boolean> ID_FOIL =
            SynchedEntityData.defineId(ThrownSpear.class, EntityDataSerializers.BOOLEAN);

    private boolean dealtDamage;

    public ThrownSpear(EntityType<? extends ThrownSpear> entityType, Level level) {
        super(entityType, level);
        this.setBaseDamage(6.0D);
    }

    public ThrownSpear(Level level, LivingEntity shooter, ItemStack pickupItemStack) {
        super(OnetoysEntityTypes.SPEAR.get(), shooter, level, pickupItemStack, null);
        this.setBaseDamage(6.0D);
        this.entityData.set(ID_ITEM, pickupItemStack.copyWithCount(1));
        this.entityData.set(ID_FOIL, pickupItemStack.hasFoil());
    }

    public ThrownSpear(Level level, double x, double y, double z, ItemStack pickupItemStack) {
        super(OnetoysEntityTypes.SPEAR.get(), x, y, z, level, pickupItemStack, null);
        this.setBaseDamage(6.0D);
        this.entityData.set(ID_ITEM, pickupItemStack.copyWithCount(1));
        this.entityData.set(ID_FOIL, pickupItemStack.hasFoil());
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(ID_ITEM, this.getDefaultPickupItem());
        builder.define(ID_FOIL, false);
    }

    public ItemStack getItem() {
        ItemStack stack = this.entityData.get(ID_ITEM);
        return stack.isEmpty() ? this.getDefaultPickupItem() : stack;
    }

    public void setItem(ItemStack stack) {
        this.entityData.set(ID_ITEM, stack.copyWithCount(1));
        this.entityData.set(ID_FOIL, stack.hasFoil());
    }

    public boolean isFoil() {
        return this.entityData.get(ID_FOIL);
    }

    @Override
    protected ItemStack getDefaultPickupItem() {
        return new ItemStack(OnetoysItems.SPEAR.get());
    }

    @Override
    protected SoundEvent getDefaultHitGroundSoundEvent() {
        return SoundEvents.TRIDENT_HIT_GROUND;
    }

    @Override
    protected float getWaterInertia() {
        return 0.99F;
    }

    @Override
    public boolean shouldRender(double x, double y, double z) {
        return true;
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        Entity target = result.getEntity();
        float damage = 6.0F;
        Entity owner = this.getOwner();
        DamageSource damageSource = this.damageSources().trident(this, owner == null ? this : owner);
        if (this.level() instanceof ServerLevel serverLevel) {
            damage = EnchantmentHelper.modifyDamage(serverLevel, this.getItem(), target, damageSource, damage);
        }
        this.dealtDamage = true;
        if (target.hurt(damageSource, damage)) {
            if (target.getType() == EntityType.ENDERMAN) {
                return;
            }
            if (target instanceof LivingEntity livingTarget) {
                this.doKnockback(livingTarget, damageSource);
                this.doPostHurtEffects(livingTarget);
                if (this.level() instanceof ServerLevel serverLevel) {
                    ItemStack item = this.getItem();
                    if (owner instanceof LivingEntity livingOwner) {
                        item.hurtAndBreak(1, serverLevel, livingOwner instanceof ServerPlayer sp ? sp : null, (i) -> {});
                        this.setItem(item);
                    }
                }
            }
        }
        this.setDeltaMovement(this.getDeltaMovement().multiply(-0.01D, -0.1D, -0.01D));
        this.playSound(SoundEvents.TRIDENT_HIT, 1.0F, 1.0F);
    }

    @Override
    protected boolean tryPickup(Player player) {
        if (this.pickup != AbstractArrow.Pickup.ALLOWED && (this.pickup != AbstractArrow.Pickup.CREATIVE_ONLY || !player.hasInfiniteMaterials())) {
            return false;
        }
        return super.tryPickup(player);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("DealtDamage", this.dealtDamage);
        tag.put("Item", this.getItem().save(this.registryAccess()));
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.dealtDamage = tag.getBoolean("DealtDamage");
        if (tag.contains("Item", 10)) {
            this.setItem(ItemStack.parseOptional(this.registryAccess(), tag.getCompound("Item")));
        }
    }
}
