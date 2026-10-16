package io.github.valajar.cryosat.datagen;

import io.github.valajar.cryosat.CryoSat;
import io.github.valajar.cryosat.registries.CryoBlocks;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.tags.BlockTags;
import net.neoforged.neoforge.common.data.BlockTagsProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

public class CryoBlockTagsProvider extends BlockTagsProvider {
    public CryoBlockTagsProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookup, ExistingFileHelper files) {
        super(output, lookup, CryoSat.MODID, files);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        CryoBlocks.BLOCKS.getPrimaryEntries().forEach(block -> tag(BlockTags.MINEABLE_WITH_PICKAXE).add(block.get()));
    }
}
