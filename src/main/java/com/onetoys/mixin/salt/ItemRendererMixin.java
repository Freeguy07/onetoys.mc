package com.onetoys.mixin.salt;

import com.onetoys.salt.SaltConfiguration;
import com.onetoys.salt.Salting;
import com.onetoys.salt.Satiating;
import com.onetoys.salt.Sweetening;
import com.onetoys.salt.client.LayeredBakedModel;
import com.onetoys.salt.client.SaltClientEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;

@Mixin(ItemRenderer.class)
public class ItemRendererMixin {
    @Inject(method = "getModel", at = @At("RETURN"), cancellable = true)
    private void getModel(ItemStack stack, Level level, LivingEntity entity, int idk, CallbackInfoReturnable<BakedModel> cir) {
        if (stack == null || stack.isEmpty()) return;

        // Fast-path: Check seasoning components before processing.
        boolean isSalted = Salting.isSalted(stack);
        boolean isSweetened = Sweetening.isSweetened(stack);
        boolean isSatiating = Satiating.isSatiating(stack);

        if (!isSalted && !isSweetened && !isSatiating) {
            return;
        }

        if (SaltConfiguration.isLoaded()) {
            isSalted = isSalted && SaltConfiguration.SALTED_OVERLAY_ENABLED.get();
            isSweetened = isSweetened && SaltConfiguration.SWEETENED_OVERLAY_ENABLED.get();
            isSatiating = isSatiating && SaltConfiguration.SATIATING_OVERLAY_ENABLED.get();

            if (isSalted || isSweetened || isSatiating) {
                BakedModel originalModel = cir.getReturnValue();
                if (originalModel != null) {
                    int flags = (isSalted ? 1 : 0) | (isSweetened ? 2 : 0) | (isSatiating ? 4 : 0);
                    LayeredBakedModel.CacheKey cacheKey = new LayeredBakedModel.CacheKey(originalModel, flags);
                    BakedModel cachedModel = LayeredBakedModel.Cache.get(cacheKey);
                    if (cachedModel != null) {
                        cir.setReturnValue(cachedModel);
                    } else {
                        List<BakedModel> layers = new ArrayList<>();
                        layers.add(originalModel);
                        var modelManager = Minecraft.getInstance().getModelManager();
                        if (isSalted) {
                            BakedModel saltOverlayModel = modelManager.getModel(SaltClientEvents.SALTED_OVERLAY_MODEL);
                            if (saltOverlayModel != null && !saltOverlayModel.equals(modelManager.getMissingModel())) {
                                layers.add(saltOverlayModel);
                            }
                        }
                        if (isSweetened) {
                            BakedModel sugarOverlayModel = modelManager.getModel(SaltClientEvents.SUGAR_OVERLAY_MODEL);
                            if (sugarOverlayModel != null && !sugarOverlayModel.equals(modelManager.getMissingModel())) {
                                layers.add(sugarOverlayModel);
                            }
                        }
                        if (isSatiating) {
                            BakedModel chrysanthemumOverlayModel = modelManager.getModel(SaltClientEvents.CHRYSANTHEMUM_OVERLAY_MODEL);
                            if (chrysanthemumOverlayModel != null && !chrysanthemumOverlayModel.equals(modelManager.getMissingModel())) {
                                layers.add(chrysanthemumOverlayModel);
                            }
                        }
                        if (layers.size() > 1) {
                            LayeredBakedModel layeredModel = new LayeredBakedModel(layers);
                            LayeredBakedModel.Cache.add(cacheKey, layeredModel);
                            cir.setReturnValue(layeredModel);
                        }
                    }
                }
            }
        }
    }
}

