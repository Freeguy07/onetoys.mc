package com.onetoys.mixin.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.onetoys.module.gameplay.weather.HeightWeatherTweaks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.Heightmap;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public abstract class LevelRendererMixin {
    @Shadow @Final private Minecraft minecraft;
    @Shadow private int ticks;
    @Shadow @Final private float[] rainSizeX;
    @Shadow @Final private float[] rainSizeZ;
    @Shadow @Final private static ResourceLocation RAIN_LOCATION;
    @Shadow @Final private static ResourceLocation SNOW_LOCATION;

    @Inject(method = "renderSnowAndRain", at = @At("HEAD"), cancellable = true)
    private void onRenderSnowAndRain(LightTexture lightTexture, float partialTick, double camX, double camY, double camZ, CallbackInfo ci) {
        if (!HeightWeatherTweaks.isFeatureEnabled()) {
            return;
        }

        ClientLevel level = this.minecraft.level;
        if (level == null) {
            return;
        }

        if (level.effects().renderSnowAndRain(level, this.ticks, partialTick, lightTexture, camX, camY, camZ)) {
            ci.cancel();
            return;
        }

        float rainLevel = level.getRainLevel(partialTick);
        if (rainLevel <= 0.0F) {
            ci.cancel();
            return;
        }

        ci.cancel();
        renderSmoothedSnowAndRain(lightTexture, partialTick, camX, camY, camZ, rainLevel, level);
    }

    @Unique
    private void renderSmoothedSnowAndRain(LightTexture lightTexture, float partialTick, double camX, double camY, double camZ, float rainLevel, ClientLevel level) {
        lightTexture.turnOnLightLayer();
        int camFloorX = Mth.floor(camX);
        int camFloorY = Mth.floor(camY);
        int camFloorZ = Mth.floor(camZ);
        Tesselator tesselator = Tesselator.getInstance();

        RenderSystem.disableCull();
        RenderSystem.enableBlend();
        RenderSystem.enableDepthTest();

        int range = Minecraft.useFancyGraphics() ? 10 : 5;
        RenderSystem.depthMask(Minecraft.useShaderTransparency());
        RenderSystem.setShader(GameRenderer::getParticleShader);

        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int blend = HeightWeatherTweaks.getTransitionRange();
        float animTick = (float) this.ticks + partialTick;

        // ==========================================
        // PASS 1: RAIN
        // ==========================================
        BufferBuilder rainBuffer = null;
        boolean hasRain = false;

        for (int z = camFloorZ - range; z <= camFloorZ + range; z++) {
            for (int x = camFloorX - range; x <= camFloorX + range; x++) {
                int colIdx = (z - camFloorZ + 16) * 32 + x - camFloorX + 16;
                double halfSizeX = (double) this.rainSizeX[colIdx] * 0.5;
                double halfSizeZ = (double) this.rainSizeZ[colIdx] * 0.5;

                pos.set((double) x, camY, (double) z);
                Biome biome = level.getBiome(pos).value();
                if (!biome.hasPrecipitation()) {
                    continue;
                }

                pos.set(x, -64, z);
                if (!biome.warmEnoughToRain(pos)) {
                    // Naturally freezing biome at sea level: 100% snow, no rain
                    continue;
                }

                int motionBlockingY = level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
                int j2 = Math.max(camFloorY - range, motionBlockingY);
                int k2 = Math.max(camFloorY + range, motionBlockingY);
                if (j2 >= k2) {
                    continue;
                }

                double snowThreshold = HeightWeatherTweaks.getSnowThresholdAt((double) x + 0.5, (double) z + 0.5);
                double rainCutoffTop = snowThreshold + (double) blend;
                if ((double) j2 >= rainCutoffTop) {
                    continue;
                }

                double rainYBottom = (double) j2;
                double rainYTop = Math.min((double) k2, rainCutoffTop);
                if (rainYBottom >= rainYTop) {
                    continue;
                }

                if (!hasRain) {
                    hasRain = true;
                    RenderSystem.setShaderTexture(0, RAIN_LOCATION);
                    rainBuffer = tesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.PARTICLE);
                }

                RandomSource randomsource = RandomSource.create((long) (x * x * 3121 + x * 45238971 ^ z * z * 418711 + z * 13761));
                int i3 = this.ticks & 131071;
                int j3 = x * x * 3121 + x * 45238971 + z * z * 418711 + z * 13761 & 0xFF;
                float f2 = 3.0F + randomsource.nextFloat();
                float f3 = -((float) (i3 + j3) + partialTick) / 32.0F * f2;
                float f4 = f3 % 32.0F;

                double dx = (double) x + 0.5 - camX;
                double dz = (double) z + 0.5 - camZ;
                float distRatio = (float) Math.sqrt(dx * dx + dz * dz) / (float) range;
                float baseAlpha = ((1.0F - distRatio * distRatio) * 0.5F + 0.5F) * rainLevel;

                float alphaTop = baseAlpha * HeightWeatherTweaks.getRainBlendFactor(rainYTop, snowThreshold, blend);
                float alphaBottom = baseAlpha * HeightWeatherTweaks.getRainBlendFactor(rainYBottom, snowThreshold, blend);

                int lightY = Math.max(motionBlockingY, camFloorY);
                pos.set(x, lightY, z);
                int lightColor = LevelRenderer.getLightColor(level, pos);

                float vTop = (float) j2 * 0.25F + f4 + (float) ((double) k2 - rainYTop) * 0.25F;
                float vBottom = (float) j2 * 0.25F + f4 + (float) ((double) k2 - rainYBottom) * 0.25F;

                rainBuffer.addVertex((float) ((double) x - camX - halfSizeX + 0.5), (float) (rainYTop - camY), (float) ((double) z - camZ - halfSizeZ + 0.5))
                        .setUv(0.0F, vTop)
                        .setColor(1.0F, 1.0F, 1.0F, alphaTop)
                        .setLight(lightColor);
                rainBuffer.addVertex((float) ((double) x - camX + halfSizeX + 0.5), (float) (rainYTop - camY), (float) ((double) z - camZ + halfSizeZ + 0.5))
                        .setUv(1.0F, vTop)
                        .setColor(1.0F, 1.0F, 1.0F, alphaTop)
                        .setLight(lightColor);
                rainBuffer.addVertex((float) ((double) x - camX + halfSizeX + 0.5), (float) (rainYBottom - camY), (float) ((double) z - camZ + halfSizeZ + 0.5))
                        .setUv(1.0F, vBottom)
                        .setColor(1.0F, 1.0F, 1.0F, alphaBottom)
                        .setLight(lightColor);
                rainBuffer.addVertex((float) ((double) x - camX - halfSizeX + 0.5), (float) (rainYBottom - camY), (float) ((double) z - camZ - halfSizeZ + 0.5))
                        .setUv(0.0F, vBottom)
                        .setColor(1.0F, 1.0F, 1.0F, alphaBottom)
                        .setLight(lightColor);
            }
        }

        if (hasRain && rainBuffer != null) {
            MeshData mesh = rainBuffer.build();
            if (mesh != null) {
                BufferUploader.drawWithShader(mesh);
            }
        }

        // ==========================================
        // PASS 2: SNOW
        // ==========================================
        BufferBuilder snowBuffer = null;
        boolean hasSnow = false;

        for (int z = camFloorZ - range; z <= camFloorZ + range; z++) {
            for (int x = camFloorX - range; x <= camFloorX + range; x++) {
                int colIdx = (z - camFloorZ + 16) * 32 + x - camFloorX + 16;
                double halfSizeX = (double) this.rainSizeX[colIdx] * 0.5;
                double halfSizeZ = (double) this.rainSizeZ[colIdx] * 0.5;

                pos.set((double) x, camY, (double) z);
                Biome biome = level.getBiome(pos).value();
                if (!biome.hasPrecipitation()) {
                    continue;
                }

                int motionBlockingY = level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
                int j2 = Math.max(camFloorY - range, motionBlockingY);
                int k2 = Math.max(camFloorY + range, motionBlockingY);
                if (j2 >= k2) {
                    continue;
                }

                pos.set(x, -64, z);
                boolean naturallySnowy = !biome.warmEnoughToRain(pos);

                double snowThreshold = HeightWeatherTweaks.getSnowThresholdAt((double) x + 0.5, (double) z + 0.5);
                double snowCutoffBottom = naturallySnowy ? (double) j2 : (snowThreshold - (double) blend);

                if ((double) k2 <= snowCutoffBottom) {
                    continue;
                }

                double snowYTop = (double) k2;
                double snowYBottom = Math.max((double) j2, snowCutoffBottom);
                if (snowYBottom >= snowYTop) {
                    continue;
                }

                if (!hasSnow) {
                    hasSnow = true;
                    RenderSystem.setShaderTexture(0, SNOW_LOCATION);
                    snowBuffer = tesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.PARTICLE);
                }

                RandomSource randomsource = RandomSource.create((long) (x * x * 3121 + x * 45238971 ^ z * z * 418711 + z * 13761));
                float f8 = -((float) (this.ticks & 511) + partialTick) / 512.0F;
                float f9 = (float) (randomsource.nextDouble() + (double) animTick * 0.01 * (double) ((float) randomsource.nextGaussian()));
                float f10 = (float) (randomsource.nextDouble() + (double) (animTick * (float) randomsource.nextGaussian()) * 0.001);

                double dx = (double) x + 0.5 - camX;
                double dz = (double) z + 0.5 - camZ;
                float distRatio = (float) Math.sqrt(dx * dx + dz * dz) / (float) range;
                float baseAlpha = ((1.0F - distRatio * distRatio) * 0.3F + 0.5F) * rainLevel;

                float alphaTop = naturallySnowy ? baseAlpha : (baseAlpha * HeightWeatherTweaks.getSnowBlendFactor(snowYTop, snowThreshold, blend));
                float alphaBottom = naturallySnowy ? baseAlpha : (baseAlpha * HeightWeatherTweaks.getSnowBlendFactor(snowYBottom, snowThreshold, blend));

                int lightY = Math.max(motionBlockingY, camFloorY);
                pos.set(x, lightY, z);
                int j4 = LevelRenderer.getLightColor(level, pos);
                int k4 = j4 >> 16 & 65535;
                int l4 = j4 & 65535;
                int l3 = (k4 * 3 + 240) / 4;
                int i4 = (l4 * 3 + 240) / 4;

                float vTop = (float) j2 * 0.25F + f8 + f10 + (float) ((double) k2 - snowYTop) * 0.25F;
                float vBottom = (float) j2 * 0.25F + f8 + f10 + (float) ((double) k2 - snowYBottom) * 0.25F;

                snowBuffer.addVertex((float) ((double) x - camX - halfSizeX + 0.5), (float) (snowYTop - camY), (float) ((double) z - camZ - halfSizeZ + 0.5))
                        .setUv(0.0F + f9, vTop)
                        .setColor(1.0F, 1.0F, 1.0F, alphaTop)
                        .setUv2(i4, l3);
                snowBuffer.addVertex((float) ((double) x - camX + halfSizeX + 0.5), (float) (snowYTop - camY), (float) ((double) z - camZ + halfSizeZ + 0.5))
                        .setUv(1.0F + f9, vTop)
                        .setColor(1.0F, 1.0F, 1.0F, alphaTop)
                        .setUv2(i4, l3);
                snowBuffer.addVertex((float) ((double) x - camX + halfSizeX + 0.5), (float) (snowYBottom - camY), (float) ((double) z - camZ + halfSizeZ + 0.5))
                        .setUv(1.0F + f9, vBottom)
                        .setColor(1.0F, 1.0F, 1.0F, alphaBottom)
                        .setUv2(i4, l3);
                snowBuffer.addVertex((float) ((double) x - camX - halfSizeX + 0.5), (float) (snowYBottom - camY), (float) ((double) z - camZ - halfSizeZ + 0.5))
                        .setUv(0.0F + f9, vBottom)
                        .setColor(1.0F, 1.0F, 1.0F, alphaBottom)
                        .setUv2(i4, l3);
            }
        }

        if (hasSnow && snowBuffer != null) {
            MeshData mesh = snowBuffer.build();
            if (mesh != null) {
                BufferUploader.drawWithShader(mesh);
            }
        }

        RenderSystem.enableCull();
        RenderSystem.disableBlend();
        lightTexture.turnOffLightLayer();
    }
}
