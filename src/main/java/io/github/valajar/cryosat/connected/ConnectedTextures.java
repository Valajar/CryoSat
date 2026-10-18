package io.github.valajar.cryosat.connected;

import java.util.Arrays;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Connected textures, the "compact" method: each face of a block is drawn as four quarters, and each quarter picks one of
 * five {@link Piece pieces} from the two edge neighbours next to it in the face's plane and the diagonal neighbour between
 * them. A neighbour connects when it is a part (a block this block connects to) and the block in front of it, along the
 * face normal, is not, so the outside of a wall connects and faces buried inside a structure never matter.
 * <p>
 * Pure logic with no client classes, so game tests can check it on the dedicated server. The client model
 * ({@code client.model.ConnectedBakedModel}) draws the quarters; {@code tools/textures/connected.py} is the same rule in
 * Python for previews, keep the two in step.
 */
public final class ConnectedTextures {
    /** Quarters of a face, in texture space: top left, top right, bottom left, bottom right. */
    public static final int QUARTERS = 4;

    /** One quarter of a face. Each piece is a full texture whose four quarters are all in that state. */
    public enum Piece {
        /** Neither edge neighbour connects: the isolated block's corner, border on both sides. */
        CORNER("corner"),
        /** Only the left or right neighbour connects: the border runs horizontally along the top or bottom. */
        HORIZONTAL("horizontal"),
        /** Only the neighbour above or below connects: the border runs vertically along the left or right. */
        VERTICAL("vertical"),
        /** Both edge neighbours connect but the diagonal does not: only the inner corner notch. */
        INNER("inner"),
        /** All three connect: no border. */
        CENTER("center");

        private static final Piece[] VALUES = values();
        private final String textureKey;

        Piece(String textureKey) {
            this.textureKey = textureKey;
        }

        /** Texture variable of this piece in a {@code cryosat:connected} model. */
        public String textureKey() {
            return textureKey;
        }

        public static Piece byIndex(int index) {
            return VALUES[index];
        }
    }

    /** The quarter choice from its neighbours. */
    public static Piece piece(boolean horizontal, boolean vertical, boolean diagonal) {
        if (horizontal && vertical) {
            return diagonal ? Piece.CENTER : Piece.INNER;
        } else if (horizontal) {
            return Piece.HORIZONTAL;
        } else if (vertical) {
            return Piece.VERTICAL;
        }
        return Piece.CORNER;
    }

    // The world directions a face's texture runs along, from Minecraft's default face UVs (BlockElement.uvsByFace):
    // u (texture right) and v (texture down). Indexed by Direction.get3DDataValue().
    private static final Direction[] RIGHT = new Direction[6];
    private static final Direction[] DOWN = new Direction[6];

    static {
        axes(Direction.DOWN, Direction.EAST, Direction.NORTH);
        axes(Direction.UP, Direction.EAST, Direction.SOUTH);
        axes(Direction.NORTH, Direction.WEST, Direction.DOWN);
        axes(Direction.SOUTH, Direction.EAST, Direction.DOWN);
        axes(Direction.WEST, Direction.SOUTH, Direction.DOWN);
        axes(Direction.EAST, Direction.NORTH, Direction.DOWN);
    }

    private static void axes(Direction face, Direction right, Direction down) {
        RIGHT[face.get3DDataValue()] = right;
        DOWN[face.get3DDataValue()] = down;
    }

    /** The world direction a face's texture runs to the right. */
    public static Direction textureRight(Direction face) {
        return RIGHT[face.get3DDataValue()];
    }

    /** The world direction a face's texture runs down. */
    public static Direction textureDown(Direction face) {
        return DOWN[face.get3DDataValue()];
    }

    /**
     * The pieces of every face from the 3x3x3 neighbourhood of a block. Every block the rule looks at (an edge or diagonal
     * neighbour, and the block in front of it) lies inside that cube.
     *
     * @param part whether the block at offset (dx, dy, dz), each in -1..1, is a part; see {@link #index}
     */
    public static Connections compute(boolean[] part) {
        int[] faces = new int[6];
        for (Direction face : Direction.values()) {
            Direction right = textureRight(face), down = textureDown(face);
            int packed = 0;
            for (int quarter = 0; quarter < QUARTERS; quarter++) {
                Direction h = (quarter & 1) == 0 ? right.getOpposite() : right;
                Direction v = (quarter & 2) == 0 ? down.getOpposite() : down;
                boolean horizontal = connects(part, face, h, null);
                boolean vertical = connects(part, face, v, null);
                boolean diagonal = connects(part, face, h, v);
                packed |= piece(horizontal, vertical, diagonal).ordinal() << (3 * quarter);
            }
            faces[face.get3DDataValue()] = packed;
        }
        return new Connections(faces);
    }

    /** Reads the 3x3x3 neighbourhood of {@code pos} and computes its connections. */
    public static Connections compute(BlockGetter level, BlockPos pos, Predicate<BlockState> isPart) {
        boolean[] part = new boolean[27];
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    part[index(dx, dy, dz)] = (dx | dy | dz) != 0 && isPart.test(level.getBlockState(p.setWithOffset(pos, dx, dy, dz)));
                }
            }
        }
        return compute(part);
    }

    /** Index of the offset (dx, dy, dz), each in -1..1, in a neighbourhood array. */
    public static int index(int dx, int dy, int dz) {
        return (dx + 1) * 9 + (dy + 1) * 3 + dz + 1;
    }

    private static boolean connects(boolean[] part, Direction face, Direction a, Direction b) {
        int dx = a.getStepX() + (b == null ? 0 : b.getStepX());
        int dy = a.getStepY() + (b == null ? 0 : b.getStepY());
        int dz = a.getStepZ() + (b == null ? 0 : b.getStepZ());
        return part[index(dx, dy, dz)] && !part[index(dx + face.getStepX(), dy + face.getStepY(), dz + face.getStepZ())];
    }

    /** The pieces of all six faces of one block. */
    public static final class Connections {
        /** A block with no connected neighbours: every quarter is a corner, so it looks like the plain texture. */
        public static final Connections ISOLATED = new Connections(new int[6]);

        // Per face, 3 bits per quarter
        private final int[] faces;

        private Connections(int[] faces) {
            this.faces = faces;
        }

        public Piece piece(Direction face, int quarter) {
            return Piece.byIndex((faces[face.get3DDataValue()] >> (3 * quarter)) & 7);
        }

        /** @return the piece all four quarters of the face share, or null if they differ */
        public Piece uniform(Direction face) {
            Piece first = piece(face, 0);
            for (int quarter = 1; quarter < QUARTERS; quarter++) {
                if (piece(face, quarter) != first) {
                    return null;
                }
            }
            return first;
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof Connections other && Arrays.equals(faces, other.faces);
        }

        @Override
        public int hashCode() {
            return Arrays.hashCode(faces);
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder("Connections{");
            for (Direction face : Direction.values()) {
                sb.append(face.getSerializedName()).append('=');
                for (int quarter = 0; quarter < QUARTERS; quarter++) {
                    sb.append(quarter == 0 ? "" : ",").append(piece(face, quarter).textureKey());
                }
                sb.append(' ');
            }
            return sb.append('}').toString();
        }
    }

    private ConnectedTextures() {
    }
}
