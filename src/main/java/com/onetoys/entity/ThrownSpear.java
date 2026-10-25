package com.onetoys.entity;

import javax.annotation.Nullable;

import com.onetoys.registry.OnetoysEntityTypes;
import com.onetoys.registry.OnetoysItems;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

public class ThrownSpear extends AbstractArrow {
    private static final EntityDataAccessor<ItemStack> ID_ITEM =
            SynchedEntityData.defineId(ThrownSpear.class, EntityDataSerializers.ITEM_STACK);

    private boolean dealtDamage;

    public ThrownSpear(EntityType<? extends ThrownSpear> entityType, Level level) {
        super(entityType, level);
        this.setBaseDamage(6.0D);
    }

    public ThrownSpear(Level level, LivingEntity shooter, ItemStack pickupItemStack) {
        super(OnetoysEntityTypes.SPEAR.get(), shooter, level, pickupItemStack, pickupItemStack);
        this.setBaseDamage(6.0D);
        this.entityData.set(ID_ITEM, pickupItemStack.copyWithCount(1));
    }

    public ThrownSpear(Level level, double x, double y, double z, ItemStack pickupItemStack) {
        super(OnetoysEntityTypes.SPEAR.get(), x, y, z, level, pickupItemStack, pickupItemStack);
        this.setBaseDamage(6.0D);
        this.entityData.set(ID_ITEM, pickupItemStack.copyWithCount(1));
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(ID_ITEM, this.getDefaultPickupItem());
    }

    @Override
    public void tick() {
        if (this.inGroundTime > 4) {
            this.dealtDamage = true;
        }
        super.tick();
    }

    public ItemStack getItem() {
        ItemStack stack = this.entityData.get(ID_ITEM);
        return stack.isEmpty() ? this.getDefaultPickupItem() : stack;
    }

    public void setItem(ItemStack stack) {
        ItemStack copy = stack.copyWithCount(1);
        this.entityData.set(ID_ITEM, copy);
        this.setPickupItemStack(copy);
    }

    @Override
    public ItemStack getWeaponItem() {
        return this.getItem();
    }

    @Override
    protected ItemStack getDefaultPickupItem() {
        return new ItemStack(OnetoysItems.SPEAR.get());
    }

    @Override
    protected ItemStack getPickupItem() {
        return this.getItem().copy();
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
    protected @Nullable EntityHitResult findHitEntity(Vec3 startVec, Vec3 endVec) {
        return this.dealtDamage ? null : super.findHitEntity(startVec, endVec);
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
            if (this.level() instanceof ServerLevel serverLevel1) {
                EnchantmentHelper.doPostAttackEffectsWithItemSource(serverLevel1, target, damageSource, this.getWeaponItem());
            }
            if (target instanceof LivingEntity livingTarget) {
                this.doKnockback(livingTarget, damageSource);
                this.doPostHurtEffects(livingTarget);
            }
        }
        this.setDeltaMovement(this.getDeltaMovement().multiply(-0.01D, -0.1D, -0.01D));
        this.playSound(SoundEvents.TRIDENT_HIT, 1.0F, 1.0F);
    }

    @Override
    protected void doKnockback(LivingEntity entity, DamageSource damageSource) {
        double d0 = (double) (
                this.level() instanceof ServerLevel serverLevel
                        ? EnchantmentHelper.modifyKnockback(serverLevel, this.getWeaponItem(), entity, damageSource, 0.0F)
                        : 0.0F
        );
        if (d0 > 0.0D) {
            double d1 = Math.max(0.0D, 1.0D - entity.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE));
            Vec3 vec3 = this.getDeltaMovement().multiply(1.0D, 0.0D, 1.0D).normalize().scale(d0 * 0.6D * d1);
            if (vec3.lengthSqr() > 0.0D) {
                entity.push(vec3.x, 0.1D, vec3.z);
            }
        }
    }

    @Override
    protected void hitBlockEnchantmentEffects(ServerLevel level, BlockHitResult hitResult, ItemStack stack) {
        Vec3 vec3 = hitResult.getBlockPos().clampLocationWithin(hitResult.getLocation());
        EnchantmentHelper.onHitBlock(
                level,
                stack,
                this.getOwner() instanceof LivingEntity livingEntity ? livingEntity : null,
                this,
                null,
                vec3,
                level.getBlockState(hitResult.getBlockPos()),
                p_348680_ -> this.kill()
        );
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
        } else if (tag.contains("item", 10)) {
            this.setItem(ItemStack.parseOptional(this.registryAccess(), tag.getCompound("item")));
        }
    }
}
