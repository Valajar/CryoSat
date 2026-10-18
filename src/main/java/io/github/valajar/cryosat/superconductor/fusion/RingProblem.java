package io.github.valajar.cryosat.superconductor.fusion;

import java.util.function.IntFunction;
import net.minecraft.util.ByIdMap;

/**
 * What stops a Field Coil ring from working, as found by {@link FieldCoilRing#scan}. Problems with a position report where
 * to look; {@link #WRONG_GAP} reports the gap it measured.
 */
public enum RingProblem {
    NONE(FusionCoilLang.RING_COMPLETE),
    NEEDS_GENERATORS(FusionCoilLang.NEEDS_GENERATORS),
    NO_REACTOR(FusionCoilLang.NO_REACTOR),
    REACTOR_NOT_FORMED(FusionCoilLang.REACTOR_NOT_FORMED),
    WRONG_LAYER(FusionCoilLang.WRONG_LAYER),
    WRONG_GAP(FusionCoilLang.WRONG_GAP),
    BROKEN_LOOP(FusionCoilLang.BROKEN_LOOP),
    GAP_BLOCKED(FusionCoilLang.GAP_BLOCKED),
    SECOND_CONTROLLER(FusionCoilLang.SECOND_CONTROLLER);

    public static final IntFunction<RingProblem> BY_ID = ByIdMap.continuous(RingProblem::ordinal, values(), ByIdMap.OutOfBoundsStrategy.ZERO);

    private final FusionCoilLang lang;

    RingProblem(FusionCoilLang lang) {
        this.lang = lang;
    }

    public FusionCoilLang lang() {
        return lang;
    }

    /** Whether the GUI text names the block position where the problem is. */
    public boolean hasPosition() {
        return this == BROKEN_LOOP || this == GAP_BLOCKED || this == SECOND_CONTROLLER;
    }
}
