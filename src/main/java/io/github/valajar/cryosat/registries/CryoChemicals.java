package io.github.valajar.cryosat.registries;

import io.github.valajar.cryosat.CryoSat;
import mekanism.api.MekanismAPI;
import mekanism.api.chemical.Chemical;
import mekanism.api.chemical.ChemicalBuilder;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Chemicals are registered straight into Mekanism's chemical registry, so they flow through
 * pressurized tubes, chemical tanks and every other Mekanism chemical handler for free. Gases are marked by the
 * {@code mekanism:gaseous} chemical tag in data generation.
 */
public final class CryoChemicals {
    public static final DeferredRegister<Chemical> CHEMICALS = DeferredRegister.create(MekanismAPI.CHEMICAL_REGISTRY_NAME, CryoSat.MODID);

    public static final DeferredHolder<Chemical, Chemical> COMPRESSED_AIR = CHEMICALS.register("compressed_air",
          () -> new Chemical(ChemicalBuilder.builder().tint(0xC9D3DC)));
    /** Boil off of liquid nitrogen coolant; liquefies back into liquid nitrogen in the plant. */
    public static final DeferredHolder<Chemical, Chemical> NITROGEN = CHEMICALS.register("nitrogen",
          () -> new Chemical(ChemicalBuilder.builder().tint(0xB9C6DA)));
    public static final DeferredHolder<Chemical, Chemical> LIQUID_NITROGEN = CHEMICALS.register("liquid_nitrogen",
          () -> new Chemical(ChemicalBuilder.builder().tint(0xD8E4F5)));
    public static final DeferredHolder<Chemical, Chemical> LIQUID_OXYGEN = CHEMICALS.register("liquid_oxygen",
          () -> new Chemical(ChemicalBuilder.builder().tint(0x8FB4FF)));
    public static final DeferredHolder<Chemical, Chemical> LIQUID_ARGON = CHEMICALS.register("liquid_argon",
          () -> new Chemical(ChemicalBuilder.builder().tint(0xC9B8FF)));
    public static final DeferredHolder<Chemical, Chemical> LIQUID_HYDROGEN = CHEMICALS.register("liquid_hydrogen",
          () -> new Chemical(ChemicalBuilder.builder().tint(0xE8F1FF)));

    private CryoChemicals() {
    }
}
