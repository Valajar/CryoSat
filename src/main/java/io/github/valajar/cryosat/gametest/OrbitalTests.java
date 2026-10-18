package io.github.valajar.cryosat.gametest;

import com.mojang.datafixers.util.Either;
import io.github.valajar.cryosat.CryoSat;
import io.github.valajar.cryosat.registries.CryoItems;
import io.github.valajar.cryosat.space.AsteroidClass;
import io.github.valajar.cryosat.space.CargoEntry;
import io.github.valajar.cryosat.space.MissionProblem;
import io.github.valajar.cryosat.space.OrbitalAsset;
import io.github.valajar.cryosat.space.OrbitalRegistry;
import io.github.valajar.cryosat.space.PayloadType;
import io.github.valajar.cryosat.space.RocketDesign;
import io.github.valajar.cryosat.space.RocketTier;
import io.github.valajar.cryosat.space.SpaceData;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.NbtOps;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Orbital registry: data loading, rocket assembly rules, and a full Asteroid Miner mission delivering into a chest.
 * Missions are fast forwarded by resetting their next event time instead of waiting.
 */
@GameTestHolder(CryoSat.MODID)
@PrefixGameTestTemplate(false)
public class OrbitalTests {
    private static final RocketDesign BASIC_MINER = new RocketDesign(RocketTier.BASIC, PayloadType.ASTEROID_MINER, RocketTier.BASIC);

    private static ItemStack stack(Item item) {
        return new ItemStack(item);
    }

    @GameTest(template = "empty")
    public static void spaceDataLoads(GameTestHelper helper) {
        helper.assertTrue(SpaceData.DESTINATIONS.get(AsteroidClass.ASTEROID_BELT) != null, "asteroid belt destination missing");
        AsteroidClass chondrite = SpaceData.ASTEROIDS.get(CryoSat.rl("chondrite"));
        helper.assertTrue(chondrite != null, "chondrite asteroid class missing");
        var iron = chondrite.yields().getFirst().resolve();
        helper.assertTrue(iron.isPresent() && iron.get().value() == Items.RAW_IRON, "iron tag resolved to " + iron);
        for (AsteroidClass.Yield yield : chondrite.yields()) {
            helper.assertTrue(yield.resolve().isPresent(), "yield " + yield.item() + " did not resolve");
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void assemblyRules(GameTestHelper helper) {
        Item guidance = CryoItems.GUIDANCE_COMPUTER.get();
        Either<RocketDesign, MissionProblem> mixed = RocketDesign.assemble(stack(CryoItems.ROCKET_ENGINES.get(RocketTier.ADVANCED).get()),
              stack(CryoItems.FUEL_TANKS.get(RocketTier.BASIC).get()), stack(guidance), stack(CryoItems.ASTEROID_MINERS.get(RocketTier.BASIC).get()));
        helper.assertTrue(mixed.left().equals(java.util.Optional.of(BASIC_MINER)), "mixed tiers should make a basic rocket: " + mixed);

        Either<RocketDesign, MissionProblem> tooAdvanced = RocketDesign.assemble(stack(CryoItems.ROCKET_ENGINES.get(RocketTier.BASIC).get()),
              stack(CryoItems.FUEL_TANKS.get(RocketTier.BASIC).get()), stack(guidance), stack(CryoItems.ASTEROID_MINERS.get(RocketTier.ELITE).get()));
        helper.assertTrue(tooAdvanced.right().equals(java.util.Optional.of(MissionProblem.PAYLOAD_TOO_ADVANCED)), "expected payload too advanced: " + tooAdvanced);

        Either<RocketDesign, MissionProblem> missing = RocketDesign.assemble(ItemStack.EMPTY, stack(CryoItems.FUEL_TANKS.get(RocketTier.BASIC).get()),
              stack(guidance), stack(CryoItems.ASTEROID_MINERS.get(RocketTier.BASIC).get()));
        helper.assertTrue(missing.right().equals(java.util.Optional.of(MissionProblem.MISSING_ENGINE)), "expected missing engine: " + missing);

        Either<RocketDesign, MissionProblem> ultimate = RocketDesign.assemble(stack(CryoItems.ROCKET_ENGINES.get(RocketTier.ULTIMATE).get()),
              stack(CryoItems.FUEL_TANKS.get(RocketTier.ULTIMATE).get()), stack(guidance), stack(CryoItems.ASTEROID_MINERS.get(RocketTier.BASIC).get()));
        helper.assertTrue(ultimate.right().equals(java.util.Optional.of(MissionProblem.TIER_CANNOT_LAUNCH)), "ultimate should not launch yet: " + ultimate);

        helper.assertTrue(OrbitalRegistry.check(BASIC_MINER, CryoSat.rl("nowhere")) == MissionProblem.UNKNOWN_DESTINATION, "unknown destination accepted");
        helper.assertTrue(OrbitalRegistry.check(BASIC_MINER, AsteroidClass.ASTEROID_BELT) == null, "basic miner cannot reach the asteroid belt");
        helper.succeed();
    }

    private static OrbitalAsset launch(GameTestHelper helper, BlockPos receiver) {
        MinecraftServer server = helper.getLevel().getServer();
        GlobalPos target = GlobalPos.of(helper.getLevel().dimension(), helper.absolutePos(receiver));
        Either<OrbitalAsset, MissionProblem> result = OrbitalRegistry.get(server).launch(server, UUID.randomUUID(), BASIC_MINER, AsteroidClass.ASTEROID_BELT, target);
        helper.assertTrue(result.left().isPresent(), "launch failed: " + result);
        return result.left().get();
    }

    /** Skips travel and every batch. */
    private static void fastForward(GameTestHelper helper, OrbitalAsset asset) {
        MinecraftServer server = helper.getLevel().getServer();
        OrbitalRegistry registry = OrbitalRegistry.get(server);
        for (int i = 0; i < 20 && asset.state != OrbitalAsset.State.RETIRED; i++) {
            asset.nextEventAt = 0;
            registry.tick(server);
        }
    }

    @GameTest(template = "empty")
    public static void minerDeliversEveryBatch(GameTestHelper helper) {
        helper.setBlock(BlockPos.ZERO, Blocks.CHEST);
        OrbitalAsset asset = launch(helper, BlockPos.ZERO);
        helper.assertTrue(asset.asteroid != null, "miner has no asteroid");
        fastForward(helper, asset);
        helper.assertTrue(asset.state == OrbitalAsset.State.RETIRED, "miner did not retire: " + asset.state);
        helper.assertTrue(asset.batchesDelivered == asset.asteroid.batches(), "delivered " + asset.batchesDelivered + " of " + asset.asteroid.batches());
        helper.assertTrue(asset.pending.isEmpty(), "cargo left over: " + asset.pending);

        Map<Item, Integer> received = new HashMap<>();
        ChestBlockEntity chest = helper.getBlockEntity(BlockPos.ZERO);
        for (int slot = 0; slot < chest.getContainerSize(); slot++) {
            ItemStack stack = chest.getItem(slot);
            if (!stack.isEmpty()) {
                received.merge(stack.getItem(), stack.getCount(), Integer::sum);
            }
        }
        for (CargoEntry entry : asset.asteroid.cargo()) {
            int got = received.getOrDefault(entry.item().value(), 0);
            helper.assertTrue(got == entry.count(), BuiltInRegistries.ITEM.getKey(entry.item().value()) + ": expected " + entry.count() + ", got " + got);
        }
        int iron = received.getOrDefault(Items.RAW_IRON, 0);
        helper.assertTrue(iron >= 384 && iron <= 640, "raw iron outside the small asteroid range: " + iron);
        OrbitalRegistry.get(helper.getLevel().getServer()).decommission(asset);
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void cargoWaitsForReceiver(GameTestHelper helper) {
        OrbitalAsset asset = launch(helper, BlockPos.ZERO);
        fastForward(helper, asset);
        helper.assertFalse(asset.pending.isEmpty(), "cargo vanished without a receiver");
        helper.setBlock(BlockPos.ZERO, Blocks.CHEST);
        OrbitalRegistry.get(helper.getLevel().getServer()).tick(helper.getLevel().getServer());
        helper.assertTrue(asset.pending.isEmpty(), "cargo not delivered once the receiver appeared: " + asset.pending);
        OrbitalRegistry.get(helper.getLevel().getServer()).decommission(asset);
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void assetSurvivesSaving(GameTestHelper helper) {
        OrbitalAsset asset = launch(helper, BlockPos.ZERO);
        var ops = helper.getLevel().registryAccess().createSerializationContext(NbtOps.INSTANCE);
        var encoded = OrbitalAsset.CODEC.encodeStart(ops, asset).getOrThrow();
        OrbitalAsset decoded = OrbitalAsset.CODEC.parse(ops, encoded).getOrThrow();
        helper.assertTrue(decoded.id.equals(asset.id) && decoded.design.equals(asset.design) && decoded.state == asset.state
                          && decoded.asteroid.equals(asset.asteroid) && decoded.number == asset.number, "asset changed after saving");
        OrbitalRegistry.get(helper.getLevel().getServer()).decommission(asset);
        helper.succeed();
    }
}
