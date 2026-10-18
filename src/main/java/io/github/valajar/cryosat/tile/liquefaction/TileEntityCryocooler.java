package io.github.valajar.cryosat.tile.liquefaction;

import io.github.valajar.cryosat.multiblock.liquefaction.LiquefactionMultiblockData;
import io.github.valajar.cryosat.registries.CryoBlocks;
import mekanism.api.IContentsListener;
import mekanism.common.attachments.containers.ContainerType;
import mekanism.common.capabilities.holder.energy.IEnergyContainerHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

/**
 * A cooler in the plant wall. Each one adds cooling power and buffer size; all of them draw on the plant's shared energy
 * buffer, which any cooler (or valve) accepts power into from any side.
 */
public class TileEntityCryocooler extends TileEntityCryoCasing {

    public TileEntityCryocooler(BlockPos pos, BlockState state) {
        super(CryoBlocks.CRYOCOOLER, pos, state);
    }

    @NotNull
    @Override
    protected IEnergyContainerHolder getInitialEnergyContainers(IContentsListener listener) {
        return side -> getMultiblock().getEnergyContainers(side);
    }

    @Override
    public boolean persists(ContainerType<?, ?, ?> type) {
        // The energy belongs to the plant and is saved with its cache
        return type != ContainerType.ENERGY && super.persists(type);
    }

    @Override
    protected boolean onUpdateServer(LiquefactionMultiblockData multiblock) {
        boolean needsPacket = super.onUpdateServer(multiblock);
        setActive(multiblock.isFormed() && multiblock.lastCooling > 0);
        return needsPacket;
    }
}
