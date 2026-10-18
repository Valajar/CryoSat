package io.github.valajar.cryosat.gametest;

import io.github.valajar.cryosat.CryoSat;
import io.github.valajar.cryosat.registries.CryoBlocks;
import io.github.valajar.cryosat.registries.CryoChemicals;
import io.github.valajar.cryosat.registries.CryoDataComponents;
import io.github.valajar.cryosat.registries.CryoItems;
import io.github.valajar.cryosat.space.MissionProblem;
import io.github.valajar.cryosat.space.OrbitalAsset;
import io.github.valajar.cryosat.space.OrbitalRegistry;
import io.github.valajar.cryosat.space.PayloadType;
import io.github.valajar.cryosat.space.RocketDesign;
import io.github.valajar.cryosat.space.RocketTier;
import io.github.valajar.cryosat.space.launch.LaunchPadMultiblockData;
import io.github.valajar.cryosat.space.launch.LaunchSequence;
import io.github.valajar.cryosat.space.launch.LaunchStatus;
import io.github.valajar.cryosat.space.launch.TileEntityLaunchController;
import java.util.List;
import java.util.UUID;
import mekanism.api.Action;
import mekanism.api.MekanismAPI;
import mekanism.api.SerializationConstants;
import mekanism.api.chemical.Chemical;
import mekanism.api.chemical.ChemicalStack;
import mekanism.api.chemical.IChemicalHandler;
import mekanism.common.capabilities.Capabilities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.Holder;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import org.jetbrains.annotations.Nullable;

/**
 * Builds a real 5x5 Launch Pad on the floor of the arena: controller in the middle of the north edge, a fuel port in the
 * middle of the west edge, plating everywhere else. The rocket column above the centre is the arena's air.
 */
@GameTestHolder(CryoSat.MODID)
@PrefixGameTestTemplate(false)
public class LaunchPadTests {
    private static final String ARENA = "empty_5x8x5";
    private static final BlockPos CONTROLLER = new BlockPos(2, 0, 0);
    private static final BlockPos PORT = new BlockPos(0, 0, 2);
    private static final BlockPos CENTRE = new BlockPos(2, 0, 2);
    private static final RocketDesign BASIC_MINER = new RocketDesign(RocketTier.BASIC, PayloadType.ASTEROID_MINER, RocketTier.BASIC);
    private static final int FORM_TICKS = 20;

    private static void build(GameTestHelper helper, BlockPos controller) {
        for (BlockPos pos : BlockPos.betweenClosed(BlockPos.ZERO, new BlockPos(4, 0, 4))) {
            helper.setBlock(pos, CryoBlocks.LAUNCH_PAD_PLATING.get());
        }
        helper.setBlock(controller, CryoBlocks.LAUNCH_CONTROLLER.get());
        helper.setBlock(PORT, CryoBlocks.LAUNCH_PAD_FUEL_PORT.get());
    }

    private static TileEntityLaunchController controller(GameTestHelper helper) {
        return helper.getBlockEntity(CONTROLLER);
    }

    private static LaunchPadMultiblockData pad(GameTestHelper helper) {
        return controller(helper).getMultiblock();
    }

    /** Builds the pad, waits for it to form and gives it an owner (unless {@code owner} is null). */
    private static void formed(GameTestHelper helper, @Nullable UUID owner, java.util.function.Consumer<LaunchPadMultiblockData> then) {
        build(helper, CONTROLLER);
        helper.runAfterDelay(FORM_TICKS, () -> {
            LaunchPadMultiblockData pad = pad(helper);
            helper.assertTrue(pad.isFormed(), "launch pad did not form");
            if (owner != null) {
                // setOwnerUUID looks the owner's name up in the profile cache, which a game test server does not have
                CompoundTag security = new CompoundTag();
                security.putUUID(SerializationConstants.OWNER_UUID, owner);
                controller(helper).getSecurity().deserialize(security, helper.getLevel().registryAccess());
            }
            then.accept(pad);
        });
    }

    private static ItemStack stack(Item item) {
        return new ItemStack(item);
    }

    private static void insertParts(LaunchPadMultiblockData pad, RocketTier tier, RocketTier payloadTier) {
        pad.engineSlot.setStack(stack(CryoItems.ROCKET_ENGINES.get(tier).get()));
        pad.fuelTankSlot.setStack(stack(CryoItems.FUEL_TANKS.get(tier).get()));
        pad.guidanceSlot.setStack(stack(CryoItems.GUIDANCE_COMPUTER.get()));
        pad.payloadSlot.setStack(stack(CryoItems.ASTEROID_MINERS.get(payloadTier).get()));
    }

    private static GlobalPos uplink(GameTestHelper helper, LaunchPadMultiblockData pad) {
        GlobalPos target = GlobalPos.of(helper.getLevel().dimension(), helper.absolutePos(new BlockPos(4, 1, 4)));
        ItemStack card = stack(Items.PAPER);
        card.set(CryoDataComponents.UPLINK_TARGET, target);
        pad.uplinkSlot.setStack(card);
        return target;
    }

    /** Fills the grain slot through the fuel port's item handler, like a pipe would. */
    private static void fuelWithGrains(GameTestHelper helper, int count) {
        IItemHandler port = helper.getLevel().getCapability(net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK, helper.absolutePos(PORT), Direction.WEST);
        helper.assertTrue(port != null, "fuel port exposes no item handler");
        ItemStack rest = ItemHandlerHelper.insertItemStacked(port, new ItemStack(CryoItems.SOLID_PROPELLANT_GRAIN.get(), count), false);
        helper.assertTrue(rest.isEmpty(), "fuel port refused grains: " + rest);
        ItemStack engine = ItemHandlerHelper.insertItemStacked(port, stack(CryoItems.ROCKET_ENGINES.get(RocketTier.BASIC).get()), false);
        helper.assertFalse(engine.isEmpty(), "fuel port accepted a rocket part");
    }

    private static List<OrbitalAsset> assets(GameTestHelper helper, UUID owner) {
        return OrbitalRegistry.get(helper.getLevel().getServer()).ownedBy(owner);
    }

    private static void cleanUp(GameTestHelper helper, UUID owner) {
        OrbitalRegistry registry = OrbitalRegistry.get(helper.getLevel().getServer());
        assets(helper, owner).forEach(registry::decommission);
    }

    @GameTest(template = "empty")
    public static void padRecipesLoad(GameTestHelper helper) {
        // Fails if any Mekanism item or tag the recipes refer to does not exist at runtime
        for (String id : List.of("launch_pad_plating", "launch_pad_fuel_port", "launch_controller")) {
            helper.assertTrue(helper.getLevel().getRecipeManager().byKey(CryoSat.rl(id)).isPresent(), id + " recipe missing");
        }
        helper.succeed();
    }

    @GameTest(template = ARENA, timeoutTicks = 100)
    public static void padForms(GameTestHelper helper) {
        build(helper, CONTROLLER);
        helper.succeedWhen(() -> {
            LaunchPadMultiblockData pad = pad(helper);
            helper.assertTrue(pad.isFormed(), "launch pad did not form");
            helper.assertTrue(pad.fuelPorts == 1, "expected 1 fuel port, counted " + pad.fuelPorts);
            helper.assertTrue(controller(helper).isMaster(), "the controller is not the multiblock master");
            helper.assertTrue(pad.getCentre().equals(helper.absolutePos(CENTRE)), "pad centre at " + pad.getCentre());
            helper.assertTrue(pad.status == LaunchStatus.ASSEMBLY_PROBLEM && pad.getProblem() == MissionProblem.MISSING_ENGINE,
                  "empty pad status " + pad.status + " / " + pad.getProblem());
        });
    }

    @GameTest(template = ARENA, timeoutTicks = 100)
    public static void refusesControllerInTheMiddle(GameTestHelper helper) {
        build(helper, new BlockPos(2, 0, 1));
        helper.runAfterDelay(FORM_TICKS * 2, () -> {
            TileEntityLaunchController controller = helper.getBlockEntity(new BlockPos(2, 0, 1));
            helper.assertFalse(controller.getMultiblock().isFormed(), "pad formed with its controller off the outer ring");
            helper.succeed();
        });
    }

    @GameTest(template = ARENA, timeoutTicks = 100)
    public static void refusesHoleInThePad(GameTestHelper helper) {
        build(helper, CONTROLLER);
        helper.setBlock(new BlockPos(3, 0, 3), Blocks.STONE);
        helper.runAfterDelay(FORM_TICKS * 2, () -> {
            helper.assertFalse(pad(helper).isFormed(), "pad formed with stone in it");
            helper.succeed();
        });
    }

    @GameTest(template = ARENA, timeoutTicks = 100)
    public static void refusesTwoControllers(GameTestHelper helper) {
        build(helper, CONTROLLER);
        helper.setBlock(new BlockPos(4, 0, 2), CryoBlocks.LAUNCH_CONTROLLER.get());
        helper.runAfterDelay(FORM_TICKS * 2, () -> {
            helper.assertFalse(pad(helper).isFormed(), "pad formed with two controllers");
            helper.succeed();
        });
    }

    @GameTest(template = ARENA, timeoutTicks = 100)
    public static void assemblyConsumesPartsAndReportsProblems(GameTestHelper helper) {
        UUID owner = UUID.randomUUID();
        formed(helper, owner, pad -> {
            insertParts(pad, RocketTier.BASIC, RocketTier.BASIC);
            pad.payloadSlot.setStack(ItemStack.EMPTY);
            helper.assertTrue(pad.assemble() == MissionProblem.MISSING_PAYLOAD, "assembled without a payload");
            pad.payloadSlot.setStack(stack(CryoItems.ASTEROID_MINERS.get(RocketTier.ELITE).get()));
            helper.assertTrue(pad.assemble() == MissionProblem.PAYLOAD_TOO_ADVANCED, "assembled an elite payload on a basic rocket");
            helper.assertTrue(pad.getDesign() == null && pad.engineSlot.getCount() == 1, "a refused assembly consumed parts");
            pad.payloadSlot.setStack(stack(CryoItems.ASTEROID_MINERS.get(RocketTier.BASIC).get()));
            pad.engineSlot.setStack(new ItemStack(CryoItems.ROCKET_ENGINES.get(RocketTier.ADVANCED).get(), 2));
            helper.runAfterDelay(2, () -> {
                helper.assertTrue(pad.status == LaunchStatus.READY_TO_ASSEMBLE, "status before assembling: " + pad.status);
                helper.assertTrue(pad.assemble() == null, "assembly failed");
                helper.assertTrue(BASIC_MINER.equals(pad.getDesign()), "advanced engine on a basic tank should make a basic rocket: " + pad.getDesign());
                helper.assertTrue(pad.engineSlot.getCount() == 1 && pad.fuelTankSlot.isEmpty() && pad.guidanceSlot.isEmpty() && pad.payloadSlot.isEmpty(),
                      "assembly did not consume one of each part");
                helper.runAfterDelay(2, () -> {
                    helper.assertTrue(pad.status == LaunchStatus.NEEDS_FUEL, "assembled pad status " + pad.status);
                    helper.assertTrue(pad.disassemble(null), "nothing to disassemble");
                    helper.assertTrue(pad.getDesign() == null, "design left after disassembly");
                    helper.assertTrue(pad.engineSlot.getCount() == 2 && pad.engineSlot.getStack().is(CryoItems.ROCKET_ENGINES.get(RocketTier.ADVANCED).get()),
                          "engine not given back: " + pad.engineSlot.getStack());
                    helper.assertTrue(!pad.fuelTankSlot.isEmpty() && !pad.guidanceSlot.isEmpty() && !pad.payloadSlot.isEmpty(), "parts not given back");
                    helper.succeed();
                });
            });
        });
    }

    @GameTest(template = ARENA, timeoutTicks = LaunchSequence.DURATION_TICKS + 100)
    public static void launchBurnsFuelAndClearsThePad(GameTestHelper helper) {
        UUID owner = UUID.randomUUID();
        formed(helper, owner, pad -> {
            insertParts(pad, RocketTier.BASIC, RocketTier.BASIC);
            helper.assertTrue(pad.assemble() == null, "assembly failed");
            GlobalPos target = uplink(helper, pad);
            fuelWithGrains(helper, 20);
            helper.assertTrue(pad.solidFuelSlot.getCount() == 20, "grains in the pad: " + pad.solidFuelSlot.getCount());
            helper.runAfterDelay(2, () -> {
                helper.assertTrue(pad.status == LaunchStatus.READY, "fuelled pad status " + pad.status + " / " + pad.getProblem());
                LaunchStatus result = controller(helper).launch(null);
                helper.assertTrue(result == LaunchStatus.LAUNCHING, "launch refused: " + result + " / " + pad.getProblem());
                helper.assertTrue(pad.solidFuelSlot.getCount() == 20 - RocketTier.BASIC.fuel().orElseThrow().solidCount(),
                      "grains after launch: " + pad.solidFuelSlot.getCount());
                List<OrbitalAsset> launched = assets(helper, owner);
                helper.assertTrue(launched.size() == 1, "expected one asset, found " + launched.size());
                OrbitalAsset asset = launched.getFirst();
                helper.assertTrue(asset.design.equals(BASIC_MINER), "asset design " + asset.design);
                helper.assertTrue(target.equals(asset.receiver), "asset receiver " + asset.receiver + ", uplink pointed at " + target);
                helper.assertTrue(pad.isLaunching() && pad.getDesign() != null, "rocket left before the launch sequence");
                helper.assertTrue(controller(helper).launch(null) == LaunchStatus.LAUNCHING && assets(helper, owner).size() == 1, "launched twice");
                helper.assertFalse(pad.uplinkSlot.isEmpty(), "the uplink card was used up");
                helper.runAfterDelay(LaunchSequence.DURATION_TICKS + 5, () -> {
                    helper.assertTrue(pad.getDesign() == null && !pad.isLaunching(), "pad not cleared after the launch sequence");
                    helper.assertTrue(pad.status == LaunchStatus.ASSEMBLY_PROBLEM, "status after the launch: " + pad.status);
                    cleanUp(helper, owner);
                    helper.succeed();
                });
            });
        });
    }

    @GameTest(template = ARENA, timeoutTicks = 100)
    public static void liquidFuelThroughThePort(GameTestHelper helper) {
        // Nobody owns this pad, so the payload goes to the player who presses Launch
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        UUID owner = player.getUUID();
        formed(helper, null, pad -> {
            insertParts(pad, RocketTier.ADVANCED, RocketTier.ADVANCED);
            helper.assertTrue(pad.assemble() == null, "assembly failed");
            IChemicalHandler port = helper.getLevel().getCapability(Capabilities.CHEMICAL.block(), helper.absolutePos(PORT), Direction.WEST);
            helper.assertTrue(port != null, "fuel port exposes no chemical handler");
            Holder<Chemical> ethene = MekanismAPI.CHEMICAL_REGISTRY.getHolder(ResourceLocation.fromNamespaceAndPath(MekanismAPI.MEKANISM_MODID, "ethene")).orElseThrow();
            ChemicalStack refused = port.insertChemical(new ChemicalStack(CryoChemicals.LIQUID_HYDROGEN, 1_000), Action.EXECUTE);
            helper.assertTrue(refused.getAmount() == 1_000, "an advanced rocket's pad took liquid hydrogen");
            for (ChemicalStack fuel : List.of(new ChemicalStack(CryoChemicals.LIQUID_OXYGEN, 4_000), new ChemicalStack(ethene, 4_000))) {
                ChemicalStack rest = port.insertChemical(fuel, Action.EXECUTE);
                helper.assertTrue(rest.isEmpty(), "fuel port refused " + fuel + ", left " + rest);
            }
            helper.runAfterDelay(2, () -> {
                helper.assertTrue(pad.status == LaunchStatus.READY, "fuelled pad status " + pad.status + " / " + pad.getProblem());
                helper.assertTrue(controller(helper).launch(null) == LaunchStatus.NO_OWNER, "launched without an owner");
                helper.assertTrue(controller(helper).launch(player) == LaunchStatus.LAUNCHING, "launch refused");
                helper.assertTrue(pad.oxidiserTank.isEmpty() && pad.propellantTank.isEmpty(), "fuel left after launch: "
                                                                                             + pad.oxidiserTank.getStack() + ", " + pad.propellantTank.getStack());
                List<OrbitalAsset> launched = assets(helper, owner);
                helper.assertTrue(launched.size() == 1 && launched.getFirst().receiver == null, "expected one asset without a receiver: " + launched);
                cleanUp(helper, owner);
                helper.succeed();
            });
        });
    }

    @GameTest(template = ARENA, timeoutTicks = 100)
    public static void obstructedColumnBlocksTheLaunch(GameTestHelper helper) {
        UUID owner = UUID.randomUUID();
        formed(helper, owner, pad -> {
            insertParts(pad, RocketTier.BASIC, RocketTier.BASIC);
            helper.assertTrue(pad.assemble() == null, "assembly failed");
            fuelWithGrains(helper, 16);
            helper.setBlock(CENTRE.offset(1, 4, -1), Blocks.STONE);
            helper.assertTrue(controller(helper).launch(null) == LaunchStatus.OBSTRUCTED, "launched through a block");
            helper.assertTrue(pad.solidFuelSlot.getCount() == 16 && assets(helper, owner).isEmpty(), "a refused launch burned fuel or made an asset");
            helper.setBlock(CENTRE.offset(1, 4, -1), Blocks.AIR);
            helper.assertTrue(controller(helper).launch(null) == LaunchStatus.LAUNCHING, "launch refused after clearing the column");
            cleanUp(helper, owner);
            helper.succeed();
        });
    }

    @GameTest(template = ARENA, timeoutTicks = 100)
    public static void redstonePulseLaunches(GameTestHelper helper) {
        UUID owner = UUID.randomUUID();
        formed(helper, owner, pad -> {
            insertParts(pad, RocketTier.BASIC, RocketTier.BASIC);
            helper.assertTrue(pad.assemble() == null, "assembly failed");
            fuelWithGrains(helper, 16);
            helper.runAfterDelay(2, () -> {
                helper.assertTrue(assets(helper, owner).isEmpty(), "launched before the redstone signal");
                Block redstone = Blocks.REDSTONE_BLOCK;
                helper.setBlock(CONTROLLER.above(), redstone);
                helper.runAfterDelay(3, () -> {
                    helper.assertTrue(pad.isLaunching(), "redstone did not launch the rocket: " + pad.status);
                    helper.assertTrue(assets(helper, owner).size() == 1, "expected one asset");
                    cleanUp(helper, owner);
                    helper.succeed();
                });
            });
        });
    }
}
