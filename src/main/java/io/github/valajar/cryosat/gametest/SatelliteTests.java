package io.github.valajar.cryosat.gametest;

import com.mojang.datafixers.util.Either;
import io.github.valajar.cryosat.CryoSat;
import io.github.valajar.cryosat.space.AsteroidClass;
import io.github.valajar.cryosat.space.Constellation;
import io.github.valajar.cryosat.space.MinerState;
import io.github.valajar.cryosat.space.MissionProblem;
import io.github.valajar.cryosat.space.OrbitalAsset;
import io.github.valajar.cryosat.space.OrbitalRegistry;
import io.github.valajar.cryosat.space.OrbitalTask;
import io.github.valajar.cryosat.space.PayloadType;
import io.github.valajar.cryosat.space.RocketDesign;
import io.github.valajar.cryosat.space.RocketTier;
import io.github.valajar.cryosat.space.SatelliteState;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Milestone 3 foundation: payload states and old saves, Survey Satellites in Earth orbit, constellations and imaging tasks.
 */
@GameTestHolder(CryoSat.MODID)
@PrefixGameTestTemplate(false)
public class SatelliteTests {
    private static final ResourceLocation EARTH_ORBIT = CryoSat.rl("earth_orbit");

    private static RocketDesign satellite(RocketTier tier) {
        return new RocketDesign(tier, PayloadType.SURVEY_SATELLITE, tier);
    }

    private static OrbitalAsset launchSatellite(GameTestHelper helper, UUID owner, RocketTier tier) {
        MinecraftServer server = helper.getLevel().getServer();
        Either<OrbitalAsset, MissionProblem> result = OrbitalRegistry.get(server).launch(server, owner, satellite(tier), EARTH_ORBIT, null, Level.OVERWORLD);
        helper.assertTrue(result.left().isPresent(), "satellite launch failed: " + result);
        OrbitalAsset asset = result.left().get();
        asset.nextEventAt = 0;
        OrbitalRegistry.get(server).tick(server);
        helper.assertTrue(asset.state == OrbitalAsset.State.ORBITING, "satellite did not reach orbit: " + asset.state);
        return asset;
    }

    @GameTest(template = "empty")
    public static void oldMinerSavesLoad(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        RocketDesign miner = new RocketDesign(RocketTier.BASIC, PayloadType.ASTEROID_MINER, RocketTier.BASIC);
        OrbitalAsset asset = OrbitalRegistry.get(server).launch(server, UUID.randomUUID(), miner, AsteroidClass.ASTEROID_BELT, null).left().orElseThrow();
        asset.miner().batchesDelivered = 2;
        var ops = helper.getLevel().registryAccess().createSerializationContext(NbtOps.INSTANCE);
        CompoundTag tag = (CompoundTag) OrbitalAsset.CODEC.encodeStart(ops, asset).getOrThrow();
        // Rewrite into the layout saved before payload states: miner fields on the asset itself
        CompoundTag payload = tag.getCompound("payload");
        tag.remove("payload");
        for (String key : new String[]{"asteroid", "batches_delivered", "pending"}) {
            Tag value = payload.get(key);
            if (value != null) {
                tag.put(key, value);
            }
        }
        OrbitalAsset loaded = OrbitalAsset.CODEC.parse(ops, tag).getOrThrow();
        MinerState state = loaded.miner();
        helper.assertTrue(state != null && state.asteroid != null && state.asteroid.equals(asset.miner().asteroid) && state.batchesDelivered == 2,
              "old save did not load as a miner: " + loaded.payload);
        OrbitalRegistry.get(server).decommission(asset);
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void earthOrbitOnlyFromTheOverworld(GameTestHelper helper) {
        helper.assertTrue(OrbitalRegistry.check(satellite(RocketTier.BASIC), EARTH_ORBIT, Level.OVERWORLD) == null, "basic satellite cannot reach Earth orbit");
        helper.assertTrue(OrbitalRegistry.check(satellite(RocketTier.BASIC), EARTH_ORBIT, Level.NETHER) == MissionProblem.WRONG_DIMENSION, "Earth orbit from the Nether");
        helper.assertTrue(OrbitalRegistry.check(satellite(RocketTier.BASIC), EARTH_ORBIT, Level.END) == MissionProblem.WRONG_DIMENSION, "Earth orbit from the End");
        RocketDesign miner = new RocketDesign(RocketTier.BASIC, PayloadType.ASTEROID_MINER, RocketTier.BASIC);
        helper.assertTrue(OrbitalRegistry.check(miner, EARTH_ORBIT, Level.OVERWORLD) == MissionProblem.PAYLOAD_NOT_USABLE, "miner accepted for Earth orbit");
        helper.assertTrue(OrbitalRegistry.check(satellite(RocketTier.BASIC), AsteroidClass.ASTEROID_BELT, Level.OVERWORLD) == MissionProblem.PAYLOAD_NOT_USABLE,
              "satellite accepted for the asteroid belt");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void coverageMaths(GameTestHelper helper) {
        helper.assertTrue(Math.abs(Constellation.coverageFor(1) - 0.1175) < 0.001, "one basic satellite: " + Constellation.coverageFor(1));
        helper.assertTrue(Constellation.uncertaintyFor(Constellation.coverageFor(12)) == 114, "three elite: " + Constellation.uncertaintyFor(Constellation.coverageFor(12)));
        helper.assertTrue(Constellation.uncertaintyFor(Constellation.coverageFor(24)) == 0, "six elite should be exact");
        helper.assertTrue(Constellation.coverageFor(100) < 1, "coverage reached 100 percent");
        int[] values = new int[RocketTier.values().length];
        for (RocketTier tier : RocketTier.values()) {
            values[tier.ordinal()] = SatelliteState.coverageValue(tier);
        }
        helper.assertTrue(values[0] == 1 && values[1] == 2 && values[2] == 4 && values[3] == 8, "tier values");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void constellationAndTasks(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        OrbitalRegistry registry = OrbitalRegistry.get(server);
        UUID owner = UUID.randomUUID();
        OrbitalAsset first = launchSatellite(helper, owner, RocketTier.BASIC);
        OrbitalAsset second = launchSatellite(helper, owner, RocketTier.ELITE);

        Constellation constellation = Constellation.of(server, owner, Level.OVERWORLD);
        helper.assertTrue(constellation.satellites().size() == 2 && constellation.value() == 5, "constellation: " + constellation.satellites().size() + " value " + constellation.value());
        helper.assertTrue(constellation.range() > 1_000 && constellation.uncertainty() > 0, "range " + constellation.range() + ", uncertainty " + constellation.uncertainty());
        helper.assertTrue(Constellation.of(server, UUID.randomUUID(), Level.OVERWORLD).satellites().isEmpty(), "a stranger can use the satellites");
        helper.assertTrue(Constellation.of(server, owner, Level.NETHER).satellites().isEmpty(), "satellites over the Nether");

        GlobalPos terminal = GlobalPos.of(helper.getLevel().dimension(), helper.absolutePos(BlockPos.ZERO));
        Optional<OrbitalTask> a = registry.requestSurvey(server, owner, terminal, CryoSat.rl("village"));
        Optional<OrbitalTask> b = registry.requestSurvey(server, owner, terminal, CryoSat.rl("village"));
        Optional<OrbitalTask> c = registry.requestSurvey(server, owner, terminal, CryoSat.rl("village"));
        helper.assertTrue(a.isPresent() && b.isPresent() && c.isEmpty(), "two satellites should take two passes at once");
        helper.assertTrue(Constellation.of(server, owner, Level.OVERWORLD).free() == 0, "satellites still free");

        // No terminal at the position: completed passes are dropped and the satellites are free again
        for (OrbitalAsset asset : new OrbitalAsset[]{first, second}) {
            OrbitalTask task = asset.satellite().task;
            asset.satellite().task = new OrbitalTask(task.id(), task.requester(), task.terminal(), task.target(), 0);
        }
        registry.tick(server);
        helper.assertTrue(first.satellite().isFree() && second.satellite().isFree(), "passes for a missing terminal were kept");
        registry.decommission(first);
        registry.decommission(second);
        helper.succeed();
    }
}
