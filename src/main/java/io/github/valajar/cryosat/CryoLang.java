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
    ENERGY_PER_MB("gui", "energy_per_mb");

    private final String key;

    CryoLang(String type, String path) {
        this.key = Util.makeDescriptionId(type, CryoSat.rl(path));
    }

    @Override
    public String getTranslationKey() {
        return key;
    }
}
