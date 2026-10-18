package io.github.valajar.cryosat.superconductor.separator;

import io.github.valajar.cryosat.CryoSat;
import mekanism.api.text.ILangEntry;
import net.minecraft.Util;
import org.jetbrains.annotations.NotNull;

/**
 * Translation keys of the Superconducting Separator. English text lives in {@code datagen/separator/SeparatorLangData}.
 */
public enum SeparatorLang implements ILangEntry {
    DESCRIPTION("description", "superconducting_separator"),

    BATCHES("gui", "superconducting_separator.batches"),
    NO_RECEIVER("gui", "superconducting_separator.no_receiver"),
    REDSTONE_OFF("gui", "superconducting_separator.redstone_off"),
    READY("gui", "superconducting_separator.ready"),
    TOOLTIP_BONUS("gui", "superconducting_separator.bonus"),
    TOOLTIP_PER_BATCH("gui", "superconducting_separator.per_batch"),
    TOOLTIP_TRICKLE("gui", "superconducting_separator.trickle");

    private final String key;

    SeparatorLang(String type, String path) {
        this.key = Util.makeDescriptionId(type, CryoSat.rl(path));
    }

    @NotNull
    @Override
    public String getTranslationKey() {
        return key;
    }
}
