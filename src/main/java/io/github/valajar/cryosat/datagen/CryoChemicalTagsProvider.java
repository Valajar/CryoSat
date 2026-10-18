package io.github.valajar.cryosat.datagen;

import io.github.valajar.cryosat.CryoSat;
import io.github.valajar.cryosat.registries.CryoChemicals;
import java.util.concurrent.CompletableFuture;
import mekanism.api.MekanismAPI;
import mekanism.api.MekanismAPITags;
import mekanism.api.chemical.Chemical;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.TagsProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

public class CryoChemicalTagsProvider extends TagsProvider<Chemical> {
    public CryoChemicalTagsProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookup, ExistingFileHelper files) {
        super(output, MekanismAPI.CHEMICAL_REGISTRY_NAME, lookup, CryoSat.MODID, files);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        // Marks chemicals that render as a gas rather than a liquid
        tag(MekanismAPITags.Chemicals.GASEOUS).add(CryoChemicals.COMPRESSED_AIR.getKey(), CryoChemicals.NITROGEN.getKey());
    }
}
