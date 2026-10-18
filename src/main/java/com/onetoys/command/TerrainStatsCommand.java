package com.onetoys.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.onetoys.Onetoys;
import com.onetoys.worldgen.surface.FarAlpha;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

@EventBusSubscriber(modid = Onetoys.MODID)
public class TerrainStatsCommand {
    private static final Logger LOGGER = LoggerFactory.getLogger("TerrainStats");

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        register(event.getDispatcher());
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("onetoys")
                        .then(Commands.literal("terrainstats")
                                .requires(s -> s.hasPermission(2))
                                .then(Commands.argument("distance", IntegerArgumentType.integer())
                                        .executes(ctx -> executeStats(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "distance")))))
        );
    }

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        Path flag = Paths.get("run_stats.flag");
        if (Files.exists(flag)) {
            try {
                Files.deleteIfExists(flag);
            } catch (Exception ignored) {
            }
            ServerLevel overworld = event.getServer().overworld();
            int[] distances = {500, 1500, 2200, 2700, 3500};
            for (int d : distances) {
                String result = computeStats(overworld, d);
                LOGGER.info(result);
                System.out.println(result);
            }
            event.getServer().halt(false);
        }
    }

    private static int executeStats(CommandSourceStack source, int distance) {
        ServerLevel overworld = source.getServer().overworld();
        String result = computeStats(overworld, distance);
        source.sendSuccess(() -> Component.literal(result), false);
        return 1;
    }

    public static String computeStats(ServerLevel level, int distance) {
        ServerChunkCache chunkSource = level.getChunkSource();
        ChunkGenerator generator = chunkSource.getGenerator();
        RandomState randomState = chunkSource.randomState();

        double centerAlpha = FarAlpha.at(distance, 0);

        List<Integer> land = new ArrayList<>();
        List<Integer> ocean = new ArrayList<>();

        for (int i = -16; i <= 16; i++) {
            int x = distance + (i * 32);
            for (int j = -16; j <= 16; j++) {
                int z = j * 32;
                int h = generator.getBaseHeight(x, z, Heightmap.Types.OCEAN_FLOOR_WG, level, randomState);
                if (h >= 64) {
                    land.add(h);
                } else {
                    ocean.add(h);
                }
            }
        }

        int sampleCount = land.size() + ocean.size();
        double landFraction = (double) land.size() / sampleCount;

        Collections.sort(land);
        double landMean = land.isEmpty() ? 0.0 : land.stream().mapToInt(Integer::intValue).average().orElse(0.0);
        double landMedian = percentile(land, 0.50);
        double landP10 = percentile(land, 0.10);
        double landP90 = percentile(land, 0.90);
        int landMax = land.isEmpty() ? 0 : land.get(land.size() - 1);

        double oceanMean = ocean.isEmpty() ? 0.0 : ocean.stream().mapToInt(Integer::intValue).average().orElse(0.0);

        return String.format(Locale.ROOT,
                "distance=%d: alpha=%.4f samples=%d land_frac=%.4f land_mean=%.2f land_median=%.2f land_p10=%.2f land_p90=%.2f land_max=%d ocean_mean=%.2f",
                distance, centerAlpha, sampleCount, landFraction, landMean, landMedian, landP10, landP90, landMax, oceanMean);
    }

    private static double percentile(List<Integer> sorted, double p) {
        if (sorted.isEmpty()) return Double.NaN;
        if (sorted.size() == 1) return sorted.get(0);
        double rank = p * (sorted.size() - 1);
        int lower = (int) Math.floor(rank);
        int upper = (int) Math.ceil(rank);
        double weight = rank - lower;
        return sorted.get(lower) * (1.0 - weight) + sorted.get(upper) * weight;
    }
}
