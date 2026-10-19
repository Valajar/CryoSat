package io.github.valajar.cryosat.gametest;

import io.github.valajar.cryosat.CryoSat;
import io.github.valajar.cryosat.registries.CryoBlocks;
import io.github.valajar.cryosat.registries.CryoChemicals;
import io.github.valajar.cryosat.superconductor.Cryostat;
import io.github.valajar.cryosat.superconductor.CryostatState;
import io.github.valajar.cryosat.superconductor.squid.TileEntitySquidArray;
import java.util.List;
import mekanism.api.chemical.ChemicalStack;
import mekanism.common.attachments.containers.chemical.AttachedChemicals;
import mekanism.common.block.attribute.Attribute;
import mekanism.common.registries.MekanismDataComponents;
import mekanism.common.tile.interfaces.IRedstoneControl.RedstoneControl;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * The SQUID Array as an {@code IDeepScanModule}: cools down, then each deep scan drains exactly
 * {@link TileEntitySquidArray#COOLANT_PER_SCAN}, refused while cooling down, quenched or switched off by redstone. Scans run
 * inside one game tick, so the trickle never runs between them. The 30 second cool down is skipped with
 * {@link Cryostat#setCooldown} once it has been seen.
 */
@GameTestHolder(CryoSat.MODID)
@PrefixGameTestTemplate(false)
public class SquidArrayTests {
    private static final String ARENA = "empty_3x3x3";
    private static final BlockPos ARRAY = new BlockPos(1, 1, 1);

    private static TileEntitySquidArray place(GameTestHelper helper) {
        helper.setBlock(ARRAY, CryoBlocks.SQUID_ARRAY.get().defaultBlockState());
        return helper.getBlockEntity(ARRAY);
    }

    private static void fill(TileEntitySquidArray array, long coolant) {
        array.getCryostat().coolantTank.setStack(new ChemicalStack(CryoChemicals.LIQUID_NITROGEN, coolant));
    }

    /** Runs one deep scan and checks it drained exactly one scan's coolant. */
    private static void scan(GameTestHelper helper, TileEntitySquidArray array) {
        Cryostat cryostat = array.getCryostat();
        long before = cryostat.coolantTank.getStored();
        helper.assertTrue(array.canDeepScan(), "cannot deep scan with " + before + " mB, " + cryostat.getState());
        helper.assertTrue(array.useDeepScan(), "deep scan refused with " + before + " mB");
        long used = before - cryostat.coolantTank.getStored();
        helper.assertTrue(used == TileEntitySquidArray.COOLANT_PER_SCAN, "one deep scan used " + used + " mB");
    }

    private static void assertRefused(GameTestHelper helper, TileEntitySquidArray array, String why) {
        long before = array.getCryostat().coolantTank.getStored();
        int scans = array.getScansDone();
        helper.assertFalse(array.canDeepScan(), "offers deep scans " + why);
        helper.assertFalse(array.useDeepScan(), "ran a deep scan " + why);
        helper.assertTrue(array.getCryostat().coolantTank.getStored() == before && array.getScansDone() == scans, "used coolant " + why);
    }

    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void coolsDownThenScans(GameTestHelper helper) {
        TileEntitySquidArray array = place(helper);
        Cryostat cryostat = array.getCryostat();
        fill(array, Cryostat.COOLANT_CAPACITY);
        helper.startSequence()
              .thenWaitUntil(() -> helper.assertTrue(cryostat.getState() == CryostatState.COOLING_DOWN, "not cooling down: " + cryostat.getState()))
              .thenExecute(() -> {
                  assertRefused(helper, array, "while cooling down");
                  helper.assertFalse(Attribute.isActive(helper.getBlockState(ARRAY)), "active while cooling down");
                  cryostat.setCooldown(0);
              })
              .thenWaitUntil(() -> helper.assertTrue(cryostat.isSuperconducting() && Attribute.isActive(helper.getBlockState(ARRAY)),
                    "not superconducting and active: " + cryostat.getState()))
              .thenExecute(() -> {
                  helper.assertFalse(array.isTerminalAdjacent(), "sees an Imaging Terminal that is not there");
                  long before = cryostat.coolantTank.getStored();
                  long boilOffBefore = cryostat.boilOffTank.getStored();
                  scan(helper, array);
                  scan(helper, array);
                  scan(helper, array);
                  helper.assertTrue(array.getScansDone() == 3, "scans done: " + array.getScansDone());
                  long used = before - cryostat.coolantTank.getStored();
                  long boilOff = cryostat.boilOffTank.getStored() - boilOffBefore;
                  helper.assertTrue(cryostat.boilOffTank.getStack().is(CryoChemicals.NITROGEN) && boilOff >= (long) (used * Cryostat.BOIL_OFF_RETURN) - 1,
                        "boil off after " + used + " mB: " + boilOff + " mB");
              })
              .thenIdle(20)
              .thenExecute(() -> helper.assertTrue(cryostat.getLastUsed() == TileEntitySquidArray.TRICKLE, "idle coolant use: " + cryostat.getLastUsed()))
              .thenSucceed();
    }

    @GameTest(template = ARENA)
    public static void dryArrayRefuses(GameTestHelper helper) {
        TileEntitySquidArray array = place(helper);
        helper.runAfterDelay(2, () -> {
            helper.assertTrue(array.getCryostat().getState() == CryostatState.NO_COOLANT, "dry array is " + array.getCryostat().getState());
            assertRefused(helper, array, "without coolant");
            helper.succeed();
        });
    }

    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void runsDryAndQuenches(GameTestHelper helper) {
        TileEntitySquidArray array = place(helper);
        Cryostat cryostat = array.getCryostat();
        // Enough for one scan and a little trickle, not for a second scan
        fill(array, TileEntitySquidArray.COOLANT_PER_SCAN + 40);
        cryostat.setCooldown(0);
        helper.startSequence()
              .thenWaitUntil(() -> helper.assertTrue(cryostat.isSuperconducting(), "not superconducting: " + cryostat.getState()))
              .thenExecute(() -> {
                  scan(helper, array);
                  assertRefused(helper, array, "without coolant for a scan");
              })
              .thenWaitUntil(() -> helper.assertTrue(cryostat.getState() == CryostatState.NO_COOLANT, "did not quench: " + cryostat.getState()))
              .thenExecute(() -> {
                  assertRefused(helper, array, "while quenched");
                  fill(array, Cryostat.COOLANT_CAPACITY);
              })
              .thenIdle(2)
              .thenExecute(() -> {
                  helper.assertTrue(cryostat.getState() == CryostatState.COOLING_DOWN, "refilled array is " + cryostat.getState());
                  assertRefused(helper, array, "while cooling down after a quench");
              })
              .thenSucceed();
    }

    @GameTest(template = ARENA)
    public static void redstoneControlRefusesScans(GameTestHelper helper) {
        TileEntitySquidArray array = place(helper);
        Cryostat cryostat = array.getCryostat();
        fill(array, Cryostat.COOLANT_CAPACITY);
        cryostat.setCooldown(0);
        helper.startSequence()
              .thenWaitUntil(() -> helper.assertTrue(cryostat.isSuperconducting(), "not superconducting: " + cryostat.getState()))
              .thenExecute(() -> {
                  array.setControlType(RedstoneControl.HIGH);
                  assertRefused(helper, array, "while switched off by redstone");
              })
              .thenIdle(5)
              .thenExecute(() -> {
                  // Kept cold while switched off, so switching back on needs no new cool down
                  helper.assertTrue(cryostat.isSuperconducting(), "warmed up while switched off: " + cryostat.getState());
                  array.setControlType(RedstoneControl.DISABLED);
                  scan(helper, array);
              })
              .thenSucceed();
    }

    @GameTest(template = ARENA)
    public static void keepsCoolantWhenBroken(GameTestHelper helper) {
        TileEntitySquidArray array = place(helper);
        array.getCryostat().coolantTank.setStack(new ChemicalStack(CryoChemicals.LIQUID_NITROGEN, 3_000));
        array.getCryostat().boilOffTank.setStack(new ChemicalStack(CryoChemicals.NITROGEN, 500));
        BlockPos pos = helper.absolutePos(ARRAY);
        List<ItemStack> drops = Block.getDrops(helper.getBlockState(ARRAY), helper.getLevel(), pos, array);
        helper.assertTrue(drops.size() == 1 && drops.getFirst().is(CryoBlocks.SQUID_ARRAY.asItem()), "drops: " + drops);
        AttachedChemicals chemicals = drops.getFirst().get(MekanismDataComponents.ATTACHED_CHEMICALS);
        helper.assertTrue(chemicals != null && chemicals.containers().size() == 2, "dropped item has no tanks: " + chemicals);
        helper.assertTrue(chemicals.containers().get(0).is(CryoChemicals.LIQUID_NITROGEN) && chemicals.containers().get(0).getAmount() == 3_000,
              "coolant on the item: " + chemicals.containers().get(0));
        helper.assertTrue(chemicals.containers().get(1).is(CryoChemicals.NITROGEN) && chemicals.containers().get(1).getAmount() == 500,
              "boil off on the item: " + chemicals.containers().get(1));
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void squidArrayRecipeLoads(GameTestHelper helper) {
        helper.assertTrue(helper.getLevel().getRecipeManager().byKey(CryoSat.rl("squid_array")).isPresent(), "squid_array recipe missing");
        helper.succeed();
    }
}
