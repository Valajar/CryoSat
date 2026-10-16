package io.github.valajar.cryosat.gametest;

import io.github.valajar.cryosat.CryoSat;
import io.github.valajar.cryosat.atmosphere.Atmosphere;
import io.github.valajar.cryosat.atmosphere.AtmosphereManager;
import io.github.valajar.cryosat.capability.MekCapabilities;
import io.github.valajar.cryosat.machine.MachineBlock;
import io.github.valajar.cryosat.machine.MachineStatus;
import io.github.valajar.cryosat.machine.compressor.AtmosphericCompressorBlockEntity;
import io.github.valajar.cryosat.registries.CryoBlocks;
import io.github.valajar.cryosat.registries.CryoChemicals;
import mekanism.api.Action;
import mekanism.api.MekanismAPI;
import mekanism.api.chemical.ChemicalStack;
import mekanism.api.chemical.IChemicalHandler;
import mekanism.api.energy.IStrictEnergyHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.capabilities.Capabilities;
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
        return helper.getLevel().getCapability(MekCapabilities.STRICT_ENERGY, helper.absolutePos(pos), side);
    }

    @Nullable
    private static IChemicalHandler chemicalsFrom(GameTestHelper helper, BlockPos pos, Direction side) {
        return helper.getLevel().getCapability(MekCapabilities.CHEMICAL, helper.absolutePos(pos), side);
    }

    private static void power(GameTestHelper helper, BlockPos pos) {
        IStrictEnergyHandler input = energyFrom(helper, pos, Direction.SOUTH);
        helper.assertTrue(input != null, "Compressor exposes no energy input on its back");
        input.insertEnergy(20_000, Action.EXECUTE);
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
            helper.assertBlockProperty(CENTER, MachineBlock.ACTIVE, true);
        });
    }

    @GameTest(template = ARENA)
    public static void idleWithoutPower(GameTestHelper helper) {
        AtmosphericCompressorBlockEntity compressor = place(helper, CENTER);
        helper.runAfterDelay(30, () -> {
            helper.assertTrue(compressor.getStatus() == MachineStatus.NO_POWER, "Status was " + compressor.getStatus());
            helper.assertTrue(compressor.getOutput().isEmpty(), "Produced chemicals without power");
            helper.assertBlockProperty(CENTER, MachineBlock.ACTIVE, false);
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
            helper.assertTrue(compressor.getOutput().isEmpty(), "Produced chemicals while sealed in");
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
    public static void acceptsForgeEnergy(GameTestHelper helper) {
        AtmosphericCompressorBlockEntity compressor = place(helper, CENTER);
        IEnergyStorage fe = helper.getLevel().getCapability(Capabilities.EnergyStorage.BLOCK, helper.absolutePos(CENTER), Direction.DOWN);
        helper.assertTrue(fe != null, "No FE capability on the bottom");
        int accepted = fe.receiveEnergy(1_000, false);
        helper.assertTrue(accepted > 0, "FE was refused");
        helper.assertTrue(compressor.getEnergy().getEnergy() > 0, "FE did not reach the Joule buffer");
        helper.succeed();
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
