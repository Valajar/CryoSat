package io.github.valajar.cryosat.gametest;

import com.mojang.datafixers.util.Either;
import io.github.valajar.cryosat.CryoSat;
import io.github.valajar.cryosat.registries.CryoBlocks;
import io.github.valajar.cryosat.registries.CryoChemicals;
import io.github.valajar.cryosat.space.AsteroidClass;
import io.github.valajar.cryosat.space.AsteroidSize;
import io.github.valajar.cryosat.space.CargoEntry;
import io.github.valajar.cryosat.space.MissionProblem;
import io.github.valajar.cryosat.space.OrbitalAsset;
import io.github.valajar.cryosat.space.OrbitalRegistry;
import io.github.valajar.cryosat.space.PayloadType;
import io.github.valajar.cryosat.space.RocketDesign;
import io.github.valajar.cryosat.space.RocketTier;
import io.github.valajar.cryosat.space.SeparatorBonus;
import io.github.valajar.cryosat.space.SpaceData;
import io.github.valajar.cryosat.space.receiver.TileEntityCargoReceiver;
import io.github.valajar.cryosat.superconductor.Cryostat;
import io.github.valajar.cryosat.superconductor.CryostatState;
import io.github.valajar.cryosat.superconductor.separator.TileEntitySuperconductingSeparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import mekanism.api.Action;
import mekanism.api.MekanismAPI;
import mekanism.api.chemical.ChemicalStack;
import mekanism.api.chemical.IChemicalHandler;
import mekanism.api.inventory.IInventorySlot;
import mekanism.common.attachments.containers.chemical.AttachedChemicals;
import mekanism.common.block.attribute.Attribute;
import mekanism.common.capabilities.Capabilities;
import mekanism.common.registries.MekanismDataComponents;
import mekanism.common.tile.interfaces.IRedstoneControl.RedstoneControl;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.jetbrains.annotations.Nullable;

/**
 * The Superconducting Separator next to a real Cargo Receiver. Missions are fast forwarded inside one game tick, like in
 * {@link CargoReceiverTests}, so the separator's own trickle does not run between batches and the per batch coolant can be
 * checked exactly. The 30 second cool down is skipped with {@link Cryostat#setCooldown}.
 */
@GameTestHolder(CryoSat.MODID)
@PrefixGameTestTemplate(false)
public class SuperconductingSeparatorTests {
    private static final RocketDesign BASIC_MINER = new RocketDesign(RocketTier.BASIC, PayloadType.ASTEROID_MINER, RocketTier.BASIC);
    private static final String ARENA = "empty_3x3x3";
    private static final BlockPos RECEIVER = new BlockPos(1, 1, 1);
    private static final BlockPos SEPARATOR = new BlockPos(2, 1, 1);

    private static TileEntityCargoReceiver placeReceiver(GameTestHelper helper) {
        helper.setBlock(RECEIVER, CryoBlocks.CARGO_RECEIVER.get().defaultBlockState());
        return helper.getBlockEntity(RECEIVER);
    }

    private static TileEntitySuperconductingSeparator placeSeparator(GameTestHelper helper) {
        helper.setBlock(SEPARATOR, CryoBlocks.SUPERCONDUCTING_SEPARATOR.get().defaultBlockState());
        return helper.getBlockEntity(SEPARATOR);
    }

    /** Fills the coolant and skips the cool down; the separator superconducts from its next tick. */
    private static void chill(TileEntitySuperconductingSeparator separator, long coolant) {
        Cryostat cryostat = separator.getCryostat();
        cryostat.coolantTank.setStack(new ChemicalStack(CryoChemicals.LIQUID_NITROGEN, coolant));
        cryostat.setCooldown(0);
    }

    /** Launches a Basic miner that found a small asteroid (retrying like {@link CargoReceiverTests}). */
    private static OrbitalAsset launch(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        OrbitalRegistry registry = OrbitalRegistry.get(server);
        GlobalPos target = GlobalPos.of(helper.getLevel().dimension(), helper.absolutePos(RECEIVER));
        for (int attempt = 0; attempt < 100; attempt++) {
            Either<OrbitalAsset, MissionProblem> result = registry.launch(server, UUID.randomUUID(), BASIC_MINER, AsteroidClass.ASTEROID_BELT, target);
            helper.assertTrue(result.left().isPresent(), "launch failed: " + result);
            OrbitalAsset asset = result.left().get();
            if (asset.miner().asteroid != null && asset.miner().asteroid.size() == AsteroidSize.SMALL) {
                return asset;
            }
            registry.decommission(asset);
        }
        throw new GameTestAssertException("no small asteroid in 100 launches");
    }

    /** Skips travel and every batch within this tick. */
    private static void fastForward(GameTestHelper helper, OrbitalAsset asset) {
        MinecraftServer server = helper.getLevel().getServer();
        OrbitalRegistry registry = OrbitalRegistry.get(server);
        for (int i = 0; i < 20 && asset.state != OrbitalAsset.State.RETIRED; i++) {
            asset.nextEventAt = 0;
            registry.tick(server);
        }
        helper.assertTrue(asset.state == OrbitalAsset.State.RETIRED, "miner did not retire: " + asset.state);
    }

    private static void decommission(GameTestHelper helper, OrbitalAsset asset) {
        OrbitalRegistry.get(helper.getLevel().getServer()).decommission(asset);
    }

    /** Everything the mission produced: what landed in the receiver plus what still waits in orbit. */
    private static Map<Item, Integer> produced(TileEntityCargoReceiver receiver, OrbitalAsset asset) {
        Map<Item, Integer> produced = new HashMap<>();
        for (IInventorySlot slot : receiver.getCargoSlots()) {
            if (!slot.isEmpty()) {
                produced.merge(slot.getStack().getItem(), slot.getCount(), Integer::sum);
            }
        }
        for (CargoEntry entry : asset.miner().pending) {
            produced.merge(entry.item().value(), entry.count(), Integer::sum);
        }
        return produced;
    }

    /** What the base cargo turns into when the first {@code separated} batches get the +50 percent. */
    private static Map<Item, Integer> expected(OrbitalAsset asset, int separated) {
        Map<Item, Integer> expected = new HashMap<>();
        for (int i = 0; i < asset.miner().asteroid.batches(); i++) {
            for (CargoEntry entry : asset.miner().asteroid.batch(i)) {
                int count = i < separated ? (int) Math.round(entry.count() * SeparatorBonus.MULTIPLIER) : entry.count();
                expected.merge(entry.item().value(), count, Integer::sum);
            }
        }
        return expected;
    }

    private static Set<Item> bonusItems(OrbitalAsset asset) {
        Set<Item> items = new HashSet<>();
        AsteroidClass asteroidClass = SpaceData.ASTEROIDS.get(asset.miner().asteroid.asteroidClass());
        if (asteroidClass != null) {
            for (AsteroidClass.BonusYield bonus : asteroidClass.bonusYields()) {
                bonus.yield().resolve().ifPresent(item -> items.add(item.value()));
            }
        }
        return items;
    }

    /**
     * Base cargo must match {@code expected} exactly; anything else must be one of the asteroid class's separator extras, and
     * only if {@code extrasAllowed}.
     */
    private static void assertProduced(GameTestHelper helper, Map<Item, Integer> produced, Map<Item, Integer> expected, Set<Item> bonus, boolean extrasAllowed) {
        for (Map.Entry<Item, Integer> entry : expected.entrySet()) {
            int got = produced.getOrDefault(entry.getKey(), 0);
            boolean ok = bonus.contains(entry.getKey()) && extrasAllowed ? got >= entry.getValue() : got == entry.getValue();
            helper.assertTrue(ok, BuiltInRegistries.ITEM.getKey(entry.getKey()) + ": expected " + entry.getValue() + ", got " + got);
        }
        for (Item item : produced.keySet()) {
            if (!expected.containsKey(item)) {
                helper.assertTrue(extrasAllowed && bonus.contains(item), "unexpected cargo " + BuiltInRegistries.ITEM.getKey(item));
            }
        }
    }

    private static int total(Map<Item, Integer> items, Set<Item> only) {
        return items.entrySet().stream().filter(e -> only.contains(e.getKey())).mapToInt(Map.Entry::getValue).sum();
    }

    @Nullable
    private static IChemicalHandler chemicalsFrom(GameTestHelper helper, BlockPos pos, Direction side) {
        return helper.getLevel().getCapability(Capabilities.CHEMICAL.block(), helper.absolutePos(pos), side);
    }

    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void separatorBoostsMission(GameTestHelper helper) {
        TileEntityCargoReceiver receiver = placeReceiver(helper);
        TileEntitySuperconductingSeparator separator = placeSeparator(helper);
        Cryostat cryostat = separator.getCryostat();
        chill(separator, Cryostat.COOLANT_CAPACITY);
        long[] mark = new long[2];
        helper.startSequence()
              .thenWaitUntil(() -> helper.assertTrue(cryostat.isSuperconducting() && Attribute.isActive(helper.getBlockState(SEPARATOR)),
                    "separator not superconducting and active: " + cryostat.getState()))
              .thenExecute(() -> {
                  helper.assertTrue(separator.isReceiverAdjacent(), "separator does not see the receiver");
                  long before = cryostat.coolantTank.getStored();
                  OrbitalAsset asset = launch(helper);
                  fastForward(helper, asset);
                  int batches = asset.miner().asteroid.batches();
                  helper.assertTrue(separator.getBatchesProcessed() == batches, "separated " + separator.getBatchesProcessed() + " of " + batches + " batches");
                  long used = before - cryostat.coolantTank.getStored();
                  helper.assertTrue(used == batches * TileEntitySuperconductingSeparator.COOLANT_PER_BATCH,
                        "coolant used for " + batches + " batches: " + used + " mB");

                  Map<Item, Integer> expected = expected(asset, batches);
                  Map<Item, Integer> produced = produced(receiver, asset);
                  assertProduced(helper, produced, expected, bonusItems(asset), true);
                  int cargo = asset.miner().asteroid.cargo().stream().mapToInt(CargoEntry::count).sum();
                  int base = total(produced, expected.keySet());
                  helper.assertTrue(base >= cargo * 1.49 && base <= cargo * 1.51, "separated " + base + " from " + cargo + " items of cargo");
                  decommission(helper, asset);

                  helper.assertTrue(cryostat.boilOffTank.getStack().is(CryoChemicals.NITROGEN)
                                    && cryostat.boilOffTank.getStored() >= (long) (used * Cryostat.BOIL_OFF_RETURN),
                        "boil off after " + used + " mB: " + cryostat.boilOffTank.getStack());
                  mark[0] = helper.getTick();
                  mark[1] = cryostat.coolantTank.getStored();
              })
              .thenIdle(20)
              .thenExecute(() -> {
                  // Idle again: only the trickle
                  helper.assertTrue(cryostat.getLastUsed() == TileEntitySuperconductingSeparator.TRICKLE, "idle coolant use: " + cryostat.getLastUsed());
                  long ticks = helper.getTick() - mark[0];
                  long drained = mark[1] - cryostat.coolantTank.getStored();
                  helper.assertTrue(Math.abs(drained - ticks * TileEntitySuperconductingSeparator.TRICKLE) <= 1,
                        "trickle drained " + drained + " mB in " + ticks + " ticks");
              })
              .thenSucceed();
    }

    @GameTest(template = ARENA)
    public static void drySeparatorPassesCargoThrough(GameTestHelper helper) {
        TileEntityCargoReceiver receiver = placeReceiver(helper);
        TileEntitySuperconductingSeparator separator = placeSeparator(helper);
        helper.runAfterDelay(2, () -> {
            helper.assertTrue(separator.getCryostat().getState() == CryostatState.NO_COOLANT, "dry separator is " + separator.getCryostat().getState());
            helper.assertFalse(Attribute.isActive(helper.getBlockState(SEPARATOR)), "dry separator is active");
            OrbitalAsset asset = launch(helper);
            fastForward(helper, asset);
            assertProduced(helper, produced(receiver, asset), expected(asset, 0), bonusItems(asset), false);
            helper.assertTrue(separator.getBatchesProcessed() == 0, "dry separator processed " + separator.getBatchesProcessed() + " batches");
            decommission(helper, asset);
            helper.succeed();
        });
    }

    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void runsDryAndQuenches(GameTestHelper helper) {
        TileEntityCargoReceiver receiver = placeReceiver(helper);
        TileEntitySuperconductingSeparator separator = placeSeparator(helper);
        Cryostat cryostat = separator.getCryostat();
        // Enough for one batch and a little trickle, not for a second batch
        chill(separator, TileEntitySuperconductingSeparator.COOLANT_PER_BATCH + 40);
        helper.startSequence()
              .thenWaitUntil(() -> helper.assertTrue(cryostat.isSuperconducting(), "not superconducting: " + cryostat.getState()))
              .thenExecute(() -> {
                  OrbitalAsset asset = launch(helper);
                  fastForward(helper, asset);
                  helper.assertTrue(separator.getBatchesProcessed() == 1, "separated " + separator.getBatchesProcessed() + " batches on coolant for one");
                  assertProduced(helper, produced(receiver, asset), expected(asset, 1), bonusItems(asset), true);
                  decommission(helper, asset);
              })
              .thenWaitUntil(() -> helper.assertTrue(cryostat.getState() == CryostatState.NO_COOLANT, "did not quench: " + cryostat.getState()))
              .thenExecute(() -> {
                  helper.assertTrue(cryostat.getCooldown() == Cryostat.COOLDOWN_TICKS, "quench did not reset the cool down: " + cryostat.getCooldown());
                  helper.assertTrue(cryostat.coolantTank.isEmpty(), "coolant left after quenching: " + cryostat.coolantTank.getStack());
              })
              .thenWaitUntil(() -> helper.assertFalse(Attribute.isActive(helper.getBlockState(SEPARATOR)), "quenched separator still active"))
              .thenSucceed();
    }

    @GameTest(template = ARENA)
    public static void cargoWaitingInOrbitGetsNoBonus(GameTestHelper helper) {
        TileEntityCargoReceiver receiver = placeReceiver(helper);
        TileEntitySuperconductingSeparator separator = placeSeparator(helper);
        Cryostat cryostat = separator.getCryostat();
        // Receiver off and separator dry while the miner works: every batch waits in orbit unseparated
        receiver.setControlType(RedstoneControl.HIGH);
        OrbitalAsset asset = launch(helper);
        fastForward(helper, asset);
        helper.assertTrue(produced(receiver, asset).equals(expected(asset, 0)), "cargo in orbit is not the plain asteroid cargo");
        helper.assertFalse(asset.miner().pending.isEmpty(), "nothing waiting in orbit");
        chill(separator, Cryostat.COOLANT_CAPACITY);
        helper.startSequence()
              .thenWaitUntil(() -> helper.assertTrue(cryostat.isSuperconducting(), "not superconducting: " + cryostat.getState()))
              .thenExecute(() -> {
                  long before = cryostat.coolantTank.getStored();
                  receiver.setControlType(RedstoneControl.DISABLED);
                  MinecraftServer server = helper.getLevel().getServer();
                  OrbitalRegistry.get(server).tick(server);
                  helper.assertTrue(asset.miner().pending.isEmpty(), "cargo still waiting: " + asset.miner().pending);
                  assertProduced(helper, produced(receiver, asset), expected(asset, 0), bonusItems(asset), false);
                  helper.assertTrue(separator.getBatchesProcessed() == 0 && cryostat.coolantTank.getStored() == before,
                        "waiting cargo was separated on delivery");
                  decommission(helper, asset);
              })
              .thenSucceed();
    }

    @GameTest(template = ARENA)
    public static void redstoneControlPausesSeparating(GameTestHelper helper) {
        placeReceiver(helper);
        TileEntitySuperconductingSeparator separator = placeSeparator(helper);
        Cryostat cryostat = separator.getCryostat();
        chill(separator, Cryostat.COOLANT_CAPACITY);
        helper.startSequence()
              .thenWaitUntil(() -> helper.assertTrue(cryostat.isSuperconducting(), "not superconducting: " + cryostat.getState()))
              .thenExecute(() -> {
                  OrbitalAsset asset = launch(helper);
                  List<CargoEntry> batch = asset.miner().asteroid.batch(0);
                  separator.setControlType(RedstoneControl.HIGH);
                  long before = cryostat.coolantTank.getStored();
                  helper.assertTrue(separator.processBatch(batch, asset, helper.getLevel().getRandom()) == null, "separated while switched off by redstone");
                  helper.assertTrue(cryostat.coolantTank.getStored() == before, "used coolant while switched off");
                  separator.setControlType(RedstoneControl.DISABLED);
                  helper.assertTrue(separator.processBatch(batch, asset, helper.getLevel().getRandom()) != null, "did not separate once back on");
                  helper.assertTrue(before - cryostat.coolantTank.getStored() == TileEntitySuperconductingSeparator.COOLANT_PER_BATCH,
                        "one batch used " + (before - cryostat.coolantTank.getStored()) + " mB");
                  decommission(helper, asset);
              })
              .thenSucceed();
    }

    @GameTest(template = ARENA)
    public static void chemicalSidesAndBoilOffEject(GameTestHelper helper) {
        TileEntitySuperconductingSeparator separator = placeSeparator(helper);
        BlockPos above = SEPARATOR.above();
        // Liquid nitrogen goes in from the sides, never from the boil off output on top
        IChemicalHandler side = chemicalsFrom(helper, SEPARATOR, Direction.NORTH);
        helper.assertTrue(side != null, "no chemical input on the side");
        ChemicalStack rest = side.insertChemical(new ChemicalStack(CryoChemicals.LIQUID_NITROGEN, 2_000), Action.EXECUTE);
        helper.assertTrue(rest.isEmpty() && separator.getCryostat().coolantTank.getStored() == 2_000, "side input refused liquid nitrogen: " + rest);
        helper.assertTrue(!side.insertChemical(new ChemicalStack(CryoChemicals.NITROGEN, 100), Action.SIMULATE).isEmpty(), "coolant input takes nitrogen gas");
        IChemicalHandler top = chemicalsFrom(helper, SEPARATOR, Direction.UP);
        helper.assertTrue(top != null, "no boil off output on top");
        helper.assertTrue(!top.insertChemical(new ChemicalStack(CryoChemicals.LIQUID_NITROGEN, 100), Action.SIMULATE).isEmpty(), "top output accepts coolant");

        // The trickle boils off; auto eject pushes the nitrogen up into a Mekanism tube
        Block tube = BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath(MekanismAPI.MEKANISM_MODID, "basic_pressurized_tube"));
        helper.assertTrue(tube != Blocks.AIR, "Mekanism's basic pressurized tube is missing");
        helper.setBlock(above, tube);
        helper.succeedWhen(() -> {
            IChemicalHandler tubeHandler = chemicalsFrom(helper, above, Direction.DOWN);
            helper.assertTrue(tubeHandler != null, "tube exposes no chemical handler");
            ChemicalStack stored = tubeHandler.getChemicalInTank(0);
            helper.assertTrue(stored.is(CryoChemicals.NITROGEN) && stored.getAmount() > 0, "tube holds " + stored);
        });
    }

    @GameTest(template = ARENA)
    public static void keepsCoolantWhenBroken(GameTestHelper helper) {
        TileEntitySuperconductingSeparator separator = placeSeparator(helper);
        separator.getCryostat().coolantTank.setStack(new ChemicalStack(CryoChemicals.LIQUID_NITROGEN, 3_000));
        separator.getCryostat().boilOffTank.setStack(new ChemicalStack(CryoChemicals.NITROGEN, 500));
        BlockPos pos = helper.absolutePos(SEPARATOR);
        List<ItemStack> drops = Block.getDrops(helper.getBlockState(SEPARATOR), helper.getLevel(), pos, separator);
        helper.assertTrue(drops.size() == 1 && drops.getFirst().is(CryoBlocks.SUPERCONDUCTING_SEPARATOR.asItem()), "drops: " + drops);
        AttachedChemicals chemicals = drops.getFirst().get(MekanismDataComponents.ATTACHED_CHEMICALS);
        helper.assertTrue(chemicals != null && chemicals.containers().size() == 2, "dropped item has no tanks: " + chemicals);
        helper.assertTrue(chemicals.containers().get(0).is(CryoChemicals.LIQUID_NITROGEN) && chemicals.containers().get(0).getAmount() == 3_000,
              "coolant on the item: " + chemicals.containers().get(0));
        helper.assertTrue(chemicals.containers().get(1).is(CryoChemicals.NITROGEN) && chemicals.containers().get(1).getAmount() == 500,
              "boil off on the item: " + chemicals.containers().get(1));
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void separatorRecipesLoad(GameTestHelper helper) {
        for (String id : List.of("superconducting_coil", "superconducting_separator")) {
            helper.assertTrue(helper.getLevel().getRecipeManager().byKey(CryoSat.rl(id)).isPresent(), id + " recipe missing");
        }
        helper.succeed();
    }
}
