package io.github.valajar.cryosat.gametest;

import io.github.valajar.cryosat.CryoSat;
import io.github.valajar.cryosat.atmosphere.Atmosphere;
import io.github.valajar.cryosat.atmosphere.AtmosphereManager;
import io.github.valajar.cryosat.machine.MachineStatus;
import io.github.valajar.cryosat.machine.compressor.AtmosphericCompressorBlockEntity;
import io.github.valajar.cryosat.registries.CryoBlocks;
import io.github.valajar.cryosat.registries.CryoChemicals;
import mekanism.api.Action;
import mekanism.api.MekanismAPI;
import mekanism.api.RelativeSide;
import mekanism.api.Upgrade;
import mekanism.api.chemical.ChemicalStack;
import mekanism.api.chemical.IChemicalHandler;
import mekanism.api.energy.IStrictEnergyHandler;
import mekanism.common.block.attribute.Attribute;
import mekanism.common.capabilities.Capabilities;
import mekanism.common.lib.transmitter.TransmissionType;
import mekanism.common.tile.component.config.ConfigInfo;
import mekanism.common.tile.component.config.DataType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.jetbrains.annotations.Nullable;

/**
 * The compressor is placed facing north, so its back (energy) is south and its output is up.
 */
@GameTestHolder(CryoSat.MODID)
@PrefixGameTestTemplate(false)
public class AtmosphericCompressorTests {
    private static final BlockPos CENTER = new BlockPos(1, 1, 1);
    private static final String ARENA = "empty_3x3x3";

    private static AtmosphericCompressorBlockEntity place(GameTestHelper helper, BlockPos pos) {
        helper.setBlock(pos, CryoBlocks.ATMOSPHERIC_COMPRESSOR.get().defaultBlockState());
        return helper.getBlockEntity(pos);
    }

    @Nullable
    private static IStrictEnergyHandler energyFrom(GameTestHelper helper, BlockPos pos, Direction side) {
        return helper.getLevel().getCapability(Capabilities.STRICT_ENERGY.block(), helper.absolutePos(pos), side);
    }

    @Nullable
    private static IChemicalHandler chemicalsFrom(GameTestHelper helper, BlockPos pos, Direction side) {
        return helper.getLevel().getCapability(Capabilities.CHEMICAL.block(), helper.absolutePos(pos), side);
    }

    private static void power(GameTestHelper helper, BlockPos pos) {
        IStrictEnergyHandler input = energyFrom(helper, pos, Direction.SOUTH);
        helper.assertTrue(input != null, "Compressor exposes no energy input on its back");
        input.insertEnergy(20_000, Action.EXECUTE);
    }

    /** Keeps the buffer topped up, for upgrade setups that burn through it within a few ticks. */
    private static void keepPowered(GameTestHelper helper, AtmosphericCompressorBlockEntity compressor) {
        helper.onEachTick(() -> compressor.getEnergyContainer().setEnergy(compressor.getEnergyContainer().getMaxEnergy()));
    }

    private static boolean isActive(GameTestHelper helper, BlockPos pos) {
        return Attribute.isActive(helper.getBlockState(pos));
    }

    @GameTest(template = ARENA)
    public static void producesCompressedAirWhenPowered(GameTestHelper helper) {
        place(helper, CENTER);
        power(helper, CENTER);
        helper.succeedWhen(() -> {
            IChemicalHandler output = chemicalsFrom(helper, CENTER, Direction.UP);
            helper.assertTrue(output != null, "Compressor exposes no chemical output on top");
            ChemicalStack stored = output.getChemicalInTank(0);
            helper.assertTrue(stored.is(CryoChemicals.COMPRESSED_AIR), "Expected compressed air, got " + stored);
            helper.assertTrue(stored.getAmount() >= 100, "Only " + stored.getAmount() + " mB produced so far");
            helper.assertTrue(isActive(helper, CENTER), "Compressor is not in its active state");
        });
    }

    @GameTest(template = ARENA)
    public static void idleWithoutPower(GameTestHelper helper) {
        AtmosphericCompressorBlockEntity compressor = place(helper, CENTER);
        helper.runAfterDelay(30, () -> {
            helper.assertTrue(compressor.getStatus() == MachineStatus.NO_POWER, "Status was " + compressor.getStatus());
            helper.assertTrue(compressor.getOutputTank().isEmpty(), "Produced chemicals without power");
            helper.assertFalse(isActive(helper, CENTER), "Compressor is active without power");
            helper.succeed();
        });
    }

    @GameTest(template = ARENA)
    public static void stallsWhenEnclosed(GameTestHelper helper) {
        for (BlockPos pos : BlockPos.betweenClosed(BlockPos.ZERO, new BlockPos(2, 2, 2))) {
            if (!pos.equals(CENTER)) {
                helper.setBlock(pos, Blocks.STONE);
            }
        }
        AtmosphericCompressorBlockEntity compressor = place(helper, CENTER);
        power(helper, CENTER);
        helper.runAfterDelay(30, () -> {
            helper.assertTrue(compressor.getStatus() == MachineStatus.NO_AIR, "Status was " + compressor.getStatus());
            helper.assertTrue(compressor.getOutputTank().isEmpty(), "Produced chemicals while sealed in");
            helper.succeed();
        });
    }

    @GameTest(template = ARENA)
    public static void exposesOnlyConfiguredSides(GameTestHelper helper) {
        place(helper, CENTER);
        helper.assertTrue(energyFrom(helper, CENTER, Direction.SOUTH) != null, "No energy input on the back");
        helper.assertTrue(energyFrom(helper, CENTER, Direction.DOWN) != null, "No energy input on the bottom");
        helper.assertTrue(energyFrom(helper, CENTER, Direction.NORTH) == null, "Energy input on the front");
        helper.assertTrue(chemicalsFrom(helper, CENTER, Direction.UP) != null, "No chemical output on top");
        for (Direction side : new Direction[]{Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST, Direction.DOWN}) {
            helper.assertTrue(chemicalsFrom(helper, CENTER, side) == null, "Chemical port on " + side);
        }
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void sideConfigMovesChemicalOutput(GameTestHelper helper) {
        AtmosphericCompressorBlockEntity compressor = place(helper, CENTER);
        ConfigInfo chemical = compressor.getConfig().getConfig(TransmissionType.CHEMICAL);
        helper.assertTrue(chemical != null && chemical.isEjecting(), "Chemical output does not auto eject by default");
        // What the Configurator and the side config tab do: change the data type of a side, then notify the config
        chemical.setDataType(DataType.NONE, RelativeSide.TOP);
        compressor.getConfig().sideChanged(TransmissionType.CHEMICAL, RelativeSide.TOP);
        chemical.setDataType(DataType.OUTPUT, RelativeSide.FRONT);
        compressor.getConfig().sideChanged(TransmissionType.CHEMICAL, RelativeSide.FRONT);
        helper.assertTrue(chemicalsFrom(helper, CENTER, Direction.UP) == null, "Chemical output still exposed on top after disabling it");
        helper.assertTrue(chemicalsFrom(helper, CENTER, Direction.NORTH) != null, "No chemical output on the front after enabling it");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void acceptsForgeEnergy(GameTestHelper helper) {
        AtmosphericCompressorBlockEntity compressor = place(helper, CENTER);
        IEnergyStorage fe = helper.getLevel().getCapability(Capabilities.ENERGY.block(), helper.absolutePos(CENTER), Direction.DOWN);
        helper.assertTrue(fe != null, "No FE capability on the bottom");
        int accepted = fe.receiveEnergy(1_000, false);
        helper.assertTrue(accepted > 0, "FE was refused");
        helper.assertTrue(compressor.getEnergyContainer().getEnergy() > 0, "FE did not reach the Joule buffer");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void energyUpgradesLowerEnergyUse(GameTestHelper helper) {
        AtmosphericCompressorBlockEntity compressor = place(helper, CENTER);
        keepPowered(helper, compressor);
        helper.runAfterDelay(5, () -> {
            long baseUse = compressor.getLastEnergyUsed();
            double basePerMb = compressor.getEnergyPerMb();
            double baseRate = compressor.getCurrentRate();
            helper.assertTrue(baseUse > 0 && baseRate > 0, "Compressor is not running: " + compressor.getStatus());
            int added = compressor.getComponent().addUpgrades(Upgrade.ENERGY, Upgrade.ENERGY.getMax());
            helper.assertTrue(added == Upgrade.ENERGY.getMax(), "Only " + added + " energy upgrades installed");
            helper.runAfterDelay(5, () -> {
                helper.assertTrue(compressor.getLastEnergyUsed() > 0 && compressor.getLastEnergyUsed() < baseUse,
                      "Energy per tick went from " + baseUse + " to " + compressor.getLastEnergyUsed() + " J");
                helper.assertTrue(compressor.getEnergyPerMb() < basePerMb, "Energy per mB went from " + basePerMb + " to " + compressor.getEnergyPerMb());
                helper.assertTrue(compressor.getCurrentRate() == baseRate, "Energy upgrades changed the output rate");
                helper.succeed();
            });
        });
    }

    @GameTest(template = ARENA)
    public static void speedUpgradesRaiseOutput(GameTestHelper helper) {
        AtmosphericCompressorBlockEntity compressor = place(helper, CENTER);
        keepPowered(helper, compressor);
        helper.runAfterDelay(5, () -> {
            double baseRate = compressor.getCurrentRate();
            helper.assertTrue(baseRate > 0, "Compressor is not running: " + compressor.getStatus());
            compressor.getComponent().addUpgrades(Upgrade.SPEED, 4);
            helper.runAfterDelay(5, () -> {
                helper.assertTrue(compressor.getCurrentRate() > 1.5 * baseRate, "Output went from " + baseRate + " to " + compressor.getCurrentRate() + " mB/t");
                helper.succeed();
            });
        });
    }

    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void pushesIntoMekanismTube(GameTestHelper helper) {
        // A tube rather than a tank: Mekanism tanks only get their side config when a player places them from an item
        BlockPos compressorPos = new BlockPos(1, 0, 1);
        BlockPos tubePos = new BlockPos(1, 1, 1);
        Block tube = BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath(MekanismAPI.MEKANISM_MODID, "basic_pressurized_tube"));
        helper.assertTrue(tube != Blocks.AIR, "Mekanism's basic pressurized tube is missing");
        place(helper, compressorPos);
        helper.setBlock(tubePos, tube);
        power(helper, compressorPos);
        helper.succeedWhen(() -> {
            IChemicalHandler tubeHandler = chemicalsFrom(helper, tubePos, Direction.DOWN);
            helper.assertTrue(tubeHandler != null, "Mekanism tube exposes no chemical handler");
            ChemicalStack stored = tubeHandler.getChemicalInTank(0);
            helper.assertTrue(stored.is(CryoChemicals.COMPRESSED_AIR) && stored.getAmount() > 0, "Tube holds " + stored);
        });
    }

    @GameTest(template = "empty")
    public static void defaultAtmospheresLoaded(GameTestHelper helper) {
        Atmosphere overworld = AtmosphereManager.INSTANCE.get(Level.OVERWORLD);
        helper.assertTrue(overworld != null && overworld.chemical().is(CryoChemicals.COMPRESSED_AIR.getKey()), "Overworld atmosphere: " + overworld);
        Atmosphere nether = AtmosphereManager.INSTANCE.get(Level.NETHER);
        helper.assertTrue(nether != null && nether.chemical().is(ResourceLocation.fromNamespaceAndPath(MekanismAPI.MEKANISM_MODID, "sulfur_dioxide")),
              "Nether atmosphere: " + nether);
        helper.assertTrue(AtmosphereManager.INSTANCE.get(Level.END) == null, "The End should have no atmosphere");
        helper.succeed();
    }
}
