package io.github.valajar.cryosat.gametest;

import io.github.valajar.cryosat.CryoSat;
import io.github.valajar.cryosat.connected.ConnectedTextures;
import io.github.valajar.cryosat.connected.ConnectedTextures.Connections;
import io.github.valajar.cryosat.connected.ConnectedTextures.Piece;
import io.github.valajar.cryosat.registries.CryoBlocks;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * The connected texture rule ({@link ConnectedTextures}) behind the Liquefaction Plant casing and the Launch Pad deck. The
 * model itself only draws on the client; these check the neighbour rule it uses.
 */
@GameTestHolder(CryoSat.MODID)
@PrefixGameTestTemplate(false)
public class ConnectedTextureTests {
    private static final int TL = 0, TR = 1, BL = 2, BR = 3;

    @GameTest(template = "empty")
    public static void pieceFromNeighbourFlags(GameTestHelper helper) {
        // horizontal, vertical, diagonal -> piece
        check(helper, false, false, false, Piece.CORNER);
        check(helper, false, false, true, Piece.CORNER);
        check(helper, true, false, false, Piece.HORIZONTAL);
        check(helper, true, false, true, Piece.HORIZONTAL);
        check(helper, false, true, false, Piece.VERTICAL);
        check(helper, false, true, true, Piece.VERTICAL);
        check(helper, true, true, false, Piece.INNER);
        check(helper, true, true, true, Piece.CENTER);
        helper.succeed();
    }

    private static void check(GameTestHelper helper, boolean h, boolean v, boolean d, Piece expected) {
        Piece piece = ConnectedTextures.piece(h, v, d);
        helper.assertTrue(piece == expected, "piece(" + h + ", " + v + ", " + d + ") = " + piece + ", expected " + expected);
    }

    @GameTest(template = "empty")
    public static void neighbourhoodPatterns(GameTestHelper helper) {
        // Alone: every quarter of every face is a corner, so it draws like the plain texture
        helper.assertTrue(ConnectedTextures.compute(new boolean[27]).equals(Connections.ISOLATED), "an isolated block should be all corners");

        // In the middle of a floor: the top and bottom are seamless, the buried sides do not connect (the neighbours to
        // either side have a part in front of them)
        boolean[] floor = new boolean[27];
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                floor[ConnectedTextures.index(dx, 0, dz)] = dx != 0 || dz != 0;
            }
        }
        Connections c = ConnectedTextures.compute(floor);
        helper.assertTrue(c.uniform(Direction.UP) == Piece.CENTER && c.uniform(Direction.DOWN) == Piece.CENTER, "floor top and bottom: " + c);
        helper.assertTrue(c.uniform(Direction.NORTH) == Piece.CORNER, "buried north face should not connect: " + c);

        // An L of neighbours to the east and south: on the top face (texture right = east, down = south) the bottom right
        // quarter is an inner corner, the others are edges and a corner
        boolean[] l = new boolean[27];
        l[ConnectedTextures.index(1, 0, 0)] = true;
        l[ConnectedTextures.index(0, 0, 1)] = true;
        c = ConnectedTextures.compute(l);
        expect(helper, c, Direction.UP, Piece.CORNER, Piece.HORIZONTAL, Piece.VERTICAL, Piece.INNER);
        l[ConnectedTextures.index(1, 0, 1)] = true;
        expect(helper, ConnectedTextures.compute(l), Direction.UP, Piece.CORNER, Piece.HORIZONTAL, Piece.VERTICAL, Piece.CENTER);

        // A neighbour with a part in front of it (a step up) does not connect on that face
        boolean[] step = new boolean[27];
        step[ConnectedTextures.index(1, 0, 0)] = true;
        step[ConnectedTextures.index(1, 1, 0)] = true;
        expect(helper, ConnectedTextures.compute(step), Direction.UP, Piece.CORNER, Piece.CORNER, Piece.CORNER, Piece.CORNER);
        helper.succeed();
    }

    /** The 3x3x3 plant from LiquefactionPlantTests, read from the level the way the model reads it. */
    @GameTest(template = "empty_3x3x3")
    public static void plantWallConnects(GameTestHelper helper) {
        BlockPos interior = new BlockPos(1, 1, 1);
        for (BlockPos pos : BlockPos.betweenClosed(BlockPos.ZERO, new BlockPos(2, 2, 2))) {
            if (!pos.equals(interior)) {
                helper.setBlock(pos, CryoBlocks.CRYO_CASING.get());
            }
        }
        helper.setBlock(new BlockPos(1, 1, 0), CryoBlocks.LIQUEFACTION_CONTROLLER.get());
        Set<Block> parts = Set.of(CryoBlocks.CRYO_CASING.get(), CryoBlocks.CRYO_VALVE.get(), CryoBlocks.CRYOCOOLER.get(),
              CryoBlocks.HEAT_REJECTION_PORT.get(), CryoBlocks.LIQUEFACTION_CONTROLLER.get());

        // Top middle of the north wall. Its north face: the top edge borders the outline, the rest joins the wall and the
        // controller below. Its top face: the north edge is the outline.
        Connections top = connections(helper, new BlockPos(1, 2, 0), parts);
        expect(helper, top, Direction.NORTH, Piece.HORIZONTAL, Piece.HORIZONTAL, Piece.CENTER, Piece.CENTER);
        expect(helper, top, Direction.UP, Piece.HORIZONTAL, Piece.HORIZONTAL, Piece.CENTER, Piece.CENTER);
        // A corner of the plant: bordered on the two outline sides of each outer face
        Connections corner = connections(helper, new BlockPos(0, 2, 0), parts);
        // North face, texture right = west: the corner block is on the right, so its outline is the top and right edges
        expect(helper, corner, Direction.NORTH, Piece.HORIZONTAL, Piece.CORNER, Piece.CENTER, Piece.VERTICAL);
        // The ceiling seen from the one block hollow: its neighbours have the walls in front of them, so it is framed alone
        expect(helper, connections(helper, new BlockPos(1, 2, 1), parts), Direction.DOWN, Piece.CORNER, Piece.CORNER, Piece.CORNER, Piece.CORNER);
        // Without the plant parts list nothing connects
        helper.assertTrue(connections(helper, new BlockPos(1, 2, 0), Set.of()).equals(Connections.ISOLATED), "nothing should connect");
        helper.succeed();
    }

    private static Connections connections(GameTestHelper helper, BlockPos relative, Set<Block> parts) {
        return ConnectedTextures.compute(helper.getLevel(), helper.absolutePos(relative), (BlockState s) -> parts.contains(s.getBlock()));
    }

    private static void expect(GameTestHelper helper, Connections c, Direction face, Piece tl, Piece tr, Piece bl, Piece br) {
        Piece[] expected = {tl, tr, bl, br};
        for (int quarter = 0; quarter < expected.length; quarter++) {
            helper.assertTrue(c.piece(face, quarter) == expected[quarter],
                  face + " quarter " + quarter + " is " + c.piece(face, quarter) + ", expected " + expected[quarter] + " in " + c);
        }
    }
}
