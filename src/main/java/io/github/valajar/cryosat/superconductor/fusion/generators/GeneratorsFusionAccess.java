package io.github.valajar.cryosat.superconductor.fusion.generators;

import io.github.valajar.cryosat.superconductor.fusion.FusionReactorAccess;
import mekanism.api.Action;
import mekanism.api.AutomationType;
import mekanism.common.util.WorldUtils;
import mekanism.generators.common.content.fusion.FusionReactorMultiblockData;
import mekanism.generators.common.tile.fusion.TileEntityFusionReactorBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import org.jetbrains.annotations.Nullable;

/**
 * {@link FusionReactorAccess} on top of Generators' public {@link FusionReactorMultiblockData}. The only class in CryoSat
 * that names Generators types; it is loaded only when Generators is present (see {@link FusionReactorAccess#get()}).
 */
public final class GeneratorsFusionAccess implements FusionReactorAccess {

    public static FusionReactorAccess create() {
        return new GeneratorsFusionAccess();
    }

    private GeneratorsFusionAccess() {
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Nullable
    @Override
    public FoundReactor find(Level level, BlockPos origin, int horizontalRadius, int verticalRadius) {
        int minX = origin.getX() - horizontalRadius, maxX = origin.getX() + horizontalRadius;
        int minY = origin.getY() - verticalRadius, maxY = origin.getY() + verticalRadius;
        int minZ = origin.getZ() - horizontalRadius, maxZ = origin.getZ() + horizontalRadius;
        TileEntityFusionReactorBlock nearestFormed = null, nearestAny = null;
        double formedDistance = Double.MAX_VALUE, anyDistance = Double.MAX_VALUE;
        // Walk the block entities of the few chunks the box touches rather than every position in it
        for (int cx = SectionPos.blockToSectionCoord(minX); cx <= SectionPos.blockToSectionCoord(maxX); cx++) {
            for (int cz = SectionPos.blockToSectionCoord(minZ); cz <= SectionPos.blockToSectionCoord(maxZ); cz++) {
                LevelChunk chunk = level.getChunkSource().getChunkNow(cx, cz);
                if (chunk == null) {
                    continue;
                }
                for (BlockEntity blockEntity : chunk.getBlockEntities().values()) {
                    if (!(blockEntity instanceof TileEntityFusionReactorBlock reactorBlock) || blockEntity.isRemoved()) {
                        continue;
                    }
                    BlockPos pos = blockEntity.getBlockPos();
                    if (pos.getX() < minX || pos.getX() > maxX || pos.getY() < minY || pos.getY() > maxY || pos.getZ() < minZ || pos.getZ() > maxZ) {
                        continue;
                    }
                    double distance = pos.distSqr(origin);
                    if (distance < anyDistance) {
                        anyDistance = distance;
                        nearestAny = reactorBlock;
                    }
                    if (reactorBlock.getMultiblock().isFormed() && distance < formedDistance) {
                        formedDistance = distance;
                        nearestFormed = reactorBlock;
                    }
                }
            }
        }
        if (nearestFormed != null) {
            FusionReactorMultiblockData reactor = nearestFormed.getMultiblock();
            return new FoundReactor(true, reactor.getMinPos(), reactor.getMaxPos());
        }
        return nearestAny == null ? null : new FoundReactor(false, null, null);
    }

    @Nullable
    @Override
    public Reactor get(Level level, BlockPos min, BlockPos max) {
        // The reactor controller always sits in the middle of the top face
        BlockPos controller = new BlockPos((min.getX() + max.getX()) / 2, max.getY(), (min.getZ() + max.getZ()) / 2);
        TileEntityFusionReactorBlock tile = WorldUtils.getTileEntity(TileEntityFusionReactorBlock.class, level, controller);
        if (tile == null) {
            return null;
        }
        FusionReactorMultiblockData reactor = tile.getMultiblock();
        if (!reactor.isFormed() || !min.equals(reactor.getMinPos()) || !max.equals(reactor.getMaxPos())) {
            return null;
        }
        return new GeneratorsReactor(reactor);
    }

    private record GeneratorsReactor(FusionReactorMultiblockData data) implements Reactor {
        @Override
        public boolean isBurning() {
            return data.isBurning();
        }

        @Override
        public long passiveGeneration() {
            // Air cooled formula at the current case temperature; with current = true the "active" flag is not used
            return data.getPassiveGeneration(false, true);
        }

        @Override
        public boolean isWaterCooled() {
            return !data.waterTank.isEmpty() || !data.steamTank.isEmpty();
        }

        @Override
        public double caseTemperature() {
            return data.getLastCaseTemp();
        }

        @Override
        public long insertEnergy(long amount, Action action) {
            if (amount <= 0) {
                return 0;
            }
            return amount - data.energyContainer.insert(amount, action, AutomationType.INTERNAL);
        }
    }
}
