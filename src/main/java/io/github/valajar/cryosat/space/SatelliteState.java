package io.github.valajar.cryosat.space;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.core.GlobalPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * A Survey Satellite: travels to its orbit, then joins its owner's constellation over {@link #orbitDimension} and works on at
 * most one {@link OrbitalTask} at a time. Satellites do not wear out. Mutable; change it only on the server thread.
 */
public final class SatelliteState implements PayloadState {
    public static final MapCodec<SatelliteState> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
          Level.RESOURCE_KEY_CODEC.fieldOf("orbit_dimension").forGetter(state -> state.orbitDimension),
          Orbit.CODEC.fieldOf("orbit").forGetter(state -> state.orbit),
          OrbitalTask.CODEC.optionalFieldOf("task").forGetter(state -> Optional.ofNullable(state.task))
    ).apply(instance, (dimension, orbit, task) -> new SatelliteState(dimension, orbit, task.orElse(null))));

    public final ResourceKey<Level> orbitDimension;
    public final Orbit orbit;
    @Nullable
    public OrbitalTask task;

    public SatelliteState(ResourceKey<Level> orbitDimension, Orbit orbit, @Nullable OrbitalTask task) {
        this.orbitDimension = orbitDimension;
        this.orbit = orbit;
        this.task = task;
    }

    @Override
    public PayloadType type() {
        return PayloadType.SURVEY_SATELLITE;
    }

    /** Ticks one imaging pass takes for a satellite of this tier: 4 minutes for Basic down to 1 for Ultimate. */
    public static int passTicks(RocketTier tier) {
        return (4 - tier.ordinal()) * 1_200;
    }

    /** Coverage value a satellite of this tier adds to its constellation: 1, 2, 4, 8. */
    public static int coverageValue(RocketTier tier) {
        return 1 << tier.ordinal();
    }

    public boolean isFree() {
        return task == null;
    }

    @Override
    public boolean tick(MinecraftServer server, OrbitalAsset asset, long now) {
        if (asset.state == OrbitalAsset.State.TRAVELLING) {
            if (now >= asset.nextEventAt) {
                asset.state = OrbitalAsset.State.ORBITING;
                return true;
            }
            return false;
        }
        OrbitalTask current = task;
        if (current == null || now < current.completesAt()) {
            return false;
        }
        GlobalPos terminal = current.terminal();
        ServerLevel level = server.getLevel(terminal.dimension());
        if (level == null || !level.isLoaded(terminal.pos())) {
            // Keep the result until the terminal is loaded again
            return false;
        }
        if (!(level.getBlockEntity(terminal.pos()) instanceof ISurveyTerminal surveyTerminal)) {
            // The terminal is gone: drop the result so the satellite is free again
            task = null;
            return true;
        }
        if (surveyTerminal.receiveSurvey(current, asset)) {
            task = null;
            return true;
        }
        return false;
    }
}
