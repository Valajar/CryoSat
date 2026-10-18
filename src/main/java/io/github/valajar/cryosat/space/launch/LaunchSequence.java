package io.github.valajar.cryosat.space.launch;

/**
 * Timing of the launch sequence, shared by the server (when the pad is cleared) and the client (rise and exhaust).
 * The engines burn on the pad for {@link #IGNITION_TICKS}, then the rocket accelerates out of sight.
 */
public final class LaunchSequence {
    /** Whole sequence, from ignition until the pad is cleared. */
    public static final int DURATION_TICKS = 100;
    /** Engines burn on the pad before the rocket lifts off. */
    public static final int IGNITION_TICKS = 20;
    /** Height in blocks above the pad the rocket reaches at the end of the sequence. */
    public static final float MAX_RISE = 160;

    /** @return launch progress from 0 (ignition) to 1 (out of sight) */
    public static float progress(float ticksSinceLaunch) {
        return Math.max(0, Math.min(1, ticksSinceLaunch / DURATION_TICKS));
    }

    /** @return how far the rocket has risen above the pad, in blocks; eases in like a real lift off */
    public static float rise(float ticksSinceLaunch) {
        float t = (ticksSinceLaunch - IGNITION_TICKS) / (DURATION_TICKS - IGNITION_TICKS);
        if (t <= 0) {
            return 0;
        }
        t = Math.min(1, t);
        return MAX_RISE * t * t * t;
    }

    private LaunchSequence() {
    }
}
