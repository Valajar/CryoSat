package io.github.valajar.cryosat.datagen;

import io.github.valajar.cryosat.CryoLang;
import io.github.valajar.cryosat.CryoSat;
import io.github.valajar.cryosat.datagen.plant.PlantLang;
import io.github.valajar.cryosat.datagen.space.SpaceLangData;
import io.github.valajar.cryosat.machine.MachineStatus;
import io.github.valajar.cryosat.registries.CryoBlocks;
import io.github.valajar.cryosat.registries.CryoChemicals;
import mekanism.api.chemical.Chemical;
import mekanism.api.text.IHasTranslationKey;
import net.minecraft.Util;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.PackOutput;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.common.data.LanguageProvider;
import net.neoforged.neoforge.registries.DeferredHolder;
import io.github.valajar.cryosat.datagen.mission.MissionLangData;

public class CryoLanguageProvider extends LanguageProvider {
    public CryoLanguageProvider(PackOutput output) {
        super(output, CryoSat.MODID, "en_us");
    }

    @Override
    protected void addTranslations() {
        add("itemGroup." + CryoSat.MODID, "CryoSat");

        machine(CryoBlocks.ATMOSPHERIC_COMPRESSOR.get(), "Atmospheric Compressor");
        add(CryoLang.DESCRIPTION_ATMOSPHERIC_COMPRESSOR, "Compresses the surrounding atmosphere into a gas. Needs open air around it, and output thins out at "
              + "high altitude.");

        chemical(CryoChemicals.COMPRESSED_AIR, "Compressed Air");
        chemical(CryoChemicals.LIQUID_NITROGEN, "Liquid Nitrogen");
        chemical(CryoChemicals.LIQUID_OXYGEN, "Liquid Oxygen");
        chemical(CryoChemicals.LIQUID_ARGON, "Liquid Argon");
        chemical(CryoChemicals.LIQUID_HYDROGEN, "Liquid Hydrogen");

        add(MachineStatus.IDLE, "Idle");
        add(MachineStatus.RUNNING, "Running");
        add(MachineStatus.NO_POWER, "Not enough power");
        add(MachineStatus.OUTPUT_FULL, "Output full");
        add(MachineStatus.OUTPUT_MISMATCH, "Output holds a different chemical");
        add(MachineStatus.NO_ATMOSPHERE, "No atmosphere here");
        add(MachineStatus.NO_AIR, "Needs open air around it");

        add(CryoLang.OUTPUT_RATE, "Output: %1$s mB/t");
        add(CryoLang.ENERGY_PER_MB, "Energy: %1$s/mB");

        PlantLang.addTo(this);
        SpaceLangData.addTo(this);
        // Cargo Receiver and Uplink Card (space workstream A)
        io.github.valajar.cryosat.datagen.receiver.ReceiverLangData.addTo(this);
        // Mission Control (workstream D)
        MissionLangData.addTo(this);
    }

    /** Block name, plus the same name as the GUI title Mekanism looks up under {@code container.<ns>.<name>}. */
    private void machine(Block block, String name) {
        add(block, name);
        add(Util.makeDescriptionId("container", BuiltInRegistries.BLOCK.getKey(block)), name);
    }

    private void add(IHasTranslationKey key, String text) {
        add(key.getTranslationKey(), text);
    }

    private void chemical(DeferredHolder<Chemical, Chemical> chemical, String name) {
        add("chemical." + CryoSat.MODID + "." + chemical.getId().getPath(), name);
    }
}
