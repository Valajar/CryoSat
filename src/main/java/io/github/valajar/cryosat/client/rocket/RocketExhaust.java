package io.github.valajar.cryosat.client.rocket;

import io.github.valajar.cryosat.space.RocketDesign;
import io.github.valajar.cryosat.space.RocketTier;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;

/**
 * Launch exhaust, built from vanilla particles on the client only; nothing here reads or changes blocks.
 * <ul>
 *     <li>a flame jet straight down out of the nozzle, densest at ignition;</li>
 *     <li>smoke rolling outward across the pad while the rocket is still low;</li>
 *     <li>a rising plume of campfire smoke that outlives the launch and lingers over the pad;</li>
 *     <li>once climbing, a smoke trail under the nozzle.</li>
 * </ul>
 * What burns sets the look: solid grain is the smokiest, LOX and ethene burns orange and sooty, LOX and hydrogen (Elite
 * and up) burns pale blue and leaves white vapour rather than smoke.
 */
final class RocketExhaust {
    /** Share of the launch spent at ignition: the most flame, smoke rolling over the pad. */
    private static final float IGNITION = 0.15F;
    /** Share of the launch during which smoke still gathers on the pad and the plume keeps growing. */
    private static final float PAD_SMOKE = 0.4F;
    /** Length in blocks of the streak the particles of one tick spread over at the end of the launch, so a fast climb leaves a trail. */
    private static final double MAX_STREAK = 1.5;

    static void tick(ClientLevel level, Vec3 nozzle, RocketDesign design, float progress) {
        RandomSource random = level.random;
        Burn burn = Burn.of(design.tier());
        float ignition = 1 - Mth.clamp(progress / IGNITION, 0, 1);
        float pad = 1 - Mth.clamp(progress / PAD_SMOKE, 0, 1);
        double streak = MAX_STREAK * Mth.clamp(progress, 0, 1);

        // Flame jet
        int flames = 3 + Mth.ceil(7 * ignition);
        double flameSpread = 0.03 + 0.08 * ignition;
        for (int i = 0; i < flames; i++) {
            ParticleOptions type = burn.blueFlame && random.nextInt(3) != 0 ? ParticleTypes.SOUL_FIRE_FLAME : ParticleTypes.FLAME;
            level.addParticle(type, nozzle.x + spread(random, 0.15), nozzle.y - 0.1 - random.nextDouble() * streak, nozzle.z + spread(random, 0.15),
                  spread(random, flameSpread), -0.3 - 0.25 * random.nextDouble(), spread(random, flameSpread));
        }
        if (pad > 0) {
            // Smoke rolling outward across the pad
            int billow = Mth.ceil(5 * pad * burn.smoke);
            for (int i = 0; i < billow; i++) {
                double angle = random.nextDouble() * Mth.TWO_PI;
                double cos = Math.cos(angle), sin = Math.sin(angle);
                double speed = 0.15 + 0.25 * random.nextDouble();
                ParticleOptions type = burn.sooty && random.nextInt(3) == 0 ? ParticleTypes.LARGE_SMOKE : ParticleTypes.CLOUD;
                level.addParticle(type, nozzle.x + 0.5 * cos, nozzle.y + 0.1 + 0.3 * random.nextDouble(), nozzle.z + 0.5 * sin,
                      speed * cos, 0.01 + 0.02 * random.nextDouble(), speed * sin);
            }
            // Rising plume: campfire smoke lives for several seconds (signal smoke for over ten), so it stays over the
            // pad after the rocket has gone. Visible from far away, like a campfire's.
            int plume = random.nextFloat() < 0.4F + 0.6F * pad ? 1 + (ignition > 0 ? 1 : 0) : 0;
            for (int i = 0; i < plume; i++) {
                ParticleOptions type = random.nextInt(3) == 0 ? ParticleTypes.CAMPFIRE_SIGNAL_SMOKE : ParticleTypes.CAMPFIRE_COSY_SMOKE;
                level.addAlwaysVisibleParticle(type, true, nozzle.x + spread(random, 1.2), nozzle.y + 0.5 * random.nextDouble(),
                      nozzle.z + spread(random, 1.2), spread(random, 0.04), 0.03 + 0.05 * random.nextDouble(), spread(random, 0.04));
            }
        }
        // Trail once climbing
        int trail = Mth.ceil(2 * burn.smoke * (1 - ignition));
        for (int i = 0; i < trail; i++) {
            ParticleOptions type = burn.sooty && random.nextBoolean() ? ParticleTypes.LARGE_SMOKE : ParticleTypes.CLOUD;
            level.addParticle(type, nozzle.x + spread(random, 0.25), nozzle.y - 0.5 - random.nextDouble() * streak, nozzle.z + spread(random, 0.25),
                  spread(random, 0.05), -0.05 - 0.1 * random.nextDouble(), spread(random, 0.05));
        }
    }

    private static double spread(RandomSource random, double size) {
        return (random.nextDouble() - 0.5) * 2 * size;
    }

    /**
     * @param smoke     how much smoke, relative to a liquid fuelled rocket
     * @param sooty     dark smoke mixed in with the white
     * @param blueFlame hydrogen flame
     */
    private record Burn(float smoke, boolean sooty, boolean blueFlame) {
        private static final Burn SOLID = new Burn(1.5F, true, false);
        private static final Burn ETHENE = new Burn(1, true, false);
        private static final Burn HYDROGEN = new Burn(0.8F, false, true);

        static Burn of(RocketTier tier) {
            return switch (tier) {
                case BASIC -> SOLID;
                case ADVANCED -> ETHENE;
                case ELITE, ULTIMATE -> HYDROGEN;
            };
        }
    }

    private RocketExhaust() {
    }
}
