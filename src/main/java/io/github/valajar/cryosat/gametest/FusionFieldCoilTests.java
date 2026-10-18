package io.github.valajar.cryosat.gametest;

import io.github.valajar.cryosat.CryoSat;
import io.github.valajar.cryosat.registries.CryoBlocks;
import io.github.valajar.cryosat.registries.CryoChemicals;
import io.github.valajar.cryosat.superconductor.Cryostat;
import io.github.valajar.cryosat.superconductor.CryostatState;
import io.github.valajar.cryosat.superconductor.fusion.FieldCoilRing;
import io.github.valajar.cryosat.superconductor.fusion.RingProblem;
import io.github.valajar.cryosat.superconductor.fusion.TileEntityFieldCoilController;
import mekanism.api.chemical.ChemicalStack;
import mekanism.common.block.attribute.Attribute;
import mekanism.common.registries.MekanismBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Fusion Field Coils around a real Mekanism Generators fusion reactor, built and ignited by {@link FusionReactorFixture}
 * (Generators is on the test runtime classpath).
 * <p>
 * The arena is 13x7x13 with the 5x5x5 reactor at x and z 4 to 8, y 1 to 5, so its middle layer is y 3. A gap 1 loop is the
 * 9x9 square at x and z 2 to 10, a gap 2 loop the 11x11 square at 1 to 11. The reactor is ignited by setting its plasma hot
 * and its fuel tanks full; from then on it burns on its own through Generators' normal tick.
 */
@GameTestHolder(CryoSat.MODID)
@PrefixGameTestTemplate(false)
public class FusionFieldCoilTests {
    private static final String ARENA = "empty_13x7x13";
    private static final BlockPos REACTOR_MIN = new BlockPos(4, 1, 4);
    private static final BlockPos REACTOR_MAX = new BlockPos(8, 5, 8);
    private static final int MIDDLE = 3;
    private static final int FORM_TICKS = 20;

    private static void buildReactor(GameTestHelper helper, boolean withController) {
        FusionReactorFixture.build(helper, REACTOR_MIN, withController);
    }

    /** A loop of coils at the given gap and layer; the controller (if any) replaces the coil in the middle of the north side. */
    private static void buildLoop(GameTestHelper helper, int gap, int y) {
        for (BlockPos pos : FieldCoilRing.square(REACTOR_MIN, REACTOR_MAX, gap + 1, y)) {
            helper.setBlock(pos, CryoBlocks.FIELD_COIL.get());
        }
    }

    private static BlockPos controllerPos(int gap, int y) {
        return new BlockPos(6, y, REACTOR_MIN.getZ() - gap - 1);
    }

    private static TileEntityFieldCoilController placeController(GameTestHelper helper, int gap, int y) {
        helper.setBlock(controllerPos(gap, y), CryoBlocks.FIELD_COIL_CONTROLLER.get());
        return helper.getBlockEntity(controllerPos(gap, y));
    }

    private static FieldCoilRing.Scan rescan(GameTestHelper helper, int gap, int y) {
        TileEntityFieldCoilController controller = helper.getBlockEntity(controllerPos(gap, y));
        controller.rescan();
        return controller.getScan();
    }

    private static void fillAndCool(TileEntityFieldCoilController controller) {
        Cryostat cryostat = controller.getCryostat();
        cryostat.coolantTank.setStack(new ChemicalStack(CryoChemicals.LIQUID_NITROGEN, Cryostat.COOLANT_CAPACITY));
        // Skip the 30 second cool down; the cryostat's own tests cover it
        cryostat.setCooldown(0);
    }

    @GameTest(template = ARENA, timeoutTicks = 100)
    public static void ringFormsAroundRealReactor(GameTestHelper helper) {
        buildReactor(helper, true);
        buildLoop(helper, 1, MIDDLE);
        placeController(helper, 1, MIDDLE);
        helper.runAfterDelay(FORM_TICKS, () -> {
            helper.assertTrue(FusionReactorFixture.isFormed(helper, REACTOR_MIN), "fusion reactor did not form");
            FieldCoilRing.Scan scan = rescan(helper, 1, MIDDLE);
            helper.assertTrue(scan.works(), "ring not working: " + scan);
            helper.assertTrue(scan.loops() == 1 && scan.gap() == 1, "expected 1 loop at gap 1: " + scan);
            helper.assertTrue(scan.coils().size() == 32, "a 9x9 loop has 32 blocks, counted " + scan.coils().size());
            helper.assertTrue(helper.absolutePos(REACTOR_MIN).equals(scan.reactorMin()), "wrong reactor bounds " + scan.reactorMin());
            helper.succeed();
        });
    }

    @GameTest(template = ARENA, timeoutTicks = 100)
    public static void stackedLoopsCountAndUnfinishedLoopIsReported(GameTestHelper helper) {
        buildReactor(helper, true);
        buildLoop(helper, 2, MIDDLE);
        buildLoop(helper, 2, MIDDLE + 1);
        // The lower loop is only half built
        BlockPos gapInLower = new BlockPos(1, MIDDLE - 1, 11);
        buildLoop(helper, 2, MIDDLE - 1);
        helper.setBlock(gapInLower, Blocks.AIR);
        placeController(helper, 2, MIDDLE + 1);
        helper.runAfterDelay(FORM_TICKS, () -> {
            FieldCoilRing.Scan scan = rescan(helper, 2, MIDDLE + 1);
            helper.assertTrue(scan.works() && scan.loops() == 2 && scan.gap() == 2, "expected 2 loops at gap 2: " + scan);
            helper.assertTrue(helper.absolutePos(gapInLower).equals(scan.incomplete()), "unfinished loop not reported: " + scan.incomplete());
            helper.setBlock(gapInLower, CryoBlocks.FIELD_COIL.get());
            FieldCoilRing.Scan full = rescan(helper, 2, MIDDLE + 1);
            helper.assertTrue(full.works() && full.loops() == 3 && full.coils().size() == 120, "expected 3 loops of 40: " + full);
            helper.succeed();
        });
    }

    @GameTest(template = ARENA, timeoutTicks = 100)
    public static void brokenLoopAndSecondControllerAreReported(GameTestHelper helper) {
        buildReactor(helper, true);
        buildLoop(helper, 1, MIDDLE);
        placeController(helper, 1, MIDDLE);
        BlockPos hole = new BlockPos(10, MIDDLE, 7);
        helper.setBlock(hole, Blocks.AIR);
        helper.runAfterDelay(FORM_TICKS, () -> {
            FieldCoilRing.Scan scan = rescan(helper, 1, MIDDLE);
            helper.assertTrue(scan.problem() == RingProblem.BROKEN_LOOP && helper.absolutePos(hole).equals(scan.at()),
                  "broken loop not reported at the hole: " + scan);
            helper.setBlock(hole, CryoBlocks.FIELD_COIL_CONTROLLER.get());
            FieldCoilRing.Scan second = rescan(helper, 1, MIDDLE);
            helper.assertTrue(second.problem() == RingProblem.SECOND_CONTROLLER && helper.absolutePos(hole).equals(second.at()),
                  "second controller not reported: " + second);
            helper.succeed();
        });
    }

    @GameTest(template = ARENA, timeoutTicks = 100)
    public static void wrongGapAndLayerAreReported(GameTestHelper helper) {
        buildReactor(helper, true);
        // Right against the reactor: no gap
        buildLoop(helper, 0, MIDDLE);
        placeController(helper, 0, MIDDLE);
        // A second controller level with the reactor's bottom layer, outside any loop
        BlockPos low = new BlockPos(6, REACTOR_MIN.getY(), 1);
        helper.setBlock(low, CryoBlocks.FIELD_COIL_CONTROLLER.get());
        helper.runAfterDelay(FORM_TICKS, () -> {
            FieldCoilRing.Scan scan = rescan(helper, 0, MIDDLE);
            helper.assertTrue(scan.problem() == RingProblem.WRONG_GAP && scan.gap() == 0, "gap 0 not reported: " + scan);
            TileEntityFieldCoilController lowController = helper.getBlockEntity(low);
            lowController.rescan();
            helper.assertTrue(lowController.getScan().problem() == RingProblem.WRONG_LAYER, "wrong layer not reported: " + lowController.getScan());
            helper.succeed();
        });
    }

    @GameTest(template = ARENA, timeoutTicks = 100)
    public static void gapMustBeFreeOfFullBlocks(GameTestHelper helper) {
        buildReactor(helper, true);
        buildLoop(helper, 2, MIDDLE);
        placeController(helper, 2, MIDDLE);
        // A tube through the gap is fine, a stone block is not
        helper.setBlock(new BlockPos(6, MIDDLE, 2), MekanismBlocks.BASIC_PRESSURIZED_TUBE.get());
        BlockPos stone = new BlockPos(9, MIDDLE, 10);
        helper.setBlock(stone, Blocks.STONE);
        helper.runAfterDelay(FORM_TICKS, () -> {
            FieldCoilRing.Scan scan = rescan(helper, 2, MIDDLE);
            helper.assertTrue(scan.problem() == RingProblem.GAP_BLOCKED && helper.absolutePos(stone).equals(scan.at()), "blocked gap not reported: " + scan);
            helper.setBlock(stone, Blocks.AIR);
            FieldCoilRing.Scan clear = rescan(helper, 2, MIDDLE);
            helper.assertTrue(clear.works(), "a tube in the gap broke the ring: " + clear);
            helper.succeed();
        });
    }

    @GameTest(template = ARENA, timeoutTicks = 100)
    public static void missingOrUnformedReactorIsReported(GameTestHelper helper) {
        buildLoop(helper, 1, MIDDLE);
        placeController(helper, 1, MIDDLE);
        helper.runAfterDelay(5, () -> {
            helper.assertTrue(rescan(helper, 1, MIDDLE).problem() == RingProblem.NO_REACTOR, "no reactor not reported");
            // Frame without its controller never forms
            buildReactor(helper, false);
            helper.runAfterDelay(FORM_TICKS, () -> {
                FieldCoilRing.Scan scan = rescan(helper, 1, MIDDLE);
                helper.assertTrue(scan.problem() == RingProblem.REACTOR_NOT_FORMED, "unformed reactor not reported: " + scan);
                helper.succeed();
            });
        });
    }

    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void coldRingBoostsBurningReactor(GameTestHelper helper) {
        buildReactor(helper, true);
        for (int y = MIDDLE - 1; y <= MIDDLE + 1; y++) {
            buildLoop(helper, 1, y);
        }
        TileEntityFieldCoilController controller = placeController(helper, 1, MIDDLE);
        helper.runAfterDelay(FORM_TICKS, () -> {
            helper.assertTrue(FusionReactorFixture.isFormed(helper, REACTOR_MIN), "fusion reactor did not form");
            controller.rescan();
            helper.assertTrue(controller.getLoops() == 3, "expected 3 loops: " + controller.getScan());
            fillAndCool(controller);
            helper.runAfterDelay(5, () -> {
                // Cold but the reactor is not burning: only the trickle, no bonus
                helper.assertTrue(controller.getCryostat().isSuperconducting(), "not superconducting: " + controller.getCryostat().getState());
                helper.assertTrue(controller.getLastBonus() == 0, "bonus from a cold reactor: " + controller.getLastBonus());
                helper.assertTrue(controller.getLastCoolant() == 3 * TileEntityFieldCoilController.TRICKLE_PER_LOOP,
                      "trickle for 3 loops should be 6 mB/t, used " + controller.getLastCoolant());
                BlockPos coil = new BlockPos(2, MIDDLE + 1, 2);
                helper.assertTrue(Attribute.isActive(helper.getBlockState(coil)), "coils did not frost over");
                FusionReactorFixture.ignite(helper, REACTOR_MIN);
                helper.runAfterDelay(20, () -> {
                    helper.assertTrue(FusionReactorFixture.isBurning(helper, REACTOR_MIN), "reactor stopped burning");
                    long passive = controller.getLastPassive();
                    long bonus = controller.getLastBonus();
                    helper.assertTrue(passive > 0, "no passive generation read from the reactor");
                    helper.assertTrue(bonus > 0 && bonus == TileEntityFieldCoilController.bonusFor(3, passive),
                          "bonus " + bonus + " is not 75 percent of " + passive);
                    long expectedCoolant = 6 + bonus / 100_000;
                    long used = controller.getLastCoolant();
                    helper.assertTrue(used >= expectedCoolant && used <= expectedCoolant + 1, "coolant " + used + ", expected about " + expectedCoolant);
                    helper.assertTrue(controller.getCryostat().boilOffTank.getStack().is(CryoChemicals.NITROGEN), "no nitrogen boil off");
                    // The bonus really lands in the reactor's output only buffer
                    long before = FusionReactorFixture.storedEnergy(helper, REACTOR_MIN);
                    helper.runAfterDelay(1, () -> {
                        helper.assertTrue(FusionReactorFixture.storedEnergy(helper, REACTOR_MIN) > before, "reactor energy did not grow");
                        helper.succeed();
                    });
                });
            });
        });
    }

    @GameTest(template = ARENA, timeoutTicks = 100)
    public static void dryRingGivesNoBonus(GameTestHelper helper) {
        buildReactor(helper, true);
        buildLoop(helper, 1, MIDDLE);
        TileEntityFieldCoilController controller = placeController(helper, 1, MIDDLE);
        helper.runAfterDelay(FORM_TICKS, () -> {
            controller.rescan();
            FusionReactorFixture.ignite(helper, REACTOR_MIN);
            helper.runAfterDelay(10, () -> {
                helper.assertTrue(FusionReactorFixture.isBurning(helper, REACTOR_MIN), "reactor not burning");
                helper.assertTrue(controller.getCryostat().getState() == CryostatState.NO_COOLANT, "dry ring is " + controller.getCryostat().getState());
                helper.assertTrue(controller.getLastBonus() == 0 && controller.getLastPassive() > 0, "dry ring boosted the reactor: " + controller.getLastBonus());
                helper.assertFalse(Attribute.isActive(helper.getBlockState(new BlockPos(2, MIDDLE, 2))), "dry coils are frosted");
                helper.succeed();
            });
        });
    }

    @GameTest(template = "empty")
    public static void bonusMaths(GameTestHelper helper) {
        helper.assertTrue(TileEntityFieldCoilController.bonusFor(1, 1_000_000) == 250_000, "1 loop is not +25 percent");
        helper.assertTrue(TileEntityFieldCoilController.bonusFor(3, 1_000_000) == 750_000, "3 loops are not +75 percent");
        helper.assertTrue(TileEntityFieldCoilController.bonusFor(5, 1_000_000) == 750_000, "more than 3 loops counted");
        helper.assertTrue(TileEntityFieldCoilController.bonusFor(0, 1_000_000) == 0, "no loops gave a bonus");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void fieldCoilRecipesLoad(GameTestHelper helper) {
        var recipes = helper.getLevel().getRecipeManager();
        helper.assertTrue(recipes.byKey(CryoSat.rl("field_coil")).isPresent(), "field coil recipe missing");
        helper.assertTrue(recipes.byKey(CryoSat.rl("field_coil_controller")).isPresent(), "field coil controller recipe missing");
        helper.succeed();
    }
}
