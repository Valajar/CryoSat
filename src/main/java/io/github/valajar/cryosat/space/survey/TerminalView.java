package io.github.valajar.cryosat.space.survey;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;

/**
 * What the Imaging Terminal screen shows besides the log, built on the server and synced through Mekanism container sync.
 */
public final class TerminalView {
    /**
     * A survey target in the terminal's list.
     *
     * @param available whether it can be requested here now: its structures exist in this world and, for a deep target, a
     *                  SQUID Array next to the terminal is ready
     */
    public record TargetInfo(ResourceLocation id, ResourceLocation icon, boolean deep, boolean available) {
        public static final StreamCodec<ByteBuf, TargetInfo> STREAM_CODEC = StreamCodec.composite(
              ResourceLocation.STREAM_CODEC, TargetInfo::id,
              ResourceLocation.STREAM_CODEC, TargetInfo::icon,
              ByteBufCodecs.BOOL, TargetInfo::deep,
              ByteBufCodecs.BOOL, TargetInfo::available,
              TargetInfo::new
        );
    }

    /**
     * An imaging pass in progress for this terminal.
     *
     * @param completesAt overworld game time the pass is done
     */
    public record PassInfo(ResourceLocation target, long completesAt) {
        public static final StreamCodec<ByteBuf, PassInfo> STREAM_CODEC = StreamCodec.composite(
              ResourceLocation.STREAM_CODEC, PassInfo::target,
              ByteBufCodecs.VAR_LONG, PassInfo::completesAt,
              PassInfo::new
        );
    }

    /**
     * The viewing player's constellation over the terminal's dimension.
     *
     * @param coverage    0 to just below 1
     * @param uncertainty radius in blocks, 0 when results are exact
     * @param range       search range in blocks
     * @param cost        Joules one request costs now
     */
    public record CoverageInfo(int satellites, int free, double coverage, int uncertainty, int range, long cost) {
        public static final CoverageInfo NONE = new CoverageInfo(0, 0, 0, 0, 0, 0);
        public static final StreamCodec<ByteBuf, CoverageInfo> STREAM_CODEC = StreamCodec.composite(
              ByteBufCodecs.VAR_INT, CoverageInfo::satellites,
              ByteBufCodecs.VAR_INT, CoverageInfo::free,
              ByteBufCodecs.DOUBLE, CoverageInfo::coverage,
              ByteBufCodecs.VAR_INT, CoverageInfo::uncertainty,
              ByteBufCodecs.VAR_INT, CoverageInfo::range,
              ByteBufCodecs.VAR_LONG, CoverageInfo::cost,
              CoverageInfo::new
        );
    }

    private TerminalView() {
    }
}
