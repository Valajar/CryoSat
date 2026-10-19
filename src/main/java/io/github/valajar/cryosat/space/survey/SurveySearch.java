package io.github.valajar.cryosat.space.survey;

import com.mojang.datafixers.util.Pair;
import io.github.valajar.cryosat.space.SpaceData;
import io.github.valajar.cryosat.space.SurveyTarget;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacement;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.VisibleForTesting;

/**
 * The structure search behind an imaging pass: vanilla's explorer map lookup with "skip known structures" on, so every
 * structure it finds is marked as claimed world wide and the next search finds the next one. Searches are limited server
 * wide per tick; a pass that does not get a slot waits on its satellite and is offered again.
 */
public final class SurveySearch {
    /** Searches that may run in one server tick, across every terminal. */
    public static final int MAX_SEARCHES_PER_TICK = 2;

    private static int budgetTick = -1;
    private static int searchesThisTick;

    /** Targets added by game tests on top of the datapack ones. */
    private static final Map<ResourceLocation, SurveyTarget> TEST_TARGETS = new HashMap<>();
    /**
     * The game test world is created with structure generation off, which normally means there is nothing to find and the
     * search is skipped. Tests that set structure starts by hand turn this on.
     */
    @VisibleForTesting
    public static boolean searchWithoutStructureGeneration;

    // Targets

    @Nullable
    public static SurveyTarget target(ResourceLocation id) {
        SurveyTarget target = SpaceData.SURVEY_TARGETS.get(id);
        return target != null ? target : TEST_TARGETS.get(id);
    }

    /** Every loaded survey target. */
    public static Map<ResourceLocation, SurveyTarget> targets() {
        if (TEST_TARGETS.isEmpty()) {
            return SpaceData.SURVEY_TARGETS.all();
        }
        Map<ResourceLocation, SurveyTarget> all = new HashMap<>(TEST_TARGETS);
        all.putAll(SpaceData.SURVEY_TARGETS.all());
        return all;
    }

    @VisibleForTesting
    public static void addTestTarget(ResourceLocation id, SurveyTarget target) {
        TEST_TARGETS.put(id, target);
    }

    // Searching

    /**
     * Takes one of this tick's search slots.
     *
     * @return false if they are used up; try again on a later tick
     */
    public static boolean tryStartSearch(MinecraftServer server) {
        int tick = server.getTickCount();
        if (tick != budgetTick) {
            budgetTick = tick;
            searchesThisTick = 0;
        }
        if (searchesThisTick >= MAX_SEARCHES_PER_TICK) {
            return false;
        }
        searchesThisTick++;
        return true;
    }

    /**
     * Finds the nearest unclaimed structure of the set around {@code origin} and claims it, like an explorer map. The search
     * runs vanilla's rings out to {@code range} blocks in each direction (a square); a hit in the last ring beyond the range
     * is claimed but not reported, as is a stronghold beyond it (strongholds sit on fixed rings that are always searched
     * whole).
     *
     * @return the structure's locate position, or {@code null} if there is none within range
     */
    @Nullable
    public static BlockPos find(ServerLevel level, HolderSet<Structure> structures, BlockPos origin, int range) {
        if (range <= 0 || !searchWithoutStructureGeneration && !level.getServer().getWorldData().worldGenOptions().generateStructures()) {
            return null;
        }
        // Vanilla's radius counts rings of the placement grid, not chunks: size it for the densest placement
        ChunkGeneratorStructureState state = level.getChunkSource().getGeneratorState();
        int spacing = Integer.MAX_VALUE;
        for (Holder<Structure> structure : structures) {
            for (StructurePlacement placement : state.getPlacementsForStructure(structure)) {
                if (placement instanceof RandomSpreadStructurePlacement spread) {
                    spacing = Math.min(spacing, spread.spacing());
                }
            }
        }
        int rings = spacing == Integer.MAX_VALUE ? 0 : Math.max(1, Mth.ceil(range / (16.0 * spacing)));
        Pair<BlockPos, Holder<Structure>> found = level.getChunkSource().getGenerator().findNearestMapStructure(level, structures, origin, rings, true);
        if (found == null) {
            return null;
        }
        BlockPos pos = found.getFirst();
        return Math.max(Math.abs(pos.getX() - origin.getX()), Math.abs(pos.getZ() - origin.getZ())) <= range ? pos : null;
    }

    /**
     * Shifts a location to a random point within {@code uncertainty} blocks, the same point every time for the same seed.
     */
    public static BlockPos blur(BlockPos truePos, int uncertainty, UUID seed) {
        if (uncertainty <= 0) {
            return truePos;
        }
        RandomSource random = RandomSource.create(seed.getMostSignificantBits() ^ seed.getLeastSignificantBits());
        double angle = random.nextDouble() * 2 * Math.PI;
        // Square root for an even spread over the disc rather than bunching at the centre
        double distance = uncertainty * Math.sqrt(random.nextDouble());
        return truePos.offset((int) Math.round(Math.cos(angle) * distance), 0, (int) Math.round(Math.sin(angle) * distance));
    }

    private SurveySearch() {
    }
}
