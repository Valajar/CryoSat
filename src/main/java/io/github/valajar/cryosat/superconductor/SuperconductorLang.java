package io.github.valajar.cryosat.superconductor;

import io.github.valajar.cryosat.CryoSat;
import mekanism.api.text.ILangEntry;
import net.minecraft.Util;
import org.jetbrains.annotations.NotNull;

/**
 * Translation keys shared by superconducting blocks. English text in {@code datagen/superconductor/SuperconductorLangData}.
 */
public enum SuperconductorLang implements ILangEntry {
    STATE_NO_COOLANT("superconductor", "state.no_coolant"),
    STATE_COOLING_DOWN("superconductor", "state.cooling_down"),
    STATE_SUPERCONDUCTING("superconductor", "state.superconducting"),
    COOLDOWN_LEFT("superconductor", "cooldown_left"),
    COOLANT_USE("superconductor", "coolant_use");

    private final String key;

    SuperconductorLang(String type, String path) {
        this.key = Util.makeDescriptionId(type, CryoSat.rl(path));
    }

    @NotNull
    @Override
    public String getTranslationKey() {
        return key;
    }
}
