package io.github.valajar.cryosat.space;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.util.RandomSource;

/**
 * What the Superconducting Separator does to a batch: {@link #MULTIPLIER} on everything, plus the asteroid class's bonus
 * extras rolled once per batch.
 */
public final class SeparatorBonus {
    public static final double MULTIPLIER = 1.5;

    public static List<CargoEntry> apply(List<CargoEntry> batch, OrbitalAsset from, RandomSource random) {
        List<CargoEntry> result = new ArrayList<>();
        for (CargoEntry entry : batch) {
            CargoEntry.merge(result, entry.withCount((int) Math.round(entry.count() * MULTIPLIER)));
        }
        AsteroidRoll asteroid = from.miner() == null ? null : from.miner().asteroid;
        AsteroidClass asteroidClass = asteroid == null ? null : SpaceData.ASTEROIDS.get(asteroid.asteroidClass());
        if (asteroidClass != null) {
            for (CargoEntry extra : asteroidClass.rollBonus(random)) {
                CargoEntry.merge(result, extra);
            }
        }
        return result;
    }

    private SeparatorBonus() {
    }
}
