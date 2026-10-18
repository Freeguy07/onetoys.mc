package com.onetoys.worldgen.feature;

import java.util.List;

import com.mojang.serialization.Codec;
import com.onetoys.module.content.ContentModule;

import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.BulkSectionAccess;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;

/**
 * Highly optimized feature that generates 3D geological strata formations in the Nether.
 * Features:
 * - 3D orthonormal frame rotation for natural dip/strike strata incline on X and Z axes.
 * - Exact spherical and column-based range culling (eliminates dead-space loop iterations).
 * - Linear coordinate accumulation in the innermost vertical loop (eliminates multiplications).
 * - Core-rejection noise optimization (skips 3D noise evaluation for ~75% of candidate blocks).
 * - Column invariant hoisting and L1-friendly chunk section traversal via BulkSectionAccess.
 * - Zero-allocation fast smooth value noise and air-adjacency tests.
 */
public class CalciteStrataFeature extends Feature<CalciteStrataConfiguration> {

    // Noise hash constants
    private static final int PRIME_X = 374761393;
    private static final int PRIME_Y = 668265263;
    private static final int PRIME_Z = 912345671;
    private static final int PRIME_SEED = 1013904223;
    private static final float INT_TO_UNIT = 1.0F / 2147483647.0F;

    // Basis constants
    private static final float TILT_DEG = 25.0F;
    private static final float U_H = 1.0F / (float) Math.sqrt(2.0); // ~0.70710678F
    private static final float U_H_SQ = U_H * U_H; // 0.5F

    private static final float WARP_EXPANSION = 1.15F;

    public CalciteStrataFeature(Codec<CalciteStrataConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<CalciteStrataConfiguration> context) {
        if (!ContentModule.isCalciteStripesEnabled()) {
            return false;
        }

        CalciteStrataConfiguration config = context.config();
        List<BlockState> layerList = config.layers();
        if (layerList == null || layerList.isEmpty()) {
            return false;
        }

        BlockState[] layers = layerList.toArray(new BlockState[0]);
        int layerCount = layers.length;

        WorldGenLevel level = context.level();
        BlockPos origin = context.origin();
        RandomSource random = context.random();

        // Organic variation to blob dimensions
        float rx = config.horizontalRadius() * (0.85F + random.nextFloat() * 0.30F);
        float rz = config.horizontalRadius() * (0.85F + random.nextFloat() * 0.30F);
        float ry = config.verticalRadius() * (0.85F + random.nextFloat() * 0.30F);

        float invRx = 1.0F / rx;
        float invRy = 1.0F / ry;
        float invRz = 1.0F / rz;

        // Incline orthonormal basis vectors from config
        double maxTiltRad = config.maxTiltDegrees() * Mth.DEG_TO_RAD;
        float tanTilt = (float) Math.tan(maxTiltRad);
        float invLen = 1.0F / (float) Math.sqrt(1.0 + 2.0 * tanTilt * tanTilt);

        float nH = tanTilt * invLen;
        float nY = invLen;
        float wH = nY * U_H;
        float wY = -(float) Math.sqrt(2.0) * nH;

        float nHSq = nH * nH;
        float nYSq = nY * nY;
        float wHSq = wH * wH;
        float wYSq = wY * wY;

        // Randomly pick one of the 4 symmetrical diagonal dip quadrants (±X, ±Z)
        float signX = random.nextBoolean() ? 1.0F : -1.0F;
        float signZ = random.nextBoolean() ? 1.0F : -1.0F;

        // Orthonormal 3D basis vectors:
        // u is horizontal bedding axis (uy = 0)
        // n is normal to strata bedding planes (inclined on both X and Z)
        // w is inclined bedding axis
        float ux = -signZ * U_H;
        float uz = signX * U_H;

        float nx = signX * nH;
        float ny = nY;
        float nz = signZ * nH;

        float wx = signX * wH;
        float wy = wY;
        float wz = signZ * wH;

        // Pre-scaled transformation vectors: transforms world displacement directly into normalized ellipsoid space
        float uxNorm = ux * invRx;
        float uzNorm = uz * invRx;

        float nxNorm = nx * invRy;
        float nyNorm = ny * invRy;
        float nzNorm = nz * invRy;

        float wxNorm = wx * invRz;
        float wyNorm = wy * invRz;
        float wzNorm = wz * invRz;

        // Maximum radial extents including maximum 3D perimeter warp (+30% warp = sqrt(1.30) ~ 1.15x)
        float maxDx = (float) Math.sqrt(rx * rx * U_H_SQ + ry * ry * nHSq + rz * rz * wHSq) * WARP_EXPANSION;
        float maxDy = (float) Math.sqrt(ry * ry * nYSq + rz * rz * wYSq) * WARP_EXPANSION;
        float maxDz = (float) Math.sqrt(rx * rx * U_H_SQ + ry * ry * nHSq + rz * rz * wHSq) * WARP_EXPANSION;

        int limitX = (int) Math.ceil(maxDx);
        int limitY = (int) Math.ceil(maxDy);
        int limitZ = (int) Math.ceil(maxDz);

        float rMax = Math.max(rx, Math.max(ry, rz)) * WARP_EXPANSION;
        float rMaxSq = rMax * rMax;

        int originX = origin.getX();
        int originY = origin.getY();
        int originZ = origin.getZ();

        int boundedMinY = Math.max(originY - limitY, level.getMinBuildHeight());
        int boundedMaxY = Math.min(originY + limitY, level.getMaxBuildHeight() - 1);

        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        BlockPos.MutableBlockPos neighborPos = new BlockPos.MutableBlockPos();
        float discardChance = config.discardChanceOnAirExposure();

        // Unique deterministic seeds per deposit for uncorrelated 3D noise channels
        int seedPerimeter = random.nextInt();
        int seedWeathering = random.nextInt();

        // Map discardChance (0.0 to 1.0) to smooth noise threshold [-1.0, 1.0].
        // At discardChance = 0.1, threshold = -0.8F.
        float weatherThreshold = (discardChance - 0.5F) * 2.0F;

        // Feature-level air rejection: If selected (0.1 / 10% chance), abort if deposit touches cave air
        boolean discardOnAirContact = discardChance > 0.0F && random.nextFloat() < discardChance;

        try (BulkSectionAccess access = new BulkSectionAccess(level)) {
            if (discardOnAirContact && checkAirContact(access, level, pos, neighborPos, originX, originY, originZ,
                    (int) rx, (int) ry, (int) rz, limitX, limitZ, rMaxSq, boundedMinY, boundedMaxY,
                    uxNorm, uzNorm, nxNorm, nyNorm, nzNorm, wxNorm, wyNorm, wzNorm, seedPerimeter)) {
                return false;
            }

            int placedCount = 0;

            // Main placement traversal: outer column (dx, dz) iteration for optimal chunk locality and invariant hoisting
            for (int dx = -limitX; dx <= limitX; dx++) {
                int dxSq = dx * dx;
                if (dxSq > rMaxSq) continue;
                int worldX = originX + dx;
                float xNormX = dx * uxNorm;
                float xNormY = dx * nxNorm;
                float xNormZ = dx * wxNorm;
                float xRawY = dx * nx;

                float noiseX_warp = worldX * 0.20F;
                float noiseX_weather = worldX * 0.24F;

                for (int dz = -limitZ; dz <= limitZ; dz++) {
                    int dxDzSq = dxSq + dz * dz;
                    if (dxDzSq > rMaxSq) continue;
                    int worldZ = originZ + dz;

                    // Validate chunk writability once per (worldX, worldZ) column
                    pos.set(worldX, originY, worldZ);
                    if (!level.ensureCanWrite(pos)) continue;

                    // normX is constant for the entire vertical column because uy == 0
                    float normX = xNormX + dz * uzNorm;
                    float normXSq = normX * normX;

                    float xzNormY = xNormY + dz * nzNorm;
                    float xzNormZ = xNormZ + dz * wzNorm;
                    float xzRawY = xRawY + dz * nz;

                    float maxDyForCol = (float) Math.sqrt(rMaxSq - dxDzSq);
                    int colMinY = Math.max(boundedMinY, (int) (originY - maxDyForCol));
                    int colMaxY = Math.min(boundedMaxY, (int) (originY + maxDyForCol));

                    float noiseZ_warp = worldZ * 0.20F;
                    float noiseZ_weather = worldZ * 0.24F;

                    // Linear accumulation: initialize coordinates at colMinY, then step by constants per y
                    float dy0 = colMinY - originY;
                    float normY = xzNormY + dy0 * nyNorm;
                    float normZ = xzNormZ + dy0 * wyNorm;
                    float yLocal = xzRawY + dy0 * ny;

                    for (int y = colMinY; y <= colMaxY; y++, normY += nyNorm, normZ += wyNorm, yLocal += ny) {
                        float distSq = normXSq + normY * normY + normZ * normZ;

                        // Core Rejection Optimization:
                        // distSq > 1.30F is strictly outside the warped ellipsoid (warp max = +0.30F).
                        // distSq <= 0.70F is strictly inside the core (warp min = -0.30F), skipping noise sampling!
                        if (distSq > 1.30F) continue;
                        if (distSq > 0.70F) {
                            float warp = sampleSmoothNoise(noiseX_warp, y * 0.24F, noiseZ_warp, seedPerimeter);
                            if (distSq > (1.0F + warp * 0.30F)) continue;
                        }

                        pos.set(worldX, y, worldZ);
                        LevelChunkSection section = access.getSection(pos);
                        if (section == null) continue;

                        int relX = SectionPos.sectionRelative(worldX);
                        int relY = SectionPos.sectionRelative(y);
                        int relZ = SectionPos.sectionRelative(worldZ);

                        BlockState current = section.getBlockState(relX, relY, relZ);
                        if (config.target().test(current, random)) {
                            // Coherent Cliffside Weathering / Exposure Masking on exposed cave surfaces
                            if (discardChance > 0.0F && hasAdjacentAir(access, level, neighborPos, worldX, y, worldZ)) {
                                float weather = sampleSmoothNoise(noiseX_weather, y * 0.16F, noiseZ_weather, seedWeathering);
                                if (weather < weatherThreshold) {
                                    continue;
                                }
                            }

                            // Determine strata layer along rotated normal axis
                            int layerIndex = Math.floorMod(fastFloor(yLocal), layerCount);
                            section.setBlockState(relX, relY, relZ, layers[layerIndex], false);
                            placedCount++;
                        }
                    }
                }
            }

            return placedCount > 0;
        }
    }

    /**
     * Fast zero-allocation pre-scan testing if the deposit intersects cave air.
     * Evaluates extremity poles first, followed by column-culled boundary inspection.
     */
    private static boolean checkAirContact(BulkSectionAccess access, WorldGenLevel level, BlockPos.MutableBlockPos pos, BlockPos.MutableBlockPos neighborPos,
                                          int originX, int originY, int originZ,
                                          int rx, int ry, int rz,
                                          int limitX, int limitZ, float rMaxSq,
                                          int boundedMinY, int boundedMaxY,
                                          float uxNorm, float uzNorm,
                                          float nxNorm, float nyNorm, float nzNorm,
                                          float wxNorm, float wyNorm, float wzNorm,
                                          int seedPerimeter) {
        // Fast extremity checks (origin, top, bottom, cardinal poles)
        if (getBlockState(access, level, pos, originX, originY, originZ).isAir()) {
            return true;
        }
        if (isAirOrAdjacent(access, level, pos, originX, originY + ry, originZ)
                || isAirOrAdjacent(access, level, pos, originX, originY - ry, originZ)
                || isAirOrAdjacent(access, level, pos, originX + rx, originY, originZ)
                || isAirOrAdjacent(access, level, pos, originX - rx, originY, originZ)
                || isAirOrAdjacent(access, level, pos, originX, originY, originZ + rz)
                || isAirOrAdjacent(access, level, pos, originX, originY, originZ - rz)) {
            return true;
        }

        // Full pre-scan with column culling
        for (int dx = -limitX; dx <= limitX; dx++) {
            int dxSq = dx * dx;
            if (dxSq > rMaxSq) continue;
            int worldX = originX + dx;
            float xNormX = dx * uxNorm;
            float xNormY = dx * nxNorm;
            float xNormZ = dx * wxNorm;
            float noiseX_warp = worldX * 0.20F;

            for (int dz = -limitZ; dz <= limitZ; dz++) {
                int dxDzSq = dxSq + dz * dz;
                if (dxDzSq > rMaxSq) continue;
                int worldZ = originZ + dz;

                pos.set(worldX, originY, worldZ);
                if (!level.ensureCanWrite(pos)) continue;

                float normX = xNormX + dz * uzNorm;
                float normXSq = normX * normX;
                float xzNormY = xNormY + dz * nzNorm;
                float xzNormZ = xNormZ + dz * wzNorm;

                float maxDyForCol = (float) Math.sqrt(rMaxSq - dxDzSq);
                int colMinY = Math.max(boundedMinY, (int) (originY - maxDyForCol));
                int colMaxY = Math.min(boundedMaxY, (int) (originY + maxDyForCol));

                float noiseZ_warp = worldZ * 0.20F;

                float dy0 = colMinY - originY;
                float normY = xzNormY + dy0 * nyNorm;
                float normZ = xzNormZ + dy0 * wyNorm;

                for (int y = colMinY; y <= colMaxY; y++, normY += nyNorm, normZ += wyNorm) {
                    float distSq = normXSq + normY * normY + normZ * normZ;

                    if (distSq > 1.30F) continue;
                    if (distSq > 0.70F) {
                        float warp = sampleSmoothNoise(noiseX_warp, y * 0.24F, noiseZ_warp, seedPerimeter);
                        if (distSq > (1.0F + warp * 0.30F)) continue;
                    }

                    BlockState state = getBlockState(access, level, pos, worldX, y, worldZ);
                    if (state.isAir() || hasAdjacentAir(access, level, neighborPos, worldX, y, worldZ)) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    private static BlockState getBlockState(BulkSectionAccess access, WorldGenLevel level, BlockPos.MutableBlockPos pos, int x, int y, int z) {
        if (level.isOutsideBuildHeight(y)) {
            return Blocks.AIR.defaultBlockState();
        }
        pos.set(x, y, z);
        LevelChunkSection section = access.getSection(pos);
        if (section == null) {
            return Blocks.AIR.defaultBlockState();
        }
        return section.getBlockState(SectionPos.sectionRelative(x), SectionPos.sectionRelative(y), SectionPos.sectionRelative(z));
    }

    private static boolean isAirOrAdjacent(BulkSectionAccess access, WorldGenLevel level, BlockPos.MutableBlockPos pos, int x, int y, int z) {
        return getBlockState(access, level, pos, x, y, z).isAir() || hasAdjacentAir(access, level, pos, x, y, z);
    }

    private static boolean hasAdjacentAir(BulkSectionAccess access, WorldGenLevel level, BlockPos.MutableBlockPos pos, int x, int y, int z) {
        if (getBlockState(access, level, pos, x, y + 1, z).isAir()) return true;
        if (getBlockState(access, level, pos, x, y - 1, z).isAir()) return true;
        if (getBlockState(access, level, pos, x + 1, y, z).isAir()) return true;
        if (getBlockState(access, level, pos, x - 1, y, z).isAir()) return true;
        if (getBlockState(access, level, pos, x, y, z + 1).isAir()) return true;
        return getBlockState(access, level, pos, x, y, z - 1).isAir();
    }

    /**
     * Fast float floor without double conversion.
     */
    private static int fastFloor(float f) {
        int i = (int) f;
        return f < i ? i - 1 : i;
    }

    /**
     * Highly optimized 3D smooth value noise using cubic hermite interpolation (smoothstep).
     * Computes unit hashes via incremental linear combinations (3 multiplications instead of 32)
     * and uses float multiplication instead of float division.
     */
    private static float sampleSmoothNoise(float x, float y, float z, int seed) {
        int ix = fastFloor(x);
        int iy = fastFloor(y);
        int iz = fastFloor(z);

        float tx = x - ix;
        float ty = y - iy;
        float tz = z - iz;

        // Smoothstep interpolation curves (3t^2 - 2t^3)
        float sx = tx * tx * (3.0F - 2.0F * tx);
        float sy = ty * ty * (3.0F - 2.0F * ty);
        float sz = tz * tz * (3.0F - 2.0F * tz);

        int y0z0 = iy * PRIME_Y + iz * PRIME_Z + seed * PRIME_SEED;
        int y1z0 = y0z0 + PRIME_Y;
        int y0z1 = y0z0 + PRIME_Z;
        int y1z1 = y1z0 + PRIME_Z;

        int x0 = ix * PRIME_X;
        int x1 = x0 + PRIME_X;

        float c000 = hash(x0 + y0z0);
        float c100 = hash(x1 + y0z0);
        float c010 = hash(x0 + y1z0);
        float c110 = hash(x1 + y1z0);
        float c001 = hash(x0 + y0z1);
        float c101 = hash(x1 + y0z1);
        float c011 = hash(x0 + y1z1);
        float c111 = hash(x1 + y1z1);

        float c00 = c000 + (c100 - c000) * sx;
        float c10 = c010 + (c110 - c010) * sx;
        float c01 = c001 + (c101 - c001) * sx;
        float c11 = c011 + (c111 - c011) * sx;

        float c0 = c00 + (c10 - c00) * sy;
        float c1 = c01 + (c11 - c01) * sy;

        float val = c0 + (c1 - c0) * sz;
        return val * 2.0F - 1.0F; // Rescale from [0.0, 1.0] to [-1.0, 1.0]
    }

    private static float hash(int h) {
        h = (h ^ (h >> 13)) * 1274126177;
        return (h & 0x7fffffff) * INT_TO_UNIT;
    }
}
