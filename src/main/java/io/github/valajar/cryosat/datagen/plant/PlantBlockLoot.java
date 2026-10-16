package io.github.valajar.cryosat.datagen.plant;

import io.github.valajar.cryosat.registries.plant.PlantBlocks;
import java.util.Set;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.registries.DeferredHolder;

public class PlantBlockLoot extends BlockLootSubProvider {
    public PlantBlockLoot(HolderLookup.Provider registries) {
        super(Set.of(), FeatureFlags.REGISTRY.allFlags(), registries);
    }

    @Override
    protected void generate() {
        getKnownBlocks().forEach(this::dropSelf);
    }

    @Override
    protected Iterable<Block> getKnownBlocks() {
        return PlantBlocks.BLOCKS.getPrimaryEntries().stream().map(DeferredHolder::value).map(Block.class::cast).toList();
    }
}
