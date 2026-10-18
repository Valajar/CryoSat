package io.github.valajar.cryosat.gametest;

import io.github.valajar.cryosat.CryoSat;
import io.github.valajar.cryosat.registries.CryoBlocks;
import io.github.valajar.cryosat.registries.CryoDataComponents;
import io.github.valajar.cryosat.space.AsteroidClass;
import io.github.valajar.cryosat.space.CargoEntry;
import io.github.valajar.cryosat.space.OrbitalAsset;
import io.github.valajar.cryosat.space.OrbitalRegistry;
import io.github.valajar.cryosat.space.PayloadType;
import io.github.valajar.cryosat.space.RocketDesign;
import io.github.valajar.cryosat.space.RocketTier;
import io.github.valajar.cryosat.space.mission.MissionAction;
import io.github.valajar.cryosat.space.mission.MissionControl;
import io.github.valajar.cryosat.space.mission.MissionResult;
import io.github.valajar.cryosat.space.mission.MissionSummary;
import io.github.valajar.cryosat.space.mission.TileEntityMissionControl;
import io.netty.buffer.Unpooled;
import java.util.List;
import java.util.UUID;
import mekanism.api.Action;
import mekanism.api.AutomationType;
import mekanism.common.block.attribute.Attribute;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Mission Control's server side: each player lists only their own assets, the actions change the registry, and actions on
 * someone else's asset are refused. Assets are created with {@link OrbitalRegistry#launch} for random owners, so tests
 * do not see each other's assets in the shared registry.
 */
@GameTestHolder(CryoSat.MODID)
@PrefixGameTestTemplate(false)
public class MissionControlTests {
    private static final RocketDesign BASIC_MINER = new RocketDesign(RocketTier.BASIC, PayloadType.ASTEROID_MINER, RocketTier.BASIC);

    private static MinecraftServer server(GameTestHelper helper) {
        return helper.getLevel().getServer();
    }

    private static OrbitalRegistry registry(GameTestHelper helper) {
        return OrbitalRegistry.get(server(helper));
    }

    private static OrbitalAsset launch(GameTestHelper helper, UUID owner, GlobalPos receiver) {
        var result = registry(helper).launch(server(helper), owner, BASIC_MINER, AsteroidClass.ASTEROID_BELT, receiver);
        helper.assertTrue(result.left().isPresent(), "launch failed: " + result);
        return result.left().get();
    }

    private static OrbitalAsset launch(GameTestHelper helper, UUID owner) {
        return launch(helper, owner, null);
    }

    private static MissionResult act(GameTestHelper helper, UUID player, MissionAction action, OrbitalAsset asset, String name, ItemStack card) {
        return MissionControl.perform(server(helper), player, action, asset.id, name, card);
    }

    private static ItemStack card(GlobalPos target) {
        // Any item carrying the component works as an Uplink Card here; the real card item comes from the Cargo Receiver workstream
        ItemStack stack = new ItemStack(Items.PAPER);
        stack.set(CryoDataComponents.UPLINK_TARGET, target);
        return stack;
    }

    private static void cleanUp(GameTestHelper helper, OrbitalAsset... assets) {
        for (OrbitalAsset asset : assets) {
            registry(helper).decommission(asset);
        }
    }

    @GameTest(template = "empty")
    public static void listsOnlyOwnAssets(GameTestHelper helper) {
        UUID alice = UUID.randomUUID();
        UUID bob = UUID.randomUUID();
        OrbitalAsset first = launch(helper, alice);
        OrbitalAsset other = launch(helper, bob);
        OrbitalAsset second = launch(helper, alice);

        List<MissionSummary> list = MissionControl.summaries(server(helper), alice);
        helper.assertTrue(list.size() == 2, "alice should see 2 assets, saw " + list.size());
        helper.assertTrue(list.get(0).id().equals(first.id) && list.get(1).id().equals(second.id), "alice's assets missing or out of launch order");
        helper.assertTrue(MissionControl.summaries(server(helper), bob).stream().map(MissionSummary::id).toList().equals(List.of(other.id)),
              "bob should see only his asset");
        helper.assertTrue(MissionControl.summaries(server(helper), UUID.randomUUID()).isEmpty(), "a stranger sees assets");

        // The asteroid stays hidden until the miner arrives
        MissionSummary travelling = list.get(0);
        helper.assertTrue(travelling.state() == OrbitalAsset.State.TRAVELLING && travelling.asteroid().isEmpty() && travelling.totalBatches() == 0,
              "asteroid shown while travelling: " + travelling);
        first.nextEventAt = 0;
        registry(helper).tick(server(helper));
        MissionSummary arrived = MissionControl.summaries(server(helper), alice).get(0);
        helper.assertTrue(arrived.state() == OrbitalAsset.State.MINING && arrived.asteroid().isPresent()
                          && arrived.totalBatches() == first.asteroid.batches(), "asteroid not shown after arrival: " + arrived);
        helper.assertTrue(arrived.asteroid().get().asteroidClass().equals(first.asteroid.asteroidClass()), "wrong asteroid class");
        cleanUp(helper, first, other, second);
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void summarySurvivesSync(GameTestHelper helper) {
        UUID owner = UUID.randomUUID();
        OrbitalAsset asset = launch(helper, owner, GlobalPos.of(Level.OVERWORLD, new BlockPos(1, 2, 3)));
        asset.nextEventAt = 0;
        registry(helper).tick(server(helper));
        asset.pending.add(new CargoEntry(Items.RAW_IRON.builtInRegistryHolder(), 100));
        asset.pending.add(new CargoEntry(Items.RAW_GOLD.builtInRegistryHolder(), 23));
        registry(helper).rename(asset, "Ceres Hauler");
        MissionSummary summary = MissionSummary.of(asset);
        helper.assertTrue(summary.cargoInOrbit() == 123, "cargo not summed: " + summary.cargoInOrbit());

        RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), helper.getLevel().registryAccess());
        MissionSummary.STREAM_CODEC.encode(buffer, summary);
        MissionSummary decoded = MissionSummary.STREAM_CODEC.decode(buffer);
        helper.assertTrue(decoded.equals(summary), "summary changed in transit: " + summary + " became " + decoded);
        helper.assertTrue(buffer.readableBytes() == 0, "bytes left over after decoding");
        cleanUp(helper, asset);
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void renameAsset(GameTestHelper helper) {
        UUID owner = UUID.randomUUID();
        OrbitalAsset asset = launch(helper, owner);
        String defaultName = asset.displayName().getString();

        helper.assertTrue(act(helper, owner, MissionAction.RENAME, asset, "  Ceres Hauler  ", ItemStack.EMPTY) == MissionResult.RENAMED, "rename refused");
        helper.assertTrue(asset.name.equals("Ceres Hauler"), "name was " + asset.name);
        helper.assertTrue(MissionControl.summaries(server(helper), owner).get(0).displayName().getString().equals("Ceres Hauler"), "list shows the old name");

        act(helper, owner, MissionAction.RENAME, asset, "x".repeat(100), ItemStack.EMPTY);
        helper.assertTrue(asset.name.length() == MissionControl.MAX_NAME_LENGTH, "long name not capped: " + asset.name.length());

        act(helper, owner, MissionAction.RENAME, asset, "", ItemStack.EMPTY);
        helper.assertTrue(asset.name.isEmpty() && asset.displayName().getString().equals(defaultName), "empty name did not restore the default name");
        cleanUp(helper, asset);
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void relinkFromCardInSlot(GameTestHelper helper) {
        helper.setBlock(BlockPos.ZERO, CryoBlocks.MISSION_CONTROL.get().defaultBlockState());
        TileEntityMissionControl tile = helper.getBlockEntity(BlockPos.ZERO);
        UUID owner = UUID.randomUUID();
        OrbitalAsset asset = launch(helper, owner);

        helper.assertTrue(tile.perform(owner, MissionAction.RELINK, asset.id, "") == MissionResult.NO_CARD, "relinked without a card");
        helper.assertFalse(tile.getCardSlot().isItemValid(new ItemStack(Items.PAPER)), "slot accepts an item without an uplink target");

        GlobalPos target = GlobalPos.of(helper.getLevel().dimension(), helper.absolutePos(new BlockPos(0, 0, 0)).offset(5, 0, 7));
        ItemStack rest = tile.getCardSlot().insertItem(card(target), Action.EXECUTE, AutomationType.MANUAL);
        helper.assertTrue(rest.isEmpty(), "slot refused the uplink card");
        helper.assertTrue(tile.perform(owner, MissionAction.RELINK, asset.id, "") == MissionResult.RELINKED, "relink refused");
        helper.assertTrue(target.equals(asset.receiver), "receiver is " + asset.receiver + ", expected " + target);
        helper.assertTrue(MissionControl.summaries(server(helper), owner).get(0).receiver().equals(java.util.Optional.of(target)), "list shows the old receiver");
        helper.assertFalse(tile.getCard().isEmpty(), "relinking used up the card");

        helper.assertTrue(tile.perform(owner, MissionAction.UNLINK, asset.id, "") == MissionResult.UNLINKED, "unlink refused");
        helper.assertTrue(asset.receiver == null, "still linked after unlinking");
        cleanUp(helper, asset);
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void decommissionRemovesAssetAndCargo(GameTestHelper helper) {
        UUID owner = UUID.randomUUID();
        OrbitalAsset asset = launch(helper, owner);
        OrbitalAsset kept = launch(helper, owner);
        // Run the whole mission without a receiver, so all its cargo waits in orbit
        for (int i = 0; i < 20 && asset.state != OrbitalAsset.State.RETIRED; i++) {
            asset.nextEventAt = 0;
            registry(helper).tick(server(helper));
        }
        helper.assertFalse(asset.pending.isEmpty(), "no cargo waiting in orbit");
        helper.assertTrue(MissionControl.summaries(server(helper), owner).get(0).cargoInOrbit() > 0, "list shows no cargo in orbit");

        helper.assertTrue(act(helper, owner, MissionAction.DECOMMISSION, asset, "", ItemStack.EMPTY) == MissionResult.DECOMMISSIONED, "decommission refused");
        helper.assertTrue(registry(helper).get(asset.id) == null, "asset still in the registry");
        List<MissionSummary> left = MissionControl.summaries(server(helper), owner);
        helper.assertTrue(left.size() == 1 && left.get(0).id().equals(kept.id), "decommissioned the wrong asset: " + left);
        helper.assertTrue(act(helper, owner, MissionAction.RENAME, asset, "Ghost", ItemStack.EMPTY) == MissionResult.NOT_YOURS, "acted on a removed asset");
        cleanUp(helper, kept);
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void refusesOtherPlayersAssets(GameTestHelper helper) {
        UUID owner = UUID.randomUUID();
        UUID intruder = UUID.randomUUID();
        GlobalPos home = GlobalPos.of(Level.OVERWORLD, new BlockPos(10, 64, 10));
        OrbitalAsset asset = launch(helper, owner, home);
        registry(helper).rename(asset, "Mine");
        ItemStack card = card(GlobalPos.of(Level.OVERWORLD, new BlockPos(-50, 70, -50)));

        for (MissionAction action : MissionAction.values()) {
            MissionResult result = act(helper, intruder, action, asset, "Stolen", card);
            helper.assertTrue(result == MissionResult.NOT_YOURS, action + " on someone else's asset gave " + result);
        }
        helper.assertTrue(registry(helper).get(asset.id) == asset, "intruder decommissioned the asset");
        helper.assertTrue(asset.name.equals("Mine"), "intruder renamed the asset to " + asset.name);
        helper.assertTrue(home.equals(asset.receiver), "intruder relinked the asset to " + asset.receiver);
        cleanUp(helper, asset);
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void activeWhileOwnerHasWorkInOrbit(GameTestHelper helper) {
        // Setting a Mekanism owner needs the player profile cache, which the game test server lacks, so this checks the rule
        // the tile applies to its owner rather than the block state
        UUID owner = UUID.randomUUID();
        helper.assertFalse(MissionControl.hasWorkInOrbit(server(helper), owner), "work in orbit before any launch");
        helper.assertFalse(MissionControl.hasWorkInOrbit(server(helper), null), "an unowned Mission Control has work in orbit");
        OrbitalAsset asset = launch(helper, owner);
        helper.assertTrue(MissionControl.hasWorkInOrbit(server(helper), owner), "no work in orbit while travelling");
        for (int i = 0; i < 20 && asset.state != OrbitalAsset.State.RETIRED; i++) {
            asset.nextEventAt = 0;
            registry(helper).tick(server(helper));
        }
        helper.assertTrue(MissionControl.hasWorkInOrbit(server(helper), owner), "retired miner with cargo waiting counts as done");
        asset.pending.clear();
        helper.assertFalse(MissionControl.hasWorkInOrbit(server(helper), owner), "retired, delivered miner still counts as work");
        cleanUp(helper, asset);

        // An unowned block stays inactive
        helper.setBlock(BlockPos.ZERO, CryoBlocks.MISSION_CONTROL.get().defaultBlockState());
        helper.runAfterDelay(25, () -> {
            helper.assertFalse(Attribute.isActive(helper.getBlockState(BlockPos.ZERO)), "unowned Mission Control is active");
            helper.succeed();
        });
    }

    @GameTest(template = "empty")
    public static void recipeLoads(GameTestHelper helper) {
        var recipe = helper.getLevel().getRecipeManager().byKey(CryoSat.rl("mission_control"));
        helper.assertTrue(recipe.isPresent(), "Mission Control recipe missing; does a Mekanism item or tag it uses exist?");
        helper.assertTrue(recipe.get().value().getResultItem(helper.getLevel().registryAccess()).is(CryoBlocks.MISSION_CONTROL.asItem()),
              "Mission Control recipe makes something else");
        helper.succeed();
    }
}
