package io.github.valajar.cryosat.capability;

import mekanism.api.Action;
import mekanism.api.AutomationType;
import mekanism.api.energy.IEnergyContainer;
import mekanism.api.energy.IEnergyConversion;
import mekanism.api.energy.IEnergyConversionHelper;
import mekanism.api.energy.IStrictEnergyHandler;
import net.neoforged.neoforge.energy.IEnergyStorage;

/**
 * Input only view of a machine's energy buffer, both as a Mekanism strict energy handler (Joules) and as a NeoForge FE
 * storage for other mods' cables. FE is converted with Mekanism's configured rate.
 */
public class EnergyInputHandler implements IStrictEnergyHandler, IEnergyStorage {
    private final IEnergyContainer container;

    public EnergyInputHandler(IEnergyContainer container) {
        this.container = container;
    }

    // --- Mekanism -------------------------------------------------------------------------------

    @Override
    public int getEnergyContainerCount() {
        return 1;
    }

    @Override
    public long getEnergy(int index) {
        return index == 0 ? container.getEnergy() : 0;
    }

    @Override
    public void setEnergy(int index, long energy) {
        if (index == 0) {
            container.setEnergy(energy);
        }
    }

    @Override
    public long getMaxEnergy(int index) {
        return index == 0 ? container.getMaxEnergy() : 0;
    }

    @Override
    public long getNeededEnergy(int index) {
        return index == 0 ? container.getNeeded() : 0;
    }

    @Override
    public long insertEnergy(int index, long amount, Action action) {
        return index == 0 ? container.insert(amount, action, AutomationType.EXTERNAL) : amount;
    }

    @Override
    public long extractEnergy(int index, long amount, Action action) {
        return 0;
    }

    // --- NeoForge FE ----------------------------------------------------------------------------

    private static IEnergyConversion fe() {
        return IEnergyConversionHelper.INSTANCE.feConversion();
    }

    @Override
    public int receiveEnergy(int maxReceive, boolean simulate) {
        IEnergyConversion fe = fe();
        if (!fe.isEnabled() || maxReceive <= 0) {
            return 0;
        }
        long joules = fe.convertFrom(maxReceive);
        long accepted = joules - container.insert(joules, Action.get(!simulate), AutomationType.EXTERNAL);
        return Math.min(maxReceive, fe.convertToAsInt(accepted));
    }

    @Override
    public int extractEnergy(int maxExtract, boolean simulate) {
        return 0;
    }

    @Override
    public int getEnergyStored() {
        return fe().convertToAsInt(container.getEnergy());
    }

    @Override
    public int getMaxEnergyStored() {
        return fe().convertToAsInt(container.getMaxEnergy());
    }

    @Override
    public boolean canExtract() {
        return false;
    }

    @Override
    public boolean canReceive() {
        return fe().isEnabled();
    }
}
