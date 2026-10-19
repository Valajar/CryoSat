package io.github.valajar.cryosat.superconductor.squid;

import io.github.valajar.cryosat.CryoSat;
import mekanism.api.text.ILangEntry;
import net.minecraft.Util;
import org.jetbrains.annotations.NotNull;

/**
 * Translation keys of the SQUID Array. English text lives in {@code datagen/squid/SquidLangData}.
 */
public enum SquidLang implements ILangEntry {
    DESCRIPTION("description", "squid_array"),

    SCANS("gui", "squid_array.scans"),
    NO_TERMINAL("gui", "squid_array.no_terminal"),
    REDSTONE_OFF("gui", "squid_array.redstone_off"),
    LOW_COOLANT("gui", "squid_array.low_coolant"),
    READY("gui", "squid_array.ready"),
    TOOLTIP_PURPOSE("gui", "squid_array.purpose"),
    TOOLTIP_PER_SCAN("gui", "squid_array.per_scan"),
    TOOLTIP_TRICKLE("gui", "squid_array.trickle");

    private final String key;

    SquidLang(String type, String path) {
        this.key = Util.makeDescriptionId(type, CryoSat.rl(path));
    }

    @NotNull
    @Override
    public String getTranslationKey() {
        return key;
    }
}
