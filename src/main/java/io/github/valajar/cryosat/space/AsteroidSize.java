package io.github.valajar.cryosat.space;

import io.netty.buffer.ByteBuf;
import java.util.function.IntFunction;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ByIdMap;
import net.minecraft.util.StringRepresentable;
import org.jetbrains.annotations.NotNull;

/**
 * Asteroid sizes. Asteroid class yields are written for a small asteroid; bigger ones multiply them. Which sizes a rocket
 * finds depends on its tier, see {@link RocketTier#sizeWeight}.
 */
public enum AsteroidSize implements StringRepresentable {
    SMALL("small", 1, 3),
    MEDIUM("medium", 2, 4),
    LARGE("large", 4, 6),
    HUGE("huge", 8, 8);

    public static final StringRepresentable.EnumCodec<AsteroidSize> CODEC = StringRepresentable.fromEnum(AsteroidSize::values);
    public static final IntFunction<AsteroidSize> BY_ID = ByIdMap.continuous(AsteroidSize::ordinal, values(), ByIdMap.OutOfBoundsStrategy.CLAMP);
    public static final StreamCodec<ByteBuf, AsteroidSize> STREAM_CODEC = ByteBufCodecs.idMapper(BY_ID, AsteroidSize::ordinal);

    private final String name;
    private final int multiplier;
    private final int batches;

    AsteroidSize(String name, int multiplier, int batches) {
        this.name = name;
        this.multiplier = multiplier;
        this.batches = batches;
    }

    /** Yield relative to a small asteroid. */
    public int multiplier() {
        return multiplier;
    }

    /** How many batches a miner splits the asteroid's yield into. */
    public int batches() {
        return batches;
    }

    @NotNull
    @Override
    public String getSerializedName() {
        return name;
    }
}
