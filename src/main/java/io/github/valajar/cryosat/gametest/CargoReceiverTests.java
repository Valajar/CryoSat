package io.github.valajar.cryosat.gametest;

import com.mojang.datafixers.util.Either;
import io.github.valajar.cryosat.CryoSat;
import io.github.valajar.cryosat.registries.CryoBlocks;
import io.github.valajar.cryosat.registries.CryoDataComponents;
import io.github.valajar.cryosat.registries.CryoItems;
import io.github.valajar.cryosat.space.AsteroidClass;
import io.github.valajar.cryosat.space.AsteroidSize;
import io.github.valajar.cryosat.space.CargoEntry;
import io.github.valajar.cryosat.space.MissionProblem;
import io.github.valajar.cryosat.space.OrbitalAsset;
import io.github.valajar.cryosat.space.OrbitalRegistry;
import io.github.valajar.cryosat.space.PayloadType;
import io.github.valajar.cryosat.space.RocketDesign;
import io.github.valajar.cryosat.space.RocketTier;
import io.github.valajar.cryosat.space.receiver.LinkedMission;
import io.github.valajar.cryosat.space.receiver.TileEntityCargoReceiver;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import mekanism.api.inventory.IInventorySlot;
import mekanism.common.lib.transmitter.TransmissionType;
import mekanism.common.tile.component.config.ConfigInfo;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Cargo Receiver and Uplink Card with the real block: missions fill the receiver, a full receiver leaves the rest in
 * orbit, the card stores the receiver. Missions are fast forwarded like in {@link OrbitalTests}.
 */
@GameTestHolder(CryoSat.MODID)
@PrefixGameTestTemplate(false)
public class CargoReceiverTests {
    private static final RocketDesign BASIC_MINER = new RocketDesign(RocketTier.BASIC, PayloadType.ASTEROID_MINER, RocketTier.BASIC);
    private static final BlockPos CENTER = new BlockPos(1, 1, 1);
    private static final String ARENA = "empty_3x3x3";

    private static TileEntityCargoReceiver place(GameTestHelper helper, BlockPos pos) {
        helper.setBlock(pos, CryoBlocks.CARGO_RECEIVER.get().defaultBlockState());
        return helper.getBlockEntity(pos);
    }

    /** Launches a Basic miner that found a small asteroid, so its whole cargo fits into the 27 slots. */
    private static OrbitalAsset launch(GameTestHelper helper, BlockPos receiver) {
        MinecraftServer server = helper.getLevel().getServer();
        OrbitalRegistry registry = OrbitalRegistry.get(server);
        GlobalPos target = GlobalPos.of(helper.getLevel().dimension(), helper.absolutePos(receiver));
        for (int attempt = 0; attempt < 100; attempt++) {
            Either<OrbitalAsset, MissionProblem> result = registry.launch(server, UUID.randomUUID(), BASIC_MINER, AsteroidClass.ASTEROID_BELT, target);
            helper.assertTrue(result.left().isPresent(), "launch failed: " + result);
            OrbitalAsset asset = result.left().get();
            if (asset.asteroid != null && asset.asteroid.size() == AsteroidSize.SMALL) {
                return asset;
            }
            registry.decommission(asset);
        }
        throw new GameTestAssertException("no small asteroid in 100 launches");
    }

    /** Skips travel and every batch. */
    private static void fastForward(GameTestHelper helper, OrbitalAsset asset) {
        MinecraftServer server = helper.getLevel().getServer();
        OrbitalRegistry registry = OrbitalRegistry.get(server);
        for (int i = 0; i < 20 && asset.state != OrbitalAsset.State.RETIRED; i++) {
            asset.nextEventAt = 0;
            registry.tick(server);
        }
        helper.assertTrue(asset.state == OrbitalAsset.State.RETIRED, "miner did not retire: " + asset.state);
    }

    private static Map<Item, Integer> contents(TileEntityCargoReceiver receiver) {
        Map<Item, Integer> contents = new HashMap<>();
        for (IInventorySlot slot : receiver.getCargoSlots()) {
            if (!slot.isEmpty()) {
                contents.merge(slot.getStack().getItem(), slot.getCount(), Integer::sum);
            }
        }
        return contents;
    }

    private static void assertReceivedAll(GameTestHelper helper, TileEntityCargoReceiver receiver, OrbitalAsset asset) {
        Map<Item, Integer> received = contents(receiver);
        for (CargoEntry entry : asset.asteroid.cargo()) {
            int got = received.getOrDefault(entry.item().value(), 0);
            helper.assertTrue(got == entry.count(), BuiltInRegistries.ITEM.getKey(entry.item().value()) + ": expected " + entry.count() + ", got " + got);
        }
    }

    @GameTest(template = "empty")
    public static void missionFillsReceiver(GameTestHelper helper) {
        TileEntityCargoReceiver receiver = place(helper, BlockPos.ZERO);
        OrbitalAsset asset = launch(helper, BlockPos.ZERO);
        List<LinkedMission> linked = receiver.getLinkedMissions();
        helper.assertTrue(linked.size() == 1 && linked.getFirst().state() == OrbitalAsset.State.TRAVELLING, "receiver should list the new mission: " + linked);

        fastForward(helper, asset);
        helper.assertTrue(asset.pending.isEmpty(), "cargo left in orbit: " + asset.pending);
        assertReceivedAll(helper, receiver, asset);
        helper.assertTrue(receiver.getCurrentRedstoneLevel() > 0 || receiver.getRedstoneLevel() > 0, "comparator output stays at 0 with cargo inside");
        // Done missions drop off the receiver's list
        helper.assertTrue(receiver.getLinkedMissions().isEmpty(), "finished mission still listed: " + receiver.getLinkedMissions());
        OrbitalRegistry.get(helper.getLevel().getServer()).decommission(asset);
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void fullReceiverKeepsRestInOrbit(GameTestHelper helper) {
        TileEntityCargoReceiver receiver = place(helper, BlockPos.ZERO);
        List<IInventorySlot> slots = receiver.getCargoSlots();
        // Leave two slots free, so some cargo lands and the rest has to wait
        for (int i = 0; i < slots.size() - 2; i++) {
            slots.get(i).setStack(new ItemStack(Items.COBBLESTONE, 64));
        }
        OrbitalAsset asset = launch(helper, BlockPos.ZERO);
        fastForward(helper, asset);
        helper.assertFalse(asset.pending.isEmpty(), "everything fit into two slots?");
        int landed = 0;
        for (int i = slots.size() - 2; i < slots.size(); i++) {
            helper.assertTrue(slots.get(i).getCount() == slots.get(i).getLimit(slots.get(i).getStack()), "free slot " + i + " not filled up");
            landed += slots.get(i).getCount();
        }
        int waiting = receiver.getLinkedMissions().getFirst().waiting();
        int total = asset.asteroid.cargo().stream().mapToInt(CargoEntry::count).sum();
        helper.assertTrue(landed + waiting == total, "landed " + landed + " plus waiting " + waiting + " is not the asteroid's " + total);

        // Empty the receiver; the next orbital tick delivers the rest
        for (IInventorySlot slot : slots) {
            slot.setEmpty();
        }
        MinecraftServer server = helper.getLevel().getServer();
        OrbitalRegistry.get(server).tick(server);
        helper.assertTrue(asset.pending.isEmpty(), "cargo still waiting after slots were freed: " + asset.pending);
        Map<Item, Integer> received = contents(receiver);
        int got = received.values().stream().mapToInt(Integer::intValue).sum();
        helper.assertTrue(got == waiting, "received " + got + " of the " + waiting + " waiting items");
        OrbitalRegistry.get(server).decommission(asset);
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void ejectsIntoChest(GameTestHelper helper) {
        TileEntityCargoReceiver receiver = place(helper, CENTER);
        ConfigInfo items = receiver.getConfig().getConfig(TransmissionType.ITEM);
        helper.assertTrue(items != null, "receiver has no item side config");
        items.setEjecting(true);
        BlockPos above = CENTER.above();
        helper.setBlock(above, Blocks.CHEST);
        receiver.getCargoSlots().getFirst().setStack(new ItemStack(Items.RAW_IRON, 10));
        helper.succeedWhen(() -> {
            ChestBlockEntity chest = helper.getBlockEntity(above);
            helper.assertTrue(chest.countItem(Items.RAW_IRON) == 10, "chest above holds " + chest.countItem(Items.RAW_IRON) + " raw iron");
            helper.assertTrue(receiver.getCargoSlots().getFirst().isEmpty(), "slot not emptied");
        });
    }

    @GameTest(template = "empty")
    public static void redstoneOffHoldsCargoInOrbit(GameTestHelper helper) {
        TileEntityCargoReceiver receiver = place(helper, BlockPos.ZERO);
        receiver.setControlType(mekanism.common.tile.interfaces.IRedstoneControl.RedstoneControl.HIGH);
        OrbitalAsset asset = launch(helper, BlockPos.ZERO);
        fastForward(helper, asset);
        helper.assertFalse(asset.pending.isEmpty(), "receiver accepted cargo while switched off by redstone");
        helper.assertTrue(contents(receiver).isEmpty(), "receiver holds cargo while switched off");
        receiver.setControlType(mekanism.common.tile.interfaces.IRedstoneControl.RedstoneControl.DISABLED);
        MinecraftServer server = helper.getLevel().getServer();
        OrbitalRegistry.get(server).tick(server);
        helper.assertTrue(asset.pending.isEmpty(), "cargo not delivered once the receiver was back on: " + asset.pending);
        assertReceivedAll(helper, receiver, asset);
        OrbitalRegistry.get(server).decommission(asset);
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void uplinkCardStoresReceiver(GameTestHelper helper) {
        place(helper, BlockPos.ZERO);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        ItemStack card = new ItemStack(CryoItems.UPLINK_CARD.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, card);
        BlockPos absolute = helper.absolutePos(BlockPos.ZERO);
        UseOnContext context = new UseOnContext(player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(absolute), Direction.UP, absolute, false));
        InteractionResult result = card.onItemUseFirst(context);
        helper.assertTrue(result.consumesAction(), "using the card on a receiver did nothing: " + result);
        GlobalPos target = card.get(CryoDataComponents.UPLINK_TARGET);
        helper.assertTrue(GlobalPos.of(helper.getLevel().dimension(), absolute).equals(target), "card stored " + target);

        // Anything that is not a receiver is ignored
        helper.setBlock(new BlockPos(0, 1, 0), Blocks.STONE);
        BlockPos stone = helper.absolutePos(new BlockPos(0, 1, 0));
        UseOnContext onStone = new UseOnContext(player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(stone), Direction.UP, stone, false));
        helper.assertTrue(card.onItemUseFirst(onStone) == InteractionResult.PASS, "card reacted to stone");
        helper.assertTrue(target.equals(card.get(CryoDataComponents.UPLINK_TARGET)), "target changed by using the card on stone");

        // Sneak use in the air clears it
        player.setShiftKeyDown(true);
        card.use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
        helper.assertFalse(card.has(CryoDataComponents.UPLINK_TARGET), "sneak use did not clear the target");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void receiverRecipesLoad(GameTestHelper helper) {
        for (String id : List.of("cargo_receiver", "uplink_card")) {
            helper.assertTrue(helper.getLevel().getRecipeManager().byKey(CryoSat.rl(id)).isPresent(), id + " recipe missing");
        }
        helper.succeed();
    }
}
