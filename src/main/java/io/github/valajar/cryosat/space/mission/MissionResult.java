package io.github.valajar.cryosat.space.mission;

/**
 * Outcome of a {@link MissionAction}, shown to the player on the action bar.
 */
public enum MissionResult {
    RENAMED(true, MissionLang.RESULT_RENAMED),
    RELINKED(true, MissionLang.RESULT_RELINKED),
    UNLINKED(true, MissionLang.RESULT_UNLINKED),
    DECOMMISSIONED(true, MissionLang.RESULT_DECOMMISSIONED),
    /** The asset does not exist or belongs to someone else; both look the same, so nothing leaks about other players' assets. */
    NOT_YOURS(false, MissionLang.RESULT_NOT_YOURS),
    /** Relink without an Uplink Card holding a target in the slot. */
    NO_CARD(false, MissionLang.RESULT_NO_CARD);

    private final boolean success;
    private final MissionLang message;

    MissionResult(boolean success, MissionLang message) {
        this.success = success;
        this.message = message;
    }

    public boolean success() {
        return success;
    }

    public MissionLang message() {
        return message;
    }
}
