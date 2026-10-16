package io.github.valajar.cryosat.tile.liquefaction;

import io.github.valajar.cryosat.multiblock.liquefaction.LiquefactionMultiblockData;
import io.github.valajar.cryosat.multiblock.liquefaction.PlantStatus;
import io.github.valajar.cryosat.registries.CryoBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

public class TileEntityLiquefactionController extends TileEntityCryoCasing {

    public TileEntityLiquefactionController(BlockPos pos, BlockState state) {
        super(CryoBlocks.LIQUEFACTION_CONTROLLER, pos, state);
        delaySupplier = NO_DELAY;
    }

    @Override
    protected boolean onUpdateServer(LiquefactionMultiblockData multiblock) {
        boolean needsPacket = super.onUpdateServer(multiblock);
        setActive(multiblock.isFormed() && multiblock.status == PlantStatus.RUNNING);
        return needsPacket;
    }

    @Override
    public boolean canBeMaster() {
        return true;
    }
}
