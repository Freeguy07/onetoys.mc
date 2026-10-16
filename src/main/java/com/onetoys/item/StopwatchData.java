package com.onetoys.item;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public record StopwatchData(boolean active, long startTick, long elapsedTicks) {
    public static final StopwatchData DEFAULT = new StopwatchData(false, 0L, 0L);

    public static final Codec<StopwatchData> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.BOOL.fieldOf("active").forGetter(StopwatchData::active),
                    Codec.LONG.fieldOf("start_tick").forGetter(StopwatchData::startTick),
                    Codec.LONG.fieldOf("elapsed_ticks").forGetter(StopwatchData::elapsedTicks)
            ).apply(instance, StopwatchData::new)
    );

    public static final StreamCodec<ByteBuf, StopwatchData> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL,
            StopwatchData::active,
            ByteBufCodecs.VAR_LONG,
            StopwatchData::startTick,
            ByteBufCodecs.VAR_LONG,
            StopwatchData::elapsedTicks,
            StopwatchData::new
    );

    public long getDisplayElapsed(long currentTick) {
        if (active) {
            return elapsedTicks + Math.max(0L, currentTick - startTick);
        }
        return elapsedTicks;
    }

    public static String formatSeconds(long ticks) {
        long sec = ticks / 20L;
        long hundredths = (ticks % 20L) * 5L;
        if (hundredths < 10L) {
            return sec + ".0" + hundredths;
        }
        return sec + "." + hundredths;
    }
}
