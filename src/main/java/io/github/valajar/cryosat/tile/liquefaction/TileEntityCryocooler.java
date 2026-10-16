package io.github.valajar.cryosat.tile.liquefaction;

import io.github.valajar.cryosat.multiblock.liquefaction.LiquefactionMultiblockData;
import io.github.valajar.cryosat.registries.plant.PlantBlocks;
import mekanism.api.IContentsListener;
import mekanism.common.capabilities.energy.MachineEnergyContainer;
import mekanism.common.capabilities.holder.energy.EnergyContainerHelper;
import mekanism.common.capabilities.holder.energy.IEnergyContainerHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

/**
 * A powered cooler in the plant wall. Each one has its own energy buffer (fed from any side, like an SPS port) and pumps heat
 * out of the plant every tick it has the energy to.
 */
public class TileEntityCryocooler extends TileEntityCryoCasing {
    private MachineEnergyContainer<TileEntityCryocooler> energyContainer;

    public TileEntityCryocooler(BlockPos pos, BlockState state) {
        super(PlantBlocks.CRYOCOOLER, pos, state);
    }

    @NotNull
    @Override
    protected IEnergyContainerHolder getInitialEnergyContainers(IContentsListener listener) {
        EnergyContainerHelper builder = EnergyContainerHelper.forSide(facingSupplier);
        builder.addContainer(energyContainer = MachineEnergyContainer.input(this, listener));
        return builder.build();
    }

    @Override
    protected boolean onUpdateServer(LiquefactionMultiblockData multiblock) {
        boolean needsPacket = super.onUpdateServer(multiblock);
        setActive(multiblock.isFormed() && multiblock.runCooler(energyContainer));
        return needsPacket;
    }

    public MachineEnergyContainer<TileEntityCryocooler> getEnergyContainer() {
        return energyContainer;
    }
}
