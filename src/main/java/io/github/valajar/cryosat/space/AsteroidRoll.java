package io.github.valajar.cryosat.space;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ExtraCodecs;

/**
 * The asteroid a miner locked onto, rolled at launch and shown once the miner arrives.
 *
 * @param asteroidClass id of the {@link AsteroidClass}
 * @param size          rolled size
 * @param hardness      rolled hardness; each batch takes {@link OrbitalRegistry#BATCH_TICKS} times this
 * @param extracted     fraction of the asteroid the miner can extract (1 unless the asteroid is bigger than the miner)
 * @param cargo         everything the miner will deliver over its lifetime
 */
public record AsteroidRoll(ResourceLocation asteroidClass, AsteroidSize size, float hardness, float extracted, List<CargoEntry> cargo) {
    public static final Codec<AsteroidRoll> CODEC = RecordCodecBuilder.create(instance -> instance.group(
          ResourceLocation.CODEC.fieldOf("class").forGetter(AsteroidRoll::asteroidClass),
          AsteroidSize.CODEC.fieldOf("size").forGetter(AsteroidRoll::size),
          Codec.FLOAT.fieldOf("hardness").forGetter(AsteroidRoll::hardness),
          Codec.FLOAT.fieldOf("extracted").forGetter(AsteroidRoll::extracted),
          CargoEntry.CODEC.listOf().fieldOf("cargo").forGetter(AsteroidRoll::cargo)
    ).apply(instance, AsteroidRoll::new));

    public int batches() {
        return size.batches();
    }

    public int batchTicks() {
        return Math.max(20, Math.round(OrbitalRegistry.BATCH_TICKS * hardness));
    }

    /** @return the cargo of batch {@code index} (0 based); all batches together add up to {@link #cargo} */
    public List<CargoEntry> batch(int index) {
        int batches = batches();
        List<CargoEntry> batch = new ArrayList<>();
        for (CargoEntry entry : cargo) {
            int count = entry.count() / batches + (index < entry.count() % batches ? 1 : 0);
            if (count > 0) {
                batch.add(entry.withCount(count));
            }
        }
        return batch;
    }
}
