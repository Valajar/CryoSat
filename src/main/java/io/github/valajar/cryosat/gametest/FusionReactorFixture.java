package io.github.valajar.cryosat.gametest;

import mekanism.generators.common.content.fusion.FusionReactorMultiblockData;
import mekanism.generators.common.registries.GeneratorsBlocks;
import mekanism.generators.common.registries.GeneratorsChemicals;
import mekanism.generators.common.tile.fusion.TileEntityFusionReactorController;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;

/**
 * Builds and drives a real Mekanism Generators fusion reactor for {@link FusionFieldCoilTests}. All Generators types used by
 * the tests stay in here, so the test holder's own method signatures (which the game test scanner reflects on) never name
 * an optional class.
 */
final class FusionReactorFixture {
    /** Edge of Mekanism's fusion reactor: always 5x5x5. */
    static final int SIZE = 5;

    /**
     * Frame on every shell block the reactor needs (each face is a diamond, so the cube's edges and corners stay empty), and
     * optionally its controller in the middle of the top face, where Mekanism requires it.
     */
    static void build(GameTestHelper helper, BlockPos min, boolean withController) {
        for (int dx = 0; dx < SIZE; dx++) {
            for (int dy = 0; dy < SIZE; dy++) {
                for (int dz = 0; dz < SIZE; dz++) {
                    boolean shell = dx == 0 || dx == SIZE - 1 || dy == 0 || dy == SIZE - 1 || dz == 0 || dz == SIZE - 1;
                    if (shell && Math.abs(dx - 2) + Math.abs(dy - 2) + Math.abs(dz - 2) <= 4) {
                        helper.setBlock(min.offset(dx, dy, dz), GeneratorsBlocks.FUSION_REACTOR_FRAME.get());
                    }
                }
            }
        }
        if (withController) {
            helper.setBlock(controller(min), GeneratorsBlocks.FUSION_REACTOR_CONTROLLER.get());
        }
    }

    private static BlockPos controller(BlockPos min) {
        return min.offset(2, SIZE - 1, 2);
    }

    private static FusionReactorMultiblockData reactor(GameTestHelper helper, BlockPos min) {
        TileEntityFusionReactorController controller = helper.getBlockEntity(controller(min));
        return controller.getMultiblock();
    }

    static boolean isFormed(GameTestHelper helper, BlockPos min) {
        return reactor(helper, min).isFormed();
    }

    static boolean isBurning(GameTestHelper helper, BlockPos min) {
        return reactor(helper, min).isBurning();
    }

    static long storedEnergy(GameTestHelper helper, BlockPos min) {
        return reactor(helper, min).energyContainer.getEnergy();
    }

    /**
     * Fills the deuterium and tritium tanks and sets the plasma well above ignition, as a laser and hohlraum would. From then
     * on the reactor injects and burns fuel through Generators' own tick.
     */
    static void ignite(GameTestHelper helper, BlockPos min) {
        FusionReactorMultiblockData reactor = reactor(helper, min);
        reactor.deuteriumTank.setStack(GeneratorsChemicals.DEUTERIUM.asStack(reactor.deuteriumTank.getCapacity()));
        reactor.tritiumTank.setStack(GeneratorsChemicals.TRITIUM.asStack(reactor.tritiumTank.getCapacity()));
        reactor.fuelTank.setStack(GeneratorsChemicals.FUSION_FUEL.asStack(100));
        reactor.setPlasmaTemp(500_000_000);
        reactor.setLastPlasmaTemp(500_000_000);
        reactor.setBurning(true);
    }

    private FusionReactorFixture() {
    }
}
