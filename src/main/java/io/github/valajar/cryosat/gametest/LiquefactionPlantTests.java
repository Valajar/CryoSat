package io.github.valajar.cryosat.gametest;

import io.github.valajar.cryosat.CryoSat;
import io.github.valajar.cryosat.multiblock.liquefaction.LiquefactionMultiblockData;
import io.github.valajar.cryosat.multiblock.liquefaction.PlantStatus;
import io.github.valajar.cryosat.multiblock.liquefaction.ValveMode;
import io.github.valajar.cryosat.registries.CryoChemicals;
import io.github.valajar.cryosat.registries.CryoBlocks;
import io.github.valajar.cryosat.tile.liquefaction.TileEntityCryoValve;
import io.github.valajar.cryosat.tile.liquefaction.TileEntityLiquefactionController;
import mekanism.api.Action;
import mekanism.api.chemical.ChemicalStack;
import mekanism.api.chemical.IChemicalHandler;
import mekanism.api.energy.IStrictEnergyHandler;
import mekanism.common.registries.MekanismChemicals;
import mekanism.common.capabilities.Capabilities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Builds the smallest plant (3x3x3) filling the test arena: controller on the north wall, cryocooler on the south wall, valve
 * on the west wall, heat rejection port on the east wall, casing everywhere else.
 */
@GameTestHolder(CryoSat.MODID)
@PrefixGameTestTemplate(false)
public class LiquefactionPlantTests {
    private static final String ARENA = "empty_3x3x3";
    private static final BlockPos CONTROLLER = new BlockPos(1, 1, 0);
    private static final BlockPos COOLER = new BlockPos(1, 1, 2);
    private static final BlockPos VALVE = new BlockPos(0, 1, 1);
    private static final BlockPos PORT = new BlockPos(2, 1, 1);
    private static final BlockPos INTERIOR = new BlockPos(1, 1, 1);

    private static void build(GameTestHelper helper) {
        for (BlockPos pos : BlockPos.betweenClosed(BlockPos.ZERO, new BlockPos(2, 2, 2))) {
            if (!pos.equals(INTERIOR)) {
                helper.setBlock(pos, CryoBlocks.CRYO_CASING.get());
            }
        }
        place(helper, CONTROLLER, CryoBlocks.LIQUEFACTION_CONTROLLER.get());
        place(helper, COOLER, CryoBlocks.CRYOCOOLER.get());
        place(helper, VALVE, CryoBlocks.CRYO_VALVE.get());
        place(helper, PORT, CryoBlocks.HEAT_REJECTION_PORT.get());
    }

    private static void place(GameTestHelper helper, BlockPos pos, Block block) {
        helper.setBlock(pos, block);
    }

    private static LiquefactionMultiblockData plant(GameTestHelper helper) {
        TileEntityLiquefactionController controller = helper.getBlockEntity(CONTROLLER);
        return controller.getMultiblock();
    }

    private static void powerCooler(GameTestHelper helper) {
        LiquefactionMultiblockData plant = plant(helper);
        plant.energyContainer.setEnergy(plant.energyContainer.getMaxEnergy());
    }

    @GameTest(template = ARENA, timeoutTicks = 100)
    public static void formsWithAllParts(GameTestHelper helper) {
        build(helper);
        helper.succeedWhen(() -> {
            LiquefactionMultiblockData plant = plant(helper);
            helper.assertTrue(plant.isFormed(), "plant did not form");
            helper.assertTrue(plant.coolers == 1, "expected 1 cryocooler, counted " + plant.coolers);
            helper.assertTrue(plant.heatPorts == 1, "expected 1 heat rejection port, counted " + plant.heatPorts);
            helper.assertTrue(plant.getInteriorVolume() == 1, "interior volume " + plant.getInteriorVolume());
        });
    }

    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void coolerCoolsAndHeatsHotSide(GameTestHelper helper) {
        build(helper);
        helper.runAfterDelay(20, () -> {
            LiquefactionMultiblockData plant = plant(helper);
            helper.assertTrue(plant.isFormed(), "plant did not form");
            double start = plant.coldTemperature;
            double hotStart = plant.getHotTemperature();
            powerCooler(helper);
            helper.runAfterDelay(40, () -> {
                helper.assertTrue(plant.coldTemperature < start - 1, "interior did not cool: " + start + " -> " + plant.coldTemperature);
                helper.assertTrue(plant.getHotTemperature() > hotStart, "hot side did not warm: " + hotStart + " -> " + plant.getHotTemperature());
                helper.assertTrue(plant.lastEnergyUsed > 0, "cooler used no energy");
                helper.succeed();
            });
        });
    }

    @GameTest(template = ARENA, timeoutTicks = 100)
    public static void unpoweredPlantWarmsTowardsAmbient(GameTestHelper helper) {
        build(helper);
        helper.runAfterDelay(20, () -> {
            LiquefactionMultiblockData plant = plant(helper);
            helper.assertTrue(plant.isFormed(), "plant did not form");
            plant.coldTemperature = 100;
            helper.runAfterDelay(20, () -> {
                helper.assertTrue(plant.coldTemperature > 100, "interior did not warm up: " + plant.coldTemperature);
                helper.succeed();
            });
        });
    }

    @GameTest(template = ARENA, timeoutTicks = 100)
    public static void liquefiesAirWhenColdEnough(GameTestHelper helper) {
        build(helper);
        helper.runAfterDelay(20, () -> {
            LiquefactionMultiblockData plant = plant(helper);
            helper.assertTrue(plant.isFormed(), "plant did not form");
            IChemicalHandler valve = helper.getLevel().getCapability(Capabilities.CHEMICAL.block(), helper.absolutePos(VALVE), Direction.WEST);
            helper.assertTrue(valve != null, "valve exposes no chemical handler");
            ChemicalStack remainder = valve.insertChemical(new ChemicalStack(CryoChemicals.COMPRESSED_AIR, 1_000), Action.EXECUTE);
            helper.assertTrue(remainder.isEmpty(), "valve refused compressed air: " + remainder);
            // Too warm: nothing happens
            helper.runAfterDelay(5, () -> {
                helper.assertTrue(plant.lastProcessed == 0, "liquefied while warm");
                helper.assertTrue(plant.status != PlantStatus.RUNNING, "status running while warm");
                plant.coldTemperature = 60;
                helper.runAfterDelay(60, () -> {
                    long n2 = plant.outputTank1.getStored(), o2 = plant.outputTank2.getStored(), ar = plant.outputTank3.getStored();
                    helper.assertTrue(plant.outputTank1.getStack().is(CryoChemicals.LIQUID_NITROGEN), "output 1 holds " + plant.outputTank1.getStack());
                    helper.assertTrue(plant.outputTank2.getStack().is(CryoChemicals.LIQUID_OXYGEN), "output 2 holds " + plant.outputTank2.getStack());
                    helper.assertTrue(n2 >= 770 && n2 <= 780, "nitrogen " + n2);
                    helper.assertTrue(o2 >= 205 && o2 <= 210, "oxygen " + o2);
                    helper.assertTrue(ar >= 9 && ar <= 10, "argon " + ar);
                    helper.succeed();
                });
            });
        });
    }

    @GameTest(template = ARENA, timeoutTicks = 100)
    public static void valveModeSelectsOutputTank(GameTestHelper helper) {
        build(helper);
        helper.runAfterDelay(20, () -> {
            LiquefactionMultiblockData plant = plant(helper);
            helper.assertTrue(plant.isFormed(), "plant did not form");
            plant.outputTank2.setStack(new ChemicalStack(CryoChemicals.LIQUID_OXYGEN, 500));
            TileEntityCryoValve valve = helper.getBlockEntity(VALVE);
            valve.setMode(ValveMode.OUTPUT_2);
            IChemicalHandler handler = helper.getLevel().getCapability(Capabilities.CHEMICAL.block(), helper.absolutePos(VALVE), Direction.WEST);
            helper.assertTrue(handler != null, "valve exposes no chemical handler");
            ChemicalStack drained = handler.extractChemical(100, Action.EXECUTE);
            helper.assertTrue(drained.is(CryoChemicals.LIQUID_OXYGEN) && drained.getAmount() == 100, "drained " + drained);
            helper.succeed();
        });
    }

    @GameTest(template = ARENA, timeoutTicks = 100)
    public static void valvePowersTheWholePlant(GameTestHelper helper) {
        build(helper);
        helper.runAfterDelay(20, () -> {
            LiquefactionMultiblockData plant = plant(helper);
            helper.assertTrue(plant.isFormed(), "plant did not form");
            IStrictEnergyHandler valve = helper.getLevel().getCapability(Capabilities.STRICT_ENERGY.block(), helper.absolutePos(VALVE), Direction.WEST);
            helper.assertTrue(valve != null, "valve exposes no energy input");
            long accepted = 50_000 - valve.insertEnergy(50_000, Action.EXECUTE);
            helper.assertTrue(accepted > 0, "valve refused energy");
            double start = plant.coldTemperature;
            helper.runAfterDelay(20, () -> {
                helper.assertTrue(plant.coldTemperature < start - 0.5, "power through the valve did not run the cooler: " + start + " -> " + plant.coldTemperature);
                helper.succeed();
            });
        });
    }

    @GameTest(template = ARENA, timeoutTicks = 100)
    public static void hydrogenNeedsTwentyKelvin(GameTestHelper helper) {
        build(helper);
        helper.runAfterDelay(20, () -> {
            LiquefactionMultiblockData plant = plant(helper);
            helper.assertTrue(plant.isFormed(), "plant did not form");
            IChemicalHandler valve = helper.getLevel().getCapability(Capabilities.CHEMICAL.block(), helper.absolutePos(VALVE), Direction.WEST);
            ChemicalStack hydrogen = new ChemicalStack(MekanismChemicals.HYDROGEN, 1_000);
            helper.assertTrue(valve.insertChemical(hydrogen, Action.EXECUTE).isEmpty(), "valve refused hydrogen");
            plant.coldTemperature = 40;
            helper.runAfterDelay(5, () -> {
                helper.assertTrue(plant.targetTemperature == 20, "target temperature for hydrogen: " + plant.targetTemperature);
                helper.assertTrue(plant.status == PlantStatus.COOLING_DOWN || plant.status == PlantStatus.NO_COOLERS, "status at 40 K: " + plant.status);
                plant.coldTemperature = 10;
                helper.runAfterDelay(10, () -> {
                    helper.assertTrue(plant.outputTank1.getStack().is(CryoChemicals.LIQUID_HYDROGEN) && plant.outputTank1.getStored() > 0,
                          "no liquid hydrogen at 10 K: " + plant.outputTank1.getStack());
                    helper.succeed();
                });
            });
        });
    }

    @GameTest(template = ARENA, timeoutTicks = 100)
    public static void hydrogenAfterAirUsesAFreeTank(GameTestHelper helper) {
        build(helper);
        helper.runAfterDelay(20, () -> {
            LiquefactionMultiblockData plant = plant(helper);
            helper.assertTrue(plant.isFormed(), "plant did not form");
            // Leftovers from a compressed air run in the first output tank
            plant.outputTank1.setStack(new ChemicalStack(CryoChemicals.LIQUID_NITROGEN, 500));
            IChemicalHandler valve = helper.getLevel().getCapability(Capabilities.CHEMICAL.block(), helper.absolutePos(VALVE), Direction.WEST);
            helper.assertTrue(valve.insertChemical(new ChemicalStack(MekanismChemicals.HYDROGEN, 1_000), Action.EXECUTE).isEmpty(), "valve refused hydrogen");
            plant.coldTemperature = 10;
            helper.runAfterDelay(10, () -> {
                helper.assertTrue(plant.outputTank1.getStack().is(CryoChemicals.LIQUID_NITROGEN) && plant.outputTank1.getStored() == 500,
                      "nitrogen tank changed: " + plant.outputTank1.getStack());
                helper.assertTrue(plant.outputTank2.getStack().is(CryoChemicals.LIQUID_HYDROGEN) && plant.outputTank2.getStored() > 0,
                      "liquid hydrogen did not go to the free tank: " + plant.outputTank2.getStack() + ", status " + plant.status);
                helper.succeed();
            });
        });
    }
}
