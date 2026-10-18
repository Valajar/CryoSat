package io.github.valajar.cryosat.superconductor.fusion;

import io.github.valajar.cryosat.superconductor.fusion.generators.GeneratorsFusionAccess;
import mekanism.api.Action;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.neoforged.fml.ModList;
import org.jetbrains.annotations.Nullable;

/**
 * The Field Coils' only view of Mekanism Generators' fusion reactor. Generators is an optional dependency, so nothing outside
 * {@code superconductor.fusion.generators} may name a Generators class: {@link #get()} only loads
 * {@link GeneratorsFusionAccess} (whose static {@code create} is resolved when first called, not when this interface loads)
 * once {@link ModList} reports Generators. Without it every lookup finds nothing.
 */
public interface FusionReactorAccess {
    String GENERATORS_MODID = "mekanismgenerators";

    /** The access for this game: Generators backed if it is loaded, otherwise {@link #MISSING}. */
    static FusionReactorAccess get() {
        return Holder.INSTANCE;
    }

    /**
     * Whether Generators is loaded and reactors can be found at all.
     */
    boolean isAvailable();

    /**
     * Looks for fusion reactor blocks in a box around {@code origin}, preferring formed reactors and then the nearest.
     *
     * @return {@code null} when there are none
     */
    @Nullable
    FoundReactor find(Level level, BlockPos origin, int horizontalRadius, int verticalRadius);

    /**
     * @return the formed reactor whose bounds are exactly {@code min} to {@code max}, or {@code null} when it is gone or
     * not formed any more
     */
    @Nullable
    Reactor get(Level level, BlockPos min, BlockPos max);

    /**
     * Fusion reactor blocks found by {@link #find}. Bounds are only known (non null) for a formed reactor.
     */
    record FoundReactor(boolean formed, @Nullable BlockPos min, @Nullable BlockPos max) {
    }

    /** A formed reactor, read and written through Generators' public multiblock data. Do not keep it beyond one tick. */
    interface Reactor {
        boolean isBurning();

        /** Joules per tick the thermocouples make right now, the reactor GUI's production rate. */
        long passiveGeneration();

        /** Whether the reactor holds water or steam, so part of its heat goes to steam rather than energy. */
        boolean isWaterCooled();

        double caseTemperature();

        /**
         * Puts energy into the reactor's own output buffer (an output only container, which accepts internal insertion).
         *
         * @return Joules actually accepted
         */
        long insertEnergy(long amount, Action action);
    }

    FusionReactorAccess MISSING = new FusionReactorAccess() {
        @Override
        public boolean isAvailable() {
            return false;
        }

        @Nullable
        @Override
        public FoundReactor find(Level level, BlockPos origin, int horizontalRadius, int verticalRadius) {
            return null;
        }

        @Nullable
        @Override
        public Reactor get(Level level, BlockPos min, BlockPos max) {
            return null;
        }
    };

    final class Holder {
        private static final FusionReactorAccess INSTANCE = ModList.get().isLoaded(GENERATORS_MODID) ? GeneratorsFusionAccess.create() : MISSING;

        private Holder() {
        }
    }
}
