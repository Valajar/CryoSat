package io.github.valajar.cryosat.registries;

import io.github.valajar.cryosat.CryoSat;
import mekanism.api.MekanismAPI;
import mekanism.api.chemical.Chemical;
import mekanism.api.chemical.ChemicalBuilder;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Chemicals are registered straight into Mekanism's chemical registry, so they flow through
 * pressurized tubes, chemical tanks and every other Mekanism chemical handler for free.
 */
public final class CryoChemicals {
    public static final DeferredRegister<Chemical> CHEMICALS = DeferredRegister.create(MekanismAPI.CHEMICAL_REGISTRY_NAME, CryoSat.MODID);

    public static final DeferredHolder<Chemical, Chemical> LIQUID_NITROGEN = CHEMICALS.register("liquid_nitrogen",
          () -> new Chemical(ChemicalBuilder.builder().tint(0xD8E4F5)));

    private CryoChemicals() {
    }
}
