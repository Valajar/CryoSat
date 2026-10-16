package io.github.valajar.cryosat.multiblock.liquefaction;

import io.github.valajar.cryosat.registries.plant.PlantBlockTypes;
import io.github.valajar.cryosat.tile.liquefaction.TileEntityCryocooler;
import io.github.valajar.cryosat.tile.liquefaction.TileEntityHeatRejectionPort;
import io.github.valajar.cryosat.tile.liquefaction.TileEntityLiquefactionController;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import mekanism.common.content.blocktype.BlockType;
import mekanism.common.lib.math.voxel.VoxelCuboid;
import mekanism.common.lib.multiblock.CuboidStructureValidator;
import mekanism.common.lib.multiblock.FormationProtocol;
import mekanism.common.lib.multiblock.FormationProtocol.CasingType;
import mekanism.common.lib.multiblock.FormationProtocol.FormationResult;
import mekanism.common.MekanismLang;
import mekanism.common.tile.prefab.TileEntityStructuralMultiblock;
import mekanism.common.util.WorldUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;

/**
 * Any hollow box from 3x3x3 to 9x9x9. Edges must be Cryo Casing; walls may also hold Cryo Valves, Cryocoolers, Heat Rejection
 * Ports, exactly one Liquefaction Controller, and Mekanism structural glass.
 */
public class LiquefactionValidator extends CuboidStructureValidator<LiquefactionMultiblockData> {
    private static final VoxelCuboid MIN = new VoxelCuboid(3, 3, 3);
    private static final VoxelCuboid MAX = new VoxelCuboid(9, 9, 9);

    private boolean foundController;

    public LiquefactionValidator() {
        super(MIN, MAX);
    }

    @Override
    protected CasingType getCasingType(BlockState state) {
        Block block = state.getBlock();
        if (BlockType.is(block, PlantBlockTypes.CRYO_CASING)) {
            return CasingType.FRAME;
        } else if (BlockType.is(block, PlantBlockTypes.CRYO_VALVE)) {
            return CasingType.VALVE;
        } else if (BlockType.is(block, PlantBlockTypes.LIQUEFACTION_CONTROLLER, PlantBlockTypes.CRYOCOOLER, PlantBlockTypes.HEAT_REJECTION_PORT)) {
            return CasingType.OTHER;
        }
        return CasingType.INVALID;
    }

    @Override
    protected FormationResult validateFrame(FormationProtocol<LiquefactionMultiblockData> ctx, BlockPos pos, BlockState state, CasingType type, boolean needsFrame) {
        boolean controller = structure.getTile(pos) instanceof TileEntityLiquefactionController;
        if (foundController && controller) {
            return FormationResult.fail(MekanismLang.MULTIBLOCK_INVALID_CONTROLLER_CONFLICT, pos, true);
        }
        foundController |= controller;
        return super.validateFrame(ctx, pos, state, type, needsFrame);
    }

    @Override
    public FormationResult postcheck(LiquefactionMultiblockData structure, Long2ObjectMap<ChunkAccess> chunkMap) {
        if (!foundController) {
            return FormationResult.fail(MekanismLang.MULTIBLOCK_INVALID_NO_CONTROLLER);
        }
        int coolers = 0, ports = 0, glass = 0;
        for (BlockPos pos : structure.locations) {
            BlockEntity tile = WorldUtils.getTileEntity(world, chunkMap, pos);
            if (tile instanceof TileEntityCryocooler) {
                coolers++;
            } else if (tile instanceof TileEntityHeatRejectionPort) {
                ports++;
            } else if (tile instanceof TileEntityStructuralMultiblock) {
                glass++;
            }
        }
        structure.coolers = coolers;
        structure.heatPorts = ports;
        structure.glassBlocks = glass;
        structure.casingBlocks = structure.locations.size() - glass;
        return FormationResult.SUCCESS;
    }
}
