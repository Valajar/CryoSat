package io.github.valajar.cryosat.space.survey;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import java.util.Locale;
import java.util.UUID;
import java.util.function.IntFunction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ByIdMap;
import net.minecraft.util.StringRepresentable;
import org.jetbrains.annotations.NotNull;

/**
 * One entry of an Imaging Terminal's log: what an imaging pass found.
 *
 * @param id          id of the pass ({@link io.github.valajar.cryosat.space.OrbitalTask#id()})
 * @param target      survey target id
 * @param pos         reported location (the true one shifted inside the uncertainty radius), only meaningful when found
 * @param uncertainty radius in blocks the true location lies within, 0 when exact
 * @param time        overworld game time the result came in
 */
public record SurveyResult(UUID id, ResourceLocation target, Outcome outcome, BlockPos pos, int uncertainty, long time) {
    public static final Codec<SurveyResult> CODEC = RecordCodecBuilder.create(instance -> instance.group(
          UUIDUtil.CODEC.fieldOf("id").forGetter(SurveyResult::id),
          ResourceLocation.CODEC.fieldOf("target").forGetter(SurveyResult::target),
          Outcome.CODEC.fieldOf("outcome").forGetter(SurveyResult::outcome),
          BlockPos.CODEC.optionalFieldOf("pos", BlockPos.ZERO).forGetter(SurveyResult::pos),
          Codec.INT.optionalFieldOf("uncertainty", 0).forGetter(SurveyResult::uncertainty),
          Codec.LONG.fieldOf("time").forGetter(SurveyResult::time)
    ).apply(instance, SurveyResult::new));
    public static final StreamCodec<ByteBuf, SurveyResult> STREAM_CODEC = StreamCodec.composite(
          UUIDUtil.STREAM_CODEC, SurveyResult::id,
          ResourceLocation.STREAM_CODEC, SurveyResult::target,
          Outcome.STREAM_CODEC, SurveyResult::outcome,
          BlockPos.STREAM_CODEC, SurveyResult::pos,
          ByteBufCodecs.VAR_INT, SurveyResult::uncertainty,
          ByteBufCodecs.VAR_LONG, SurveyResult::time,
          SurveyResult::new
    );

    public boolean found() {
        return outcome == Outcome.FOUND;
    }

    /** Horizontal distance in blocks from {@code from} to the reported location. */
    public int distanceFrom(BlockPos from) {
        return (int) Math.round(Math.hypot(pos.getX() - from.getX(), pos.getZ() - from.getZ()));
    }

    public enum Outcome implements StringRepresentable {
        /** A structure was found; the position is set. */
        FOUND,
        /** Nothing unclaimed of this type within the constellation's range. */
        NOT_FOUND,
        /** An underground target came in, but no SQUID Array next to the terminal could run the deep scan. */
        NO_DEEP_SCAN;

        public static final Codec<Outcome> CODEC = StringRepresentable.fromEnum(Outcome::values);
        public static final IntFunction<Outcome> BY_ID = ByIdMap.continuous(Outcome::ordinal, values(), ByIdMap.OutOfBoundsStrategy.ZERO);
        public static final StreamCodec<ByteBuf, Outcome> STREAM_CODEC = ByteBufCodecs.idMapper(BY_ID, Outcome::ordinal);

        @NotNull
        @Override
        public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }
    }
}
