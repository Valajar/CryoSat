package io.github.valajar.cryosat.space.locator;

import io.github.valajar.cryosat.CryoSat;
import mekanism.api.text.ILangEntry;
import net.minecraft.Util;
import org.jetbrains.annotations.NotNull;

/**
 * Translation keys of the Survey Locator. English text lives in {@code datagen/locator/LocatorLangData}.
 */
public enum LocatorLang implements ILangEntry {
    NO_TARGETS("tooltip", "survey_locator.no_targets"),
    ENTRY("tooltip", "survey_locator.entry"),
    EXACT("tooltip", "survey_locator.exact"),
    WITHIN("tooltip", "survey_locator.within"),
    USAGE("tooltip", "survey_locator.usage"),
    SELECTED("survey_locator", "selected");

    private final String key;

    LocatorLang(String type, String path) {
        this.key = Util.makeDescriptionId(type, CryoSat.rl(path));
    }

    @NotNull
    @Override
    public String getTranslationKey() {
        return key;
    }
}
