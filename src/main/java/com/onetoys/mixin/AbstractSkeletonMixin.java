package com.onetoys.mixin;

import com.onetoys.entity.ThrownSpear;
import com.onetoys.module.gameplay.mobs.SpecialMobTweaks;
import com.onetoys.module.gameplay.mobs.goal.SkeletonSpearAttackGoal;
import com.onetoys.module.gameplay.mobs.goal.SmartSkeletonSpearMeleeGoal;
import com.onetoys.registry.OnetoysAttachments;
import com.onetoys.registry.OnetoysItems;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RangedBowAttackGoal;
import net.minecraft.world.entity.ai.goal.RangedCrossbowAttackGoal;
import net.minecraft.world.entity.monster.AbstractSkeleton;
import net.minecraft.world.entity.monster.CrossbowAttackMob;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ProjectileWeaponItem;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractSkeleton.class)
public abstract class AbstractSkeletonMixin extends Monster implements RangedAttackMob, CrossbowAttackMob {

    @Shadow
    @Final
    private RangedBowAttackGoal<AbstractSkeleton> bowGoal;

    @Shadow
    @Final
    private MeleeAttackGoal meleeGoal;

    @Unique
    private RangedCrossbowAttackGoal<?> onetoys$crossbowGoal;

    @Unique
    private SkeletonSpearAttackGoal onetoys$spearGoal;

    @Unique
    private SmartSkeletonSpearMeleeGoal onetoys$spearMeleeGoal;

    protected AbstractSkeletonMixin(EntityType<? extends Monster> entityType, Level level) {
        super(entityType, level);
    }

    @Override
    public void setChargingCrossbow(boolean isCharging) {
        this.setData(OnetoysAttachments.SKELETON_CHARGING_CROSSBOW, isCharging);
    }

    @Override
    public void onCrossbowAttackPerformed() {
        this.noActionTime = 0;
    }

    @Inject(method = "reassessWeaponGoal", at = @At("HEAD"), cancellable = true)
    private void Onetoys$handleCustomWeaponGoals(CallbackInfo ci) {
        AbstractSkeleton skeleton = (AbstractSkeleton) (Object) this;
        if (skeleton.level() == null || skeleton.level().isClientSide) {
            return;
        }

        if (this.onetoys$crossbowGoal != null) {
            this.goalSelector.removeGoal(this.onetoys$crossbowGoal);
        }
        if (this.onetoys$spearGoal != null) {
            this.goalSelector.removeGoal(this.onetoys$spearGoal);
        }
        if (this.onetoys$spearMeleeGoal != null) {
            this.goalSelector.removeGoal(this.onetoys$spearMeleeGoal);
        }

        if (SpecialMobTweaks.isWarlockWitherSkeletonEnabled()
                && SpecialMobTweaks.isSpecialMob(skeleton, SpecialMobTweaks.TYPE_WARLOCK_WITHER_SKELETON)) {
            this.goalSelector.removeGoal(this.meleeGoal);
            this.goalSelector.removeGoal(this.bowGoal);
            ci.cancel();
            return;
        }

        if (skeleton.getMainHandItem().is(OnetoysItems.SPEAR.get())) {
            this.goalSelector.removeGoal(this.meleeGoal);
            this.goalSelector.removeGoal(this.bowGoal);
            if (this.onetoys$spearGoal == null) {
                this.onetoys$spearGoal = new SkeletonSpearAttackGoal(skeleton, 1.0D, 40, 10.0F);
            }
            this.goalSelector.addGoal(4, this.onetoys$spearGoal);
            if (this.onetoys$spearMeleeGoal == null) {
                this.onetoys$spearMeleeGoal = new SmartSkeletonSpearMeleeGoal(skeleton, 1.2D, false);
            }
            this.goalSelector.addGoal(4, this.onetoys$spearMeleeGoal);
            ci.cancel();
            return;
        }

        InteractionHand crossbowHand = ProjectileUtil.getWeaponHoldingHand(skeleton, item -> item instanceof CrossbowItem);
        ItemStack weaponStack = skeleton.getItemInHand(crossbowHand);
        if (weaponStack.getItem() instanceof CrossbowItem) {
            this.goalSelector.removeGoal(this.meleeGoal);
            this.goalSelector.removeGoal(this.bowGoal);
            if (this.onetoys$crossbowGoal == null) {
                this.onetoys$crossbowGoal = onetoys$createCrossbowGoal(skeleton);
            }
            this.goalSelector.addGoal(4, this.onetoys$crossbowGoal);
            ci.cancel();
        }
    }

    @Unique
    @SuppressWarnings("unchecked")
    private static <T extends Monster & RangedAttackMob & CrossbowAttackMob> RangedCrossbowAttackGoal<T> onetoys$createCrossbowGoal(AbstractSkeleton skeleton) {
        return new RangedCrossbowAttackGoal<>((T) skeleton, 1.0D, 8.0F);
    }

    @Inject(method = "canFireProjectileWeapon", at = @At("HEAD"), cancellable = true)
    private void Onetoys$allowCrossbowWeapon(ProjectileWeaponItem projectileWeapon, CallbackInfoReturnable<Boolean> cir) {
        if (projectileWeapon instanceof CrossbowItem) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "performRangedAttack", at = @At("HEAD"), cancellable = true)
    private void Onetoys$performCustomRangedAttacks(LivingEntity target, float velocity, CallbackInfo ci) {
        AbstractSkeleton skeleton = (AbstractSkeleton) (Object) this;
        InteractionHand crossbowHand = ProjectileUtil.getWeaponHoldingHand(skeleton, item -> item instanceof CrossbowItem);
        if (skeleton.getItemInHand(crossbowHand).getItem() instanceof CrossbowItem) {
            this.performCrossbowAttack(skeleton, 1.6F);
            ci.cancel();
            return;
        }

        if (skeleton.getMainHandItem().is(OnetoysItems.SPEAR.get())) {
            onetoys$performSpearAttack(skeleton, target);
            ci.cancel();
        }
    }

    @Unique
    private static void onetoys$performSpearAttack(AbstractSkeleton skeleton, LivingEntity target) {
        ThrownSpear thrownSpear = new ThrownSpear(skeleton.level(), skeleton, new ItemStack(OnetoysItems.SPEAR.get()));
        thrownSpear.pickup = AbstractArrow.Pickup.DISALLOWED;
        double d0 = target.getX() - skeleton.getX();
        double d1 = target.getY(0.3333333333333333D) - thrownSpear.getY();
        double d2 = target.getZ() - skeleton.getZ();
        double d3 = Math.sqrt(d0 * d0 + d2 * d2);
        thrownSpear.shoot(d0, d1 + d3 * 0.20000000298023224D, d2, 1.6F, (float) (14 - skeleton.level().getDifficulty().getId() * 4));
        skeleton.playSound(SoundEvents.TRIDENT_THROW.value(), 1.0F, 1.0F / (skeleton.getRandom().nextFloat() * 0.4F + 0.8F));
        skeleton.level().addFreshEntity(thrownSpear);
    }
}
