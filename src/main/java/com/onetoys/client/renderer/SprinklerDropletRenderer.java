package com.onetoys.client.renderer;

import com.onetoys.entity.SprinklerDropletEntity;

import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class SprinklerDropletRenderer extends EntityRenderer<SprinklerDropletEntity> {

    public SprinklerDropletRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public ResourceLocation getTextureLocation(SprinklerDropletEntity entity) {
        return null;
    }

    @Override
    public boolean shouldRender(SprinklerDropletEntity entity, Frustum camera, double camX, double camY, double camZ) {
        return false; // Visual stream is rendered via trailing splash and water particles in tick()
    }
}
