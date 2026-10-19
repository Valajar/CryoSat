package io.github.valajar.cryosat.space;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.RandomSource;

/**
 * Where a satellite flies, rolled at launch. Purely cosmetic for now: orbits stay abstract in gameplay, but a future globe view
 * can draw existing satellites without a migration.
 *
 * @param altitude    kilometres above the surface
 * @param inclination degrees against the equator
 * @param phase       position along the orbit at launch, 0 to 1
 * @param period      ticks per revolution
 */
public record Orbit(int altitude, float inclination, float phase, int period) {
    public static final Codec<Orbit> CODEC = RecordCodecBuilder.create(instance -> instance.group(
          Codec.INT.fieldOf("altitude").forGetter(Orbit::altitude),
          Codec.FLOAT.fieldOf("inclination").forGetter(Orbit::inclination),
          Codec.FLOAT.fieldOf("phase").forGetter(Orbit::phase),
          Codec.INT.fieldOf("period").forGetter(Orbit::period)
    ).apply(instance, Orbit::new));

    /** A low Earth orbit: 400 to 1200 km, any inclination, roughly 1.5 to 2 hours real orbit time scaled to game days. */
    public static Orbit roll(RandomSource random) {
        int altitude = 400 + random.nextInt(801);
        return new Orbit(altitude, random.nextFloat() * 180, random.nextFloat(), 6_000 + altitude * 5);
    }
}
