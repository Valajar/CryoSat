package io.github.valajar.cryosat.space.survey;

import io.github.valajar.cryosat.CryoSat;
import mekanism.api.text.ILangEntry;
import net.minecraft.Util;
import org.jetbrains.annotations.NotNull;

/**
 * Translation keys of the Imaging Terminal. English text lives in {@code datagen/survey/SurveyLangData}.
 */
public enum SurveyLang implements ILangEntry {
    DESCRIPTION("description", "imaging_terminal"),

    NO_TARGETS("imaging_terminal", "no_targets"),
    DEEP_TARGET("imaging_terminal", "deep_target"),
    BUTTON_REQUEST("imaging_terminal", "button.request"),
    BUTTON_LOAD("imaging_terminal", "button.load"),
    LOAD_TOOLTIP("imaging_terminal", "load_tooltip"),

    SATELLITES("imaging_terminal", "satellites"),
    NO_SATELLITES("imaging_terminal", "no_satellites"),
    COVERAGE("imaging_terminal", "coverage"),
    UNCERTAINTY("imaging_terminal", "uncertainty"),
    EXACT("imaging_terminal", "exact"),
    PASS("imaging_terminal", "pass"),
    MORE_PASSES("imaging_terminal", "more_passes"),
    DURATION("imaging_terminal", "duration"),
    ENERGY_PER_REQUEST("imaging_terminal", "energy_per_request"),

    LOG_EMPTY("imaging_terminal", "log_empty"),
    LOG_TITLE("imaging_terminal", "log.title"),
    AGE_MINUTES("imaging_terminal", "age.minutes"),
    AGE_HOURS("imaging_terminal", "age.hours"),
    LOG_FOUND("imaging_terminal", "log.found"),
    LOG_FOUND_EXACT("imaging_terminal", "log.found_exact"),
    LOG_NOT_FOUND("imaging_terminal", "log.not_found"),
    LOG_NO_DEEP_SCAN("imaging_terminal", "log.no_deep_scan"),

    REQUEST_SENT("imaging_terminal", "result.requested"),
    REQUEST_UNKNOWN_TARGET("imaging_terminal", "result.unknown_target"),
    REQUEST_NEEDS_DEEP_SCAN("imaging_terminal", "result.needs_deep_scan"),
    REQUEST_NO_SATELLITES("imaging_terminal", "result.no_satellites"),
    REQUEST_ALL_BUSY("imaging_terminal", "result.all_busy"),
    REQUEST_NO_POWER("imaging_terminal", "result.no_power"),
    REQUEST_NO_ACCESS("imaging_terminal", "result.no_access"),
    LOADED("imaging_terminal", "result.loaded"),
    NO_LOCATOR("imaging_terminal", "result.no_locator");

    private final String key;

    SurveyLang(String type, String path) {
        this.key = Util.makeDescriptionId(type, CryoSat.rl(path));
    }

    @NotNull
    @Override
    public String getTranslationKey() {
        return key;
    }
}
