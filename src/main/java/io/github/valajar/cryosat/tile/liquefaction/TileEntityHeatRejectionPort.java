package io.github.valajar.cryosat.tile.liquefaction;

import io.github.valajar.cryosat.multiblock.liquefaction.LiquefactionMultiblockData;
import io.github.valajar.cryosat.registries.CryoBlocks;
import mekanism.api.IContentsListener;
import mekanism.api.heat.IHeatHandler;
import mekanism.common.attachments.containers.ContainerType;
import mekanism.common.capabilities.heat.CachedAmbientTemperature;
import mekanism.common.capabilities.holder.heat.IHeatCapacitorHolder;
import mekanism.common.tile.prefab.TileEntityMultiblock;
import mekanism.common.util.WorldUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Exposes the plant's hot side as a Mekanism heat handler and pushes heat into adjacent heat handlers (Thermodynamic
 * Conductors, boilers, anything that accepts heat) every tick.
 */
public class TileEntityHeatRejectionPort extends TileEntityCryoCasing {

    public TileEntityHeatRejectionPort(BlockPos pos, BlockState state) {
        super(CryoBlocks.HEAT_REJECTION_PORT, pos, state);
        delaySupplier = NO_DELAY;
    }

    @NotNull
    @Override
    protected IHeatCapacitorHolder getInitialHeatCapacitors(IContentsListener listener, CachedAmbientTemperature ambientTemperature) {
        return side -> getMultiblock().getHeatCapacitors(side);
    }

    @Override
    public boolean persists(ContainerType<?, ?, ?> type) {
        return type != ContainerType.HEAT && super.persists(type);
    }

    @Nullable
    @Override
    public IHeatHandler getAdjacent(@NotNull Direction side) {
        if (canHandleHeat() && getHeatCapacitorCount(side) > 0) {
            // Never push heat back into the plant itself
            if (!(WorldUtils.getTileEntity(level, worldPosition.relative(side)) instanceof TileEntityMultiblock<?> neighbour
                  && neighbour.getMultiblock() == getMultiblock())) {
                return getAdjacentUnchecked(side);
            }
        }
        return null;
    }

    @Override
    protected boolean onUpdateServer(LiquefactionMultiblockData multiblock) {
        boolean needsPacket = super.onUpdateServer(multiblock);
        boolean rejecting = false;
        if (multiblock.isFormed() && multiblock.getHotTemperature() > multiblock.getAmbientTemperature()) {
            rejecting = simulateAdjacent() > 0;
        }
        setActive(rejecting);
        return needsPacket;
    }
}
