package io.github.valajar.cryosat.datagen;

import io.github.valajar.cryosat.CryoSat;
import io.github.valajar.cryosat.datagen.plant.PlantLang;
import io.github.valajar.cryosat.machine.MachineStatus;
import io.github.valajar.cryosat.registries.CryoBlocks;
import io.github.valajar.cryosat.registries.CryoChemicals;
import mekanism.api.chemical.Chemical;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.LanguageProvider;
import net.neoforged.neoforge.registries.DeferredHolder;

public class CryoLanguageProvider extends LanguageProvider {
    public CryoLanguageProvider(PackOutput output) {
        super(output, CryoSat.MODID, "en_us");
    }

    @Override
    protected void addTranslations() {
        add("itemGroup." + CryoSat.MODID, "CryoSat");

        addBlock(CryoBlocks.ATMOSPHERIC_COMPRESSOR, "Atmospheric Compressor");

        chemical(CryoChemicals.COMPRESSED_AIR, "Compressed Air");
        chemical(CryoChemicals.LIQUID_NITROGEN, "Liquid Nitrogen");
        chemical(CryoChemicals.LIQUID_OXYGEN, "Liquid Oxygen");
        chemical(CryoChemicals.LIQUID_ARGON, "Liquid Argon");
        chemical(CryoChemicals.LIQUID_HYDROGEN, "Liquid Hydrogen");

        status(MachineStatus.IDLE, "Idle");
        status(MachineStatus.RUNNING, "Running");
        status(MachineStatus.NO_POWER, "Not enough power");
        status(MachineStatus.OUTPUT_FULL, "Output full");
        status(MachineStatus.OUTPUT_MISMATCH, "Output holds a different chemical");
        status(MachineStatus.NO_ATMOSPHERE, "No atmosphere here");
        status(MachineStatus.NO_AIR, "Needs open air around it");

        gui("rate", "%s mB/t");
        gui("energy", "%s / %s J");
        gui("amount", "%s / %s mB");
        gui("empty", "Empty");

        PlantLang.addTo(this);
    }

    private void chemical(DeferredHolder<Chemical, Chemical> chemical, String name) {
        add("chemical." + CryoSat.MODID + "." + chemical.getId().getPath(), name);
    }

    private void status(MachineStatus status, String text) {
        add(status.translationKey(), text);
    }

    private void gui(String key, String text) {
        add("gui." + CryoSat.MODID + "." + key, text);
    }
}
