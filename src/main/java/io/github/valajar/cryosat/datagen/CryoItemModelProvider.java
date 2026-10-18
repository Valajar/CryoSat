package io.github.valajar.cryosat.datagen;

import io.github.valajar.cryosat.CryoSat;
import io.github.valajar.cryosat.registries.CryoItems;
import java.util.Map;
import mekanism.common.registration.impl.ItemRegistryObject;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.client.model.generators.ItemModelProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

/**
 * Models for standalone items (block items get theirs from the block state providers). Each item is a flat
 * {@code item/generated} model of {@code textures/item/<name>.png}.
 */
public class CryoItemModelProvider extends ItemModelProvider {
    public CryoItemModelProvider(PackOutput output, ExistingFileHelper files) {
        super(output, CryoSat.MODID, files);
    }

    @Override
    protected void registerModels() {
        // Space items: rocket parts and the solid propellant chain
        tiered(CryoItems.ROCKET_ENGINES);
        tiered(CryoItems.FUEL_TANKS);
        tiered(CryoItems.ASTEROID_MINERS);
        basicItem(CryoItems.GUIDANCE_COMPUTER.get());
        basicItem(CryoItems.PROPELLANT_MIX.get());
        basicItem(CryoItems.SOLID_PROPELLANT_GRAIN.get());

        // Superconductors (superconductor workstream A): the coil
        basicItem(CryoItems.SUPERCONDUCTING_COIL.get());
    }

    private void tiered(Map<?, ? extends ItemRegistryObject<?>> items) {
        items.values().forEach(item -> basicItem(item.asItem()));
    }
}
