package io.github.valajar.cryosat.superconductor.fusion;

import io.github.valajar.cryosat.registries.CryoBlocks;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * The shape of a Field Coil ring set, checked from its controller.
 * <p>
 * A loop is a closed flat square of Field Coils (the controller counts as one) around a fusion reactor's bounding box, with a
 * gap of {@link #MIN_GAP} or {@link #MAX_GAP} blocks between the box and the loop. The loop at the reactor's middle layer is
 * required; loops directly above and below it, at the same distance, add to the field. The controller sits in one of these
 * loops. Gap cells may hold tubes, cables and other blocks that are not full cubes, so pipes can still reach the reactor.
 * <p>
 * This is a plain scan rather than a Mekanism multiblock: Mekanism's formation protocol validates filled or hollow cuboids of
 * connected casing, while a ring is a hollow loop around a different multiblock, with nothing to share between its blocks
 * (only the controller holds tanks). Scanning at most three loops of 40 blocks once a second costs less than a cuboid
 * validator, structure cache and manager would.
 */
public final class FieldCoilRing {
    public static final int MIN_GAP = 1;
    public static final int MAX_GAP = 2;
    public static final int MAX_LOOPS = 3;
    /** How far from the controller reactor blocks are looked for, sideways and up or down. */
    public static final int SEARCH_HORIZONTAL = 10;
    public static final int SEARCH_VERTICAL = 4;

    /**
     * @param problem    {@link RingProblem#NONE} when the ring works
     * @param at         where the problem is, for problems with a position
     * @param gap        the measured gap, for {@link RingProblem#WRONG_GAP} and working rings
     * @param loops      complete loops, 0 unless the ring works
     * @param incomplete first missing block of an extra loop that was started but not finished (not counted), or null
     * @param coils      every block of the counted loops, controller included
     * @param reactorMin bounds of the reactor the ring is built around, null if none was found
     */
    public record Scan(RingProblem problem, @Nullable BlockPos at, int gap, int loops, @Nullable BlockPos incomplete, List<BlockPos> coils,
                       @Nullable BlockPos reactorMin, @Nullable BlockPos reactorMax) {

        static Scan fail(RingProblem problem) {
            return new Scan(problem, null, 0, 0, null, List.of(), null, null);
        }

        static Scan fail(RingProblem problem, @Nullable BlockPos at, int gap, BlockPos min, BlockPos max) {
            return new Scan(problem, at, gap, 0, null, List.of(), min, max);
        }

        public boolean works() {
            return problem == RingProblem.NONE;
        }
    }

    /**
     * Finds the reactor near the controller and checks the ring around it.
     */
    public static Scan scan(Level level, BlockPos controller, FusionReactorAccess access) {
        if (!access.isAvailable()) {
            return Scan.fail(RingProblem.NEEDS_GENERATORS);
        }
        FusionReactorAccess.FoundReactor found = access.find(level, controller, SEARCH_HORIZONTAL, SEARCH_VERTICAL);
        if (found == null) {
            return Scan.fail(RingProblem.NO_REACTOR);
        } else if (!found.formed() || found.min() == null || found.max() == null) {
            return Scan.fail(RingProblem.REACTOR_NOT_FORMED);
        }
        return scanAround(level, controller, found.min(), found.max());
    }

    /**
     * Checks the ring the controller belongs to around a reactor occupying {@code min} to {@code max}.
     */
    public static Scan scanAround(Level level, BlockPos controller, BlockPos min, BlockPos max) {
        int middle = (min.getY() + max.getY()) / 2;
        int layer = controller.getY() - middle;
        if (Math.abs(layer) > 1) {
            return Scan.fail(RingProblem.WRONG_LAYER, null, 0, min, max);
        }
        int outX = Math.max(0, Math.max(min.getX() - controller.getX(), controller.getX() - max.getX()));
        int outZ = Math.max(0, Math.max(min.getZ() - controller.getZ(), controller.getZ() - max.getZ()));
        int radius = Math.max(outX, outZ);
        int gap = Math.max(0, radius - 1);
        if (gap < MIN_GAP || gap > MAX_GAP) {
            return Scan.fail(RingProblem.WRONG_GAP, null, gap, min, max);
        }

        List<BlockPos> coils = new ArrayList<>();
        // The controller's own loop and the middle loop must both be complete
        List<Integer> required = layer == 0 ? List.of(middle) : List.of(controller.getY(), middle);
        for (int y : required) {
            Loop loop = checkLoop(level, controller, min, max, radius, y);
            if (loop.otherController != null) {
                return Scan.fail(RingProblem.SECOND_CONTROLLER, loop.otherController, gap, min, max);
            } else if (loop.missing != null) {
                return Scan.fail(RingProblem.BROKEN_LOOP, loop.missing, gap, min, max);
            } else if (loop.blockedGap != null) {
                return Scan.fail(RingProblem.GAP_BLOCKED, loop.blockedGap, gap, min, max);
            }
            coils.addAll(loop.coils);
        }
        int loops = required.size();
        BlockPos incomplete = null;
        for (int y = middle - 1; y <= middle + 1; y++) {
            if (required.contains(y)) {
                continue;
            }
            Loop loop = checkLoop(level, controller, min, max, radius, y);
            if (loop.otherController != null) {
                return Scan.fail(RingProblem.SECOND_CONTROLLER, loop.otherController, gap, min, max);
            } else if (loop.coils.isEmpty()) {
                // Nothing built on this layer
                continue;
            } else if (loop.missing != null) {
                if (incomplete == null) {
                    incomplete = loop.missing;
                }
                continue;
            } else if (loop.blockedGap != null) {
                return Scan.fail(RingProblem.GAP_BLOCKED, loop.blockedGap, gap, min, max);
            }
            coils.addAll(loop.coils);
            loops++;
        }
        return new Scan(RingProblem.NONE, null, gap, loops, incomplete, List.copyOf(coils), min.immutable(), max.immutable());
    }

    private record Loop(List<BlockPos> coils, @Nullable BlockPos missing, @Nullable BlockPos otherController, @Nullable BlockPos blockedGap) {
    }

    private static Loop checkLoop(Level level, BlockPos controller, BlockPos min, BlockPos max, int radius, int y) {
        List<BlockPos> coils = new ArrayList<>();
        BlockPos missing = null, otherController = null;
        for (BlockPos pos : square(min, max, radius, y)) {
            BlockState state = level.getBlockState(pos);
            if (state.is(CryoBlocks.FIELD_COIL.get())) {
                coils.add(pos);
            } else if (state.is(CryoBlocks.FIELD_COIL_CONTROLLER.get())) {
                if (pos.equals(controller)) {
                    coils.add(pos);
                } else if (otherController == null) {
                    otherController = pos;
                }
            } else if (missing == null) {
                missing = pos;
            }
        }
        BlockPos blockedGap = null;
        for (int r = 1; r < radius && blockedGap == null; r++) {
            for (BlockPos pos : square(min, max, r, y)) {
                if (level.getBlockState(pos).isCollisionShapeFullBlock(level, pos)) {
                    blockedGap = pos;
                    break;
                }
            }
        }
        return new Loop(coils, missing, otherController, blockedGap);
    }

    /**
     * The cells at Chebyshev distance {@code radius} from the box {@code min} to {@code max} on layer {@code y}, walked row by
     * row from the north west corner.
     */
    public static List<BlockPos> square(BlockPos min, BlockPos max, int radius, int y) {
        int x0 = min.getX() - radius, x1 = max.getX() + radius;
        int z0 = min.getZ() - radius, z1 = max.getZ() + radius;
        List<BlockPos> cells = new ArrayList<>(2 * (x1 - x0 + z1 - z0));
        for (int z = z0; z <= z1; z++) {
            if (z == z0 || z == z1) {
                for (int x = x0; x <= x1; x++) {
                    cells.add(new BlockPos(x, y, z));
                }
            } else {
                cells.add(new BlockPos(x0, y, z));
                cells.add(new BlockPos(x1, y, z));
            }
        }
        return cells;
    }

    private FieldCoilRing() {
    }
}
