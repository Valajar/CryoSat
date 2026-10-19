package io.github.valajar.cryosat.gametest;

import com.mojang.datafixers.util.Either;
import io.github.valajar.cryosat.CryoSat;
import io.github.valajar.cryosat.registries.CryoBlocks;
import io.github.valajar.cryosat.registries.CryoDataComponents;
import io.github.valajar.cryosat.space.Constellation;
import io.github.valajar.cryosat.space.LocatorData;
import io.github.valajar.cryosat.space.MissionProblem;
import io.github.valajar.cryosat.space.OrbitalAsset;
import io.github.valajar.cryosat.space.OrbitalRegistry;
import io.github.valajar.cryosat.space.OrbitalTask;
import io.github.valajar.cryosat.space.PayloadType;
import io.github.valajar.cryosat.space.RocketDesign;
import io.github.valajar.cryosat.space.RocketTier;
import io.github.valajar.cryosat.space.SurveyTarget;
import io.github.valajar.cryosat.space.survey.RequestResult;
import io.github.valajar.cryosat.space.survey.SurveyResult;
import io.github.valajar.cryosat.space.survey.SurveySearch;
import io.github.valajar.cryosat.space.survey.TileEntityImagingTerminal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ExtraCodecs.TagOrElementLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.structure.BuiltinStructures;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Imaging Terminal with the real block: requests queue passes and cost energy, completed passes are searched and logged,
 * deep targets need a module, results wait when the search budget is used up, redstone repeats the last request.
 * <p>
 * The test world is a superflat world with structure generation off. Its only structure placements are villages and
 * strongholds, so the targets here are defined in code: a desert pyramid target (no placement at all, so every search ends at
 * once with nothing found), a plains village target whose start {@link #searchFindsPlacedStructureOnce} plants by hand (what
 * {@code /place structure} generates, registered in its chunk like world generation would) and an ancient city deep target.
 */
@GameTestHolder(CryoSat.MODID)
@PrefixGameTestTemplate(false)
public class ImagingTerminalTests {
    private static final ResourceLocation EARTH_ORBIT = CryoSat.rl("earth_orbit");
    private static final ResourceLocation PYRAMID = CryoSat.rl("test_pyramid");
    private static final ResourceLocation VILLAGE = CryoSat.rl("test_village");
    private static final ResourceLocation DEEP = CryoSat.rl("test_deep");
    private static final BlockPos CENTER = new BlockPos(1, 1, 1);

    private static void addTargets() {
        SurveySearch.addTestTarget(PYRAMID, new SurveyTarget(new TagOrElementLocation(BuiltinStructures.DESERT_PYRAMID.location(), false), false,
              ResourceLocation.withDefaultNamespace("sandstone")));
        SurveySearch.addTestTarget(VILLAGE, new SurveyTarget(new TagOrElementLocation(BuiltinStructures.VILLAGE_PLAINS.location(), false), false,
              ResourceLocation.withDefaultNamespace("bell")));
        SurveySearch.addTestTarget(DEEP, new SurveyTarget(new TagOrElementLocation(BuiltinStructures.ANCIENT_CITY.location(), false), true,
              ResourceLocation.withDefaultNamespace("sculk")));
    }

    private static TileEntityImagingTerminal place(GameTestHelper helper, BlockPos pos, boolean powered) {
        addTargets();
        helper.setBlock(pos, CryoBlocks.IMAGING_TERMINAL.get().defaultBlockState());
        TileEntityImagingTerminal terminal = helper.getBlockEntity(pos);
        if (powered) {
            terminal.getEnergyContainer().setEnergy(terminal.getEnergyContainer().getMaxEnergy());
        }
        return terminal;
    }

    private static List<OrbitalAsset> launch(GameTestHelper helper, UUID owner, RocketTier tier, int count) {
        MinecraftServer server = helper.getLevel().getServer();
        OrbitalRegistry registry = OrbitalRegistry.get(server);
        List<OrbitalAsset> satellites = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            Either<OrbitalAsset, MissionProblem> result = registry.launch(server, owner, new RocketDesign(tier, PayloadType.SURVEY_SATELLITE, tier), EARTH_ORBIT,
                  null, Level.OVERWORLD);
            helper.assertTrue(result.left().isPresent(), "satellite launch failed: " + result);
            OrbitalAsset asset = result.left().get();
            asset.nextEventAt = 0;
            satellites.add(asset);
        }
        registry.tick(server);
        return satellites;
    }

    /** Makes the satellite's pass due now. */
    private static void finishPass(GameTestHelper helper, OrbitalAsset satellite) {
        OrbitalTask task = satellite.satellite().task;
        helper.assertTrue(task != null, "satellite has no pass");
        satellite.satellite().task = new OrbitalTask(task.id(), task.requester(), task.terminal(), task.target(), 0);
    }

    private static void decommission(GameTestHelper helper, List<OrbitalAsset> satellites) {
        OrbitalRegistry registry = OrbitalRegistry.get(helper.getLevel().getServer());
        satellites.forEach(registry::decommission);
    }

    @GameTest(template = "empty")
    public static void requestQueuesPassAndLogsResult(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        UUID owner = UUID.randomUUID();
        TileEntityImagingTerminal terminal = place(helper, BlockPos.ZERO, false);
        helper.assertTrue(terminal.request(owner, PYRAMID) == RequestResult.NO_SATELLITES, "request without satellites");
        List<OrbitalAsset> satellites = launch(helper, owner, RocketTier.BASIC, 1);
        OrbitalAsset satellite = satellites.getFirst();

        helper.assertTrue(terminal.request(owner, PYRAMID) == RequestResult.NO_POWER, "request without energy");
        helper.assertTrue(satellite.satellite().isFree(), "unpowered request took the satellite");
        terminal.getEnergyContainer().setEnergy(terminal.getEnergyContainer().getMaxEnergy());
        long before = terminal.getEnergyContainer().getEnergy();
        long cost = terminal.requestCost(Constellation.of(server, owner, Level.OVERWORLD).range());
        helper.assertTrue(cost > TileEntityImagingTerminal.BASE_ENERGY_PER_REQUEST, "cost should grow with range: " + cost);

        helper.assertTrue(terminal.request(owner, PYRAMID) == RequestResult.REQUESTED, "request refused");
        OrbitalTask task = satellite.satellite().task;
        GlobalPos self = GlobalPos.of(helper.getLevel().dimension(), helper.absolutePos(BlockPos.ZERO));
        helper.assertTrue(task != null && task.terminal().equals(self) && task.target().equals(PYRAMID), "no pass queued for this terminal: " + task);
        helper.assertTrue(before - terminal.getEnergyContainer().getEnergy() == cost,
              "energy used " + (before - terminal.getEnergyContainer().getEnergy()) + ", expected " + cost);
        helper.assertTrue(terminal.passes().size() == 1, "passes in progress: " + terminal.passes());
        helper.assertTrue(terminal.request(owner, PYRAMID) == RequestResult.ALL_BUSY, "second request with one satellite");
        helper.assertTrue(terminal.request(owner, CryoSat.rl("no_such_target")) == RequestResult.UNKNOWN_TARGET, "unknown target accepted");

        finishPass(helper, satellite);
        helper.succeedWhen(() -> {
            OrbitalRegistry.get(server).tick(server);
            helper.assertTrue(terminal.getLog().size() == 1, "log: " + terminal.getLog());
            SurveyResult result = terminal.getLog().getFirst();
            helper.assertTrue(result.id().equals(task.id()) && result.target().equals(PYRAMID) && result.outcome() == SurveyResult.Outcome.NOT_FOUND,
                  "result: " + result);
            helper.assertTrue(satellite.satellite().isFree() && terminal.passes().isEmpty(), "pass still in progress");
            decommission(helper, satellites);
        });
    }

    @GameTest(template = "empty")
    public static void deepTargetsNeedAModule(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        UUID owner = UUID.randomUUID();
        TileEntityImagingTerminal terminal = place(helper, BlockPos.ZERO, true);
        List<OrbitalAsset> satellites = launch(helper, owner, RocketTier.BASIC, 1);
        long energy = terminal.getEnergyContainer().getEnergy();
        helper.assertTrue(terminal.request(owner, DEEP) == RequestResult.NEEDS_DEEP_SCAN, "deep target requested without a SQUID Array");
        helper.assertTrue(satellites.getFirst().satellite().isFree() && terminal.getEnergyContainer().getEnergy() == energy, "refused request used something");

        // A deep pass that arrives with no module next to the terminal is logged as such, not searched
        GlobalPos self = GlobalPos.of(helper.getLevel().dimension(), helper.absolutePos(BlockPos.ZERO));
        helper.assertTrue(OrbitalRegistry.get(server).requestSurvey(server, owner, self, DEEP).isPresent(), "no pass queued");
        finishPass(helper, satellites.getFirst());
        helper.succeedWhen(() -> {
            OrbitalRegistry.get(server).tick(server);
            helper.assertTrue(terminal.getLog().size() == 1 && terminal.getLog().getFirst().outcome() == SurveyResult.Outcome.NO_DEEP_SCAN, "log: " + terminal.getLog());
            decommission(helper, satellites);
        });
    }

    @GameTest(template = "empty")
    public static void resultsWaitUntilTheTerminalCanTakeThem(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        OrbitalRegistry registry = OrbitalRegistry.get(server);
        UUID owner = UUID.randomUUID();
        TileEntityImagingTerminal terminal = place(helper, BlockPos.ZERO, true);
        List<OrbitalAsset> satellites = launch(helper, owner, RocketTier.BASIC, 2);
        helper.assertTrue(terminal.request(owner, PYRAMID) == RequestResult.REQUESTED, "request refused");
        OrbitalAsset busy = satellites.stream().filter(asset -> !asset.satellite().isFree()).findFirst().orElseThrow();
        // A second pass for a terminal in a chunk that is not loaded
        GlobalPos far = GlobalPos.of(helper.getLevel().dimension(), helper.absolutePos(BlockPos.ZERO).offset(1_000_000, 0, 1_000_000));
        Optional<OrbitalTask> farTask = registry.requestSurvey(server, owner, far, PYRAMID);
        helper.assertTrue(farTask.isPresent(), "no pass for the far terminal");
        OrbitalAsset unloaded = satellites.stream().filter(asset -> asset != busy).findFirst().orElseThrow();
        finishPass(helper, busy);
        finishPass(helper, unloaded);

        // This tick's searches are used up: the terminal turns the result away and the satellite keeps it
        while (SurveySearch.tryStartSearch(server)) {
            // use up the budget
        }
        registry.tick(server);
        helper.assertTrue(!busy.satellite().isFree() && terminal.getLog().isEmpty(), "result taken without a search slot");
        helper.assertTrue(unloaded.satellite().task != null && unloaded.satellite().task.id().equals(farTask.get().id()), "result for an unloaded terminal dropped");

        helper.succeedWhen(() -> {
            registry.tick(server);
            helper.assertTrue(terminal.getLog().size() == 1 && busy.satellite().isFree(), "result not delivered on a later tick: " + terminal.getLog());
            helper.assertTrue(unloaded.satellite().task != null, "result for an unloaded terminal dropped");
            decommission(helper, satellites);
        });
    }

    @GameTest(template = "empty_3x3x3")
    public static void redstoneRepeatsLastRequest(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        UUID owner = UUID.randomUUID();
        TileEntityImagingTerminal terminal = place(helper, CENTER, true);
        List<OrbitalAsset> satellites = launch(helper, owner, RocketTier.BASIC, 2);
        helper.assertTrue(terminal.request(owner, PYRAMID) == RequestResult.REQUESTED, "request refused");
        helper.assertTrue(Constellation.of(server, owner, Level.OVERWORLD).free() == 1, "one satellite should still be free");
        helper.setBlock(CENTER.west(), Blocks.REDSTONE_BLOCK);
        helper.succeedWhen(() -> {
            helper.assertTrue(Constellation.of(server, owner, Level.OVERWORLD).free() == 0, "redstone did not repeat the request");
            helper.assertTrue(satellites.stream().allMatch(asset -> PYRAMID.equals(asset.satellite().task.target())), "repeated a different target");
            decommission(helper, satellites);
        });
    }

    @GameTest(template = "empty")
    public static void blurIsStableAndInsideTheRadius(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1000, 0, -2000);
        helper.assertTrue(SurveySearch.blur(pos, 0, UUID.randomUUID()).equals(pos), "exact results must not move");
        for (int i = 0; i < 50; i++) {
            UUID seed = UUID.randomUUID();
            BlockPos blurred = SurveySearch.blur(pos, 300, seed);
            helper.assertTrue(blurred.equals(SurveySearch.blur(pos, 300, seed)), "blur differs for the same pass");
            double distance = Math.hypot(blurred.getX() - pos.getX(), blurred.getZ() - pos.getZ());
            helper.assertTrue(distance <= 301, "blurred " + distance + " blocks for a 300 block radius");
        }
        helper.succeed();
    }

    /**
     * Plants a plains village start where vanilla's search looks first (the village placement's candidate chunk in the
     * terminal's grid cell), as world generation would have. Returns where a search reports it.
     */
    private static BlockPos plantVillage(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Holder<Structure> village = level.registryAccess().registryOrThrow(Registries.STRUCTURE).getHolderOrThrow(BuiltinStructures.VILLAGE_PLAINS);
        ChunkGeneratorStructureState state = level.getChunkSource().getGeneratorState();
        RandomSpreadStructurePlacement placement = (RandomSpreadStructurePlacement) state.getPlacementsForStructure(village).getFirst();
        ChunkPos terminalChunk = new ChunkPos(helper.absolutePos(BlockPos.ZERO));
        ChunkPos chunkPos = placement.getPotentialStructureChunk(state.getLevelSeed(), terminalChunk.x, terminalChunk.z);
        ChunkGenerator generator = level.getChunkSource().getGenerator();
        StructureStart start = village.value().generate(level.registryAccess(), generator, generator.getBiomeSource(), level.getChunkSource().randomState(),
              level.getStructureManager(), level.getSeed(), chunkPos, 0, level, biome -> true);
        helper.assertTrue(start.isValid(), "could not generate a village");
        LevelChunk chunk = level.getChunk(chunkPos.x, chunkPos.z);
        chunk.setStartForStructure(village.value(), start);
        level.onStructureStartsAvailable(chunk);
        return placement.getLocatePos(chunkPos);
    }

    @GameTest(template = "empty", timeoutTicks = 400)
    public static void searchFindsPlacedStructureOnce(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        OrbitalRegistry registry = OrbitalRegistry.get(server);
        UUID owner = UUID.randomUUID();
        TileEntityImagingTerminal terminal = place(helper, BlockPos.ZERO, true);
        SurveySearch.searchWithoutStructureGeneration = true;
        BlockPos village = plantVillage(helper);
        // Three Ultimate satellites: 95 percent coverage, so the result is exact
        List<OrbitalAsset> ultimates = launch(helper, owner, RocketTier.ULTIMATE, 3);
        helper.assertTrue(Constellation.of(server, owner, Level.OVERWORLD).uncertainty() == 0, "constellation should give exact results");
        helper.assertTrue(terminal.request(owner, VILLAGE) == RequestResult.REQUESTED, "request refused");
        ultimates.stream().filter(asset -> !asset.satellite().isFree()).forEach(asset -> finishPass(helper, asset));

        helper.startSequence()
              .thenWaitUntil(() -> {
                  registry.tick(server);
                  helper.assertTrue(terminal.getLog().size() == 1, "first result not in");
              })
              .thenExecute(() -> {
                  SurveyResult first = terminal.getLog().getFirst();
                  helper.assertTrue(first.outcome() == SurveyResult.Outcome.FOUND && first.uncertainty() == 0, "first result: " + first);
                  helper.assertTrue(first.pos().getX() == village.getX() && first.pos().getZ() == village.getZ(), "found " + first.pos() + ", village at " + village);

                  // Load it onto a locator
                  ItemStack locator = new ItemStack(Items.COMPASS);
                  locator.set(CryoDataComponents.LOCATOR_DATA, LocatorData.EMPTY);
                  terminal.getLocatorSlot().setStack(locator);
                  helper.assertTrue(terminal.loadOntoLocator(first.id()) == RequestResult.LOADED, "load refused");
                  LocatorData data = terminal.getLocatorSlot().getStack().get(CryoDataComponents.LOCATOR_DATA);
                  helper.assertTrue(data != null && data.targets().size() == 1 && data.current().pos().pos().equals(first.pos())
                                    && data.current().target().equals(VILLAGE), "locator data: " + data);

                  // A second request, now with a small constellation, must not report the claimed village again
                  decommission(helper, ultimates);
                  List<OrbitalAsset> basic = launch(helper, owner, RocketTier.BASIC, 1);
                  helper.assertTrue(terminal.request(owner, VILLAGE) == RequestResult.REQUESTED, "second request refused");
                  finishPass(helper, basic.getFirst());
              })
              .thenWaitUntil(() -> {
                  registry.tick(server);
                  helper.assertTrue(terminal.getLog().size() == 2, "second result not in");
              })
              .thenExecute(() -> {
                  SurveyResult second = terminal.getLog().getFirst();
                  boolean same = second.found() && Math.hypot(second.pos().getX() - village.getX(), second.pos().getZ() - village.getZ()) <= second.uncertainty() + 1;
                  helper.assertTrue(!same, "the claimed village was found again: " + second);
                  terminal.dismiss(second.id());
                  helper.assertTrue(terminal.getLog().size() == 1, "dismiss did not remove the entry");
                  OrbitalRegistry.get(server).ownedBy(owner).forEach(registry::decommission);
                  SurveySearch.searchWithoutStructureGeneration = false;
              })
              .thenSucceed();
    }
}
