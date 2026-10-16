package io.github.valajar.cryosat.datagen;

import io.github.valajar.cryosat.registries.CryoBlocks;
import java.util.Set;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.registries.DeferredHolder;

public class CryoBlockLoot extends BlockLootSubProvider {
    public CryoBlockLoot(HolderLookup.Provider registries) {
        super(Set.of(), FeatureFlags.REGISTRY.allFlags(), registries);
    }

    @Override
    protected void generate() {
        dropSelf(CryoBlocks.ATMOSPHERIC_COMPRESSOR.get());
    }

    @Override
    protected Iterable<Block> getKnownBlocks() {
        return CryoBlocks.BLOCKS.getEntries().stream().map(DeferredHolder::value).map(Block.class::cast).toList();
    }
}
