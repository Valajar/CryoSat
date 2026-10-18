package io.github.valajar.cryosat;

import mekanism.api.text.ILangEntry;
import net.minecraft.Util;

/**
 * Translation keys used from code, in the style of Mekanism's {@code MekanismLang}. Block descriptions are shown by
 * Mekanism's block item tooltip (hold the description key), GUI entries in machine screens.
 */
public enum CryoLang implements ILangEntry {
    DESCRIPTION_ATMOSPHERIC_COMPRESSOR("description", "atmospheric_compressor"),
    OUTPUT_RATE("gui", "output_rate"),
    ENERGY_PER_MB("gui", "energy_per_mb"),

    // Liquefaction Plant
    LIQUEFACTION_PLANT("gui", "liquefaction_plant"),
    DESCRIPTION_CRYO_CASING("description", "cryo_casing"),
    DESCRIPTION_CRYO_VALVE("description", "cryo_valve"),
    DESCRIPTION_CRYOCOOLER("description", "cryocooler"),
    DESCRIPTION_HEAT_REJECTION_PORT("description", "heat_rejection_port"),
    DESCRIPTION_LIQUEFACTION_CONTROLLER("description", "liquefaction_controller"),

    VALVE_MODE_CHANGE("valve", "mode_change"),
    VALVE_MODE_INPUT("valve", "mode.input"),
    VALVE_MODE_OUTPUT("valve", "mode.output"),

    PLANT_TEMPERATURE("gui", "plant.temperature"),
    PLANT_HOT_SIDE("gui", "plant.hot_side"),
    PLANT_COOLERS("gui", "plant.coolers"),
    PLANT_COOLING("gui", "plant.cooling"),
    PLANT_TARGET("gui", "plant.target"),
    PLANT_TARGET_NONE("gui", "plant.target_none"),
    PLANT_ENERGY("gui", "plant.energy"),
    PLANT_LEAK("gui", "plant.leak"),
    PLANT_RATE("gui", "plant.rate"),
    PLANT_SIZE("gui", "plant.size"),

    PLANT_STATUS_IDLE("status", "plant.idle"),
    PLANT_STATUS_RUNNING("status", "plant.running"),
    PLANT_STATUS_COOLING_DOWN("status", "plant.cooling_down"),
    PLANT_STATUS_NO_COOLERS("status", "plant.no_coolers"),
    PLANT_STATUS_OUTPUT_FULL("status", "plant.output_full"),
    PLANT_STATUS_OUTPUT_MISMATCH("status", "plant.output_mismatch"),
    PLANT_STATUS_OVERHEATED("status", "plant.overheated"),

    // Recipe viewers
    RECIPE_MAX_TEMPERATURE("recipe", "liquefaction.max_temperature"),
    RECIPE_LATENT_HEAT("recipe", "liquefaction.latent_heat"),
    RECIPE_INPUT("recipe", "liquefaction.input"),
    RECIPE_OUTPUT("recipe", "liquefaction.output");

    private final String key;

    CryoLang(String type, String path) {
        this.key = Util.makeDescriptionId(type, CryoSat.rl(path));
    }

    @Override
    public String getTranslationKey() {
        return key;
    }
}
