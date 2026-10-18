package io.github.valajar.cryosat.space;

import io.netty.buffer.ByteBuf;
import java.util.function.IntFunction;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ByIdMap;
import net.minecraft.util.StringRepresentable;
import org.jetbrains.annotations.NotNull;

/**
 * Asteroid sizes. Asteroid class yields are written for a small asteroid; bigger ones multiply them. A rocket can roll
 * sizes up to one step above its tier, so better rockets find bigger asteroids more often.
 */
public enum AsteroidSize implements StringRepresentable {
    SMALL("small", 1, 3, 50),
    MEDIUM("medium", 2, 4, 30),
    LARGE("large", 4, 6, 15),
    HUGE("huge", 8, 8, 5);

    public static final StringRepresentable.EnumCodec<AsteroidSize> CODEC = StringRepresentable.fromEnum(AsteroidSize::values);
    public static final IntFunction<AsteroidSize> BY_ID = ByIdMap.continuous(AsteroidSize::ordinal, values(), ByIdMap.OutOfBoundsStrategy.CLAMP);
    public static final StreamCodec<ByteBuf, AsteroidSize> STREAM_CODEC = ByteBufCodecs.idMapper(BY_ID, AsteroidSize::ordinal);

    private final String name;
    private final int multiplier;
    private final int batches;
    private final int weight;

    AsteroidSize(String name, int multiplier, int batches, int weight) {
        this.name = name;
        this.multiplier = multiplier;
        this.batches = batches;
        this.weight = weight;
    }

    /** Yield relative to a small asteroid. */
    public int multiplier() {
        return multiplier;
    }

    /** How many batches a miner splits the asteroid's yield into. */
    public int batches() {
        return batches;
    }

    /** Relative chance of rolling this size among the sizes a rocket can reach. */
    public int weight() {
        return weight;
    }

    @NotNull
    @Override
    public String getSerializedName() {
        return name;
    }
}
