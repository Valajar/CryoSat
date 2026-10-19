package io.github.valajar.cryosat.space.survey;

/**
 * Outcome of an Imaging Terminal action, shown to the player on the action bar.
 */
public enum RequestResult {
    REQUESTED(SurveyLang.REQUEST_SENT),
    /** The target is not loaded, or none of its structures exist in this world. */
    UNKNOWN_TARGET(SurveyLang.REQUEST_UNKNOWN_TARGET),
    /** An underground target without a ready SQUID Array next to the terminal. */
    NEEDS_DEEP_SCAN(SurveyLang.REQUEST_NEEDS_DEEP_SCAN),
    NO_SATELLITES(SurveyLang.REQUEST_NO_SATELLITES),
    ALL_BUSY(SurveyLang.REQUEST_ALL_BUSY),
    NO_POWER(SurveyLang.REQUEST_NO_POWER),
    NO_ACCESS(SurveyLang.REQUEST_NO_ACCESS),
    LOADED(SurveyLang.LOADED),
    /** Load without a Survey Locator in the slot, or the entry has no location. */
    NO_LOCATOR(SurveyLang.NO_LOCATOR);

    private final SurveyLang message;

    RequestResult(SurveyLang message) {
        this.message = message;
    }

    public SurveyLang message() {
        return message;
    }
}
