package io.github.valajar.cryosat.capability;

import mekanism.api.IContentsListener;
import mekanism.api.SerializationConstants;
import mekanism.api.energy.IEnergyContainer;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;

/**
 * Energy buffer of a machine, in Joules like everything in Mekanism.
 */
public class MachineEnergyContainer implements IEnergyContainer {
    private final long capacity;
    private final IContentsListener listener;
    private long energy;

    public MachineEnergyContainer(long capacity, IContentsListener listener) {
        this.capacity = capacity;
        this.listener = listener;
    }

    @Override
    public long getEnergy() {
        return energy;
    }

    @Override
    public void setEnergy(long energy) {
        long clamped = Math.max(0, Math.min(capacity, energy));
        if (clamped != this.energy) {
            this.energy = clamped;
            onContentsChanged();
        }
    }

    @Override
    public long getMaxEnergy() {
        return capacity;
    }

    /**
     * Consumes {@code amount} Joules if all of it is available.
     */
    public boolean tryUse(long amount) {
        if (energy < amount) {
            return false;
        }
        setEnergy(energy - amount);
        return true;
    }

    @Override
    public void onContentsChanged() {
        listener.onContentsChanged();
    }

    @Override
    public void deserializeNBT(HolderLookup.Provider provider, CompoundTag nbt) {
        energy = Math.min(capacity, nbt.getLong(SerializationConstants.STORED));
    }
}
