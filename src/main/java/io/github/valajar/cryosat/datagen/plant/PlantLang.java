package io.github.valajar.cryosat.datagen.plant;

import io.github.valajar.cryosat.registries.plant.CryoLang;
import io.github.valajar.cryosat.registries.plant.PlantBlocks;
import java.util.function.BiConsumer;
import net.neoforged.neoforge.common.data.LanguageProvider;

/**
 * English text for the Liquefaction Plant, added by the main language provider.
 */
public final class PlantLang {
    public static void addTo(LanguageProvider provider) {
        provider.addBlock(PlantBlocks.CRYO_CASING, "Cryo Casing");
        provider.addBlock(PlantBlocks.CRYO_VALVE, "Cryo Valve");
        provider.addBlock(PlantBlocks.CRYOCOOLER, "Cryocooler");
        provider.addBlock(PlantBlocks.HEAT_REJECTION_PORT, "Heat Rejection Port");
        provider.addBlock(PlantBlocks.LIQUEFACTION_CONTROLLER, "Liquefaction Controller");

        BiConsumer<CryoLang, String> add = (lang, text) -> provider.add(lang.getTranslationKey(), text);
        add.accept(CryoLang.LIQUEFACTION_PLANT, "Liquefaction Plant");
        add.accept(CryoLang.DESCRIPTION_CRYO_CASING, "Insulated casing for the Cryogenic Liquefaction Plant, a hollow box from 3x3x3 up to 9x9x9.");
        add.accept(CryoLang.DESCRIPTION_CRYO_VALVE, "Chemical port of the Liquefaction Plant. Sneak right click to switch between input and the three outputs; output modes push into anything attached.");
        add.accept(CryoLang.DESCRIPTION_CRYOCOOLER, "Pumps heat out of the Liquefaction Plant using power. The colder the plant, the more energy each unit of cooling costs.");
        add.accept(CryoLang.DESCRIPTION_HEAT_REJECTION_PORT, "Dumps the heat removed by Cryocoolers, plus the energy they spent, into adjacent heat handlers such as Thermodynamic Conductors or a Thermoelectric Boiler.");
        add.accept(CryoLang.DESCRIPTION_LIQUEFACTION_CONTROLLER, "Brain of the Cryogenic Liquefaction Plant. Exactly one per plant.");

        add.accept(CryoLang.VALVE_MODE_CHANGE, "Valve mode: %1$s");
        add.accept(CryoLang.VALVE_MODE_INPUT, "Input");
        add.accept(CryoLang.VALVE_MODE_OUTPUT, "Output %1$s");

        add.accept(CryoLang.PLANT_TEMPERATURE, "Temp: %1$s");
        add.accept(CryoLang.PLANT_HOT_SIDE, "Hot side: %1$s");
        add.accept(CryoLang.PLANT_COOLERS, "Coolers: %1$s, %2$s/t");
        add.accept(CryoLang.PLANT_ENERGY, "Using: %1$s/t");
        add.accept(CryoLang.PLANT_RATE, "Liquefying: %1$s mB/t");
        add.accept(CryoLang.PLANT_LEAK, "Heat leaking in: %1$s/t");
        add.accept(CryoLang.PLANT_SIZE, "Size: %1$sx%2$sx%3$s");

        add.accept(CryoLang.PLANT_STATUS_IDLE, "Idle");
        add.accept(CryoLang.PLANT_STATUS_RUNNING, "Liquefying");
        add.accept(CryoLang.PLANT_STATUS_COOLING_DOWN, "Cooling down");
        add.accept(CryoLang.PLANT_STATUS_NO_COOLERS, "No Cryocoolers");
        add.accept(CryoLang.PLANT_STATUS_OUTPUT_FULL, "Output full");
        add.accept(CryoLang.PLANT_STATUS_OUTPUT_MISMATCH, "Output holds other chemical");
        add.accept(CryoLang.PLANT_STATUS_OVERHEATED, "Hot side overheated!");
    }

    private PlantLang() {
    }
}
