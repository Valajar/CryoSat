package io.github.valajar.cryosat.space;

import io.netty.buffer.ByteBuf;
import java.util.function.IntFunction;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ByIdMap;
import net.minecraft.util.StringRepresentable;
import org.jetbrains.annotations.NotNull;

/**
 * What a rocket carries. Destinations list the payloads that work there. Each payload keeps its own state in orbit, a
 * {@link PayloadState}. The Atmospheric Scoop and Dyson segment come later.
 */
public enum PayloadType implements StringRepresentable {
    ASTEROID_MINER("asteroid_miner"),
    SURVEY_SATELLITE("survey_satellite");

    public static final StringRepresentable.EnumCodec<PayloadType> CODEC = StringRepresentable.fromEnum(PayloadType::values);
    public static final IntFunction<PayloadType> BY_ID = ByIdMap.continuous(PayloadType::ordinal, values(), ByIdMap.OutOfBoundsStrategy.CLAMP);
    public static final StreamCodec<ByteBuf, PayloadType> STREAM_CODEC = ByteBufCodecs.idMapper(BY_ID, PayloadType::ordinal);

    private final String name;

    PayloadType(String name) {
        this.name = name;
    }

    @NotNull
    @Override
    public String getSerializedName() {
        return name;
    }
}
