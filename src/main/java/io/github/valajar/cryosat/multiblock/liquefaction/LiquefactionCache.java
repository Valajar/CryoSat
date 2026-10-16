package io.github.valajar.cryosat.multiblock.liquefaction;

import mekanism.common.lib.multiblock.MultiblockCache;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;

/**
 * Keeps the plant's interior temperature across unloads and reforms. Tanks and the hot side are handled by the base cache.
 */
public class LiquefactionCache extends MultiblockCache<LiquefactionMultiblockData> {
    private double coldTemperature = -1;
    private double[] remainders = new double[LiquefactionMultiblockData.OUTPUT_TANKS];

    @Override
    public void merge(MultiblockCache<LiquefactionMultiblockData> mergeCache, RejectContents rejectContents) {
        super.merge(mergeCache, rejectContents);
        double other = ((LiquefactionCache) mergeCache).coldTemperature;
        if (other >= 0 && (coldTemperature < 0 || other > coldTemperature)) {
            // Merging two plants: the warmer one wins, so merging never creates cold out of nothing
            coldTemperature = other;
        }
    }

    @Override
    public void apply(HolderLookup.Provider provider, LiquefactionMultiblockData data) {
        super.apply(provider, data);
        data.loadThermalState(coldTemperature, remainders);
    }

    @Override
    public void sync(LiquefactionMultiblockData data) {
        super.sync(data);
        coldTemperature = data.coldTemperature;
        remainders = data.getOutputRemainders();
    }

    @Override
    public void load(HolderLookup.Provider provider, CompoundTag nbt) {
        super.load(provider, nbt);
        coldTemperature = nbt.contains("cold_temperature") ? nbt.getDouble("cold_temperature") : -1;
        for (int i = 0; i < remainders.length; i++) {
            remainders[i] = nbt.getDouble("remainder_" + i);
        }
    }

    @Override
    public void save(HolderLookup.Provider provider, CompoundTag nbt) {
        super.save(provider, nbt);
        nbt.putDouble("cold_temperature", coldTemperature);
        for (int i = 0; i < remainders.length; i++) {
            nbt.putDouble("remainder_" + i, remainders[i]);
        }
    }
}
