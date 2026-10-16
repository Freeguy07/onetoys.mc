package com.onetoys.salt.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class LayeredBakedModel implements BakedModel {
    private final List<BakedModel> layers;

    public LayeredBakedModel(List<BakedModel> layers) {
        this.layers = layers;
    }

    @Override
    public @NotNull List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, @NotNull RandomSource rand, @NotNull ModelData data, @Nullable RenderType renderType) {
        List<BakedQuad> quads = new ArrayList<>();

        for (BakedModel layer : layers) {
            List<BakedQuad> layerQuads = layer.getQuads(state, side, rand, data, renderType);
            if (layerQuads != null) { quads.addAll(layerQuads); }
        }

        return quads;
    }

    @Override
    public @NotNull List<BakedQuad> getQuads(@Nullable BlockState pState, @Nullable Direction pDirection, @NotNull RandomSource pRandom) {
        return getQuads(pState, pDirection, pRandom, ModelData.EMPTY, null);
    }

    @Override
    public boolean useAmbientOcclusion() {
        return layers.get(0).useAmbientOcclusion();
    }

    @Override
    public boolean isGui3d() {
        return layers.get(0).isGui3d();
    }

    @Override
    public boolean usesBlockLight() {
        return layers.get(0).usesBlockLight();
    }

    @Override
    public boolean isCustomRenderer() {
        return layers.get(0).isCustomRenderer();
    }

    @Override
    public @NotNull TextureAtlasSprite getParticleIcon() {
        return layers.get(0).getParticleIcon();
    }

    @Override
    public @NotNull ItemOverrides getOverrides() {
        return layers.get(0).getOverrides();
    }

    @Override
    public @NotNull ItemTransforms getTransforms() {
        return layers.get(0).getTransforms();
    }

    @Override
    public BakedModel applyTransform(ItemDisplayContext transformType, PoseStack poseStack, boolean applyLeftHandTransform) {
        if (transformType.firstPerson() || transformType == ItemDisplayContext.THIRD_PERSON_LEFT_HAND || transformType == ItemDisplayContext.THIRD_PERSON_RIGHT_HAND) {
            return layers.get(0).applyTransform(transformType, poseStack, applyLeftHandTransform);
        }
        layers.get(0).applyTransform(transformType, poseStack, applyLeftHandTransform);
        return this;
    }

    @Override
    public List<RenderType> getRenderTypes(ItemStack itemStack, boolean fabulous) {
        return layers.get(0).getRenderTypes(itemStack, fabulous);
    }

    public record CacheKey(BakedModel baseModel, int flags) {}

    public static class Cache {
        private static final Map<CacheKey, BakedModel> cache = new ConcurrentHashMap<>();

        public static void add(CacheKey key, BakedModel model) {
            cache.put(key, model);
        }

        public static @Nullable BakedModel get(CacheKey key) {
            return cache.get(key);
        }

        public static void clear() {
            cache.clear();
        }
    }
}

