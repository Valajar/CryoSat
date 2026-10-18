package io.github.valajar.cryosat.space.mission;

import io.github.valajar.cryosat.CryoSat;
import mekanism.api.text.ILangEntry;
import net.minecraft.Util;
import org.jetbrains.annotations.NotNull;

/**
 * Translation keys of Mission Control. English text lives in {@code datagen/mission/MissionLangData}.
 */
public enum MissionLang implements ILangEntry {
    DESCRIPTION("description", "mission_control"),

    NO_MISSIONS("mission_control", "no_missions"),
    SELECT_MISSION("mission_control", "select_mission"),
    DESTINATION("mission_control", "destination"),
    ASTEROID("mission_control", "asteroid"),
    HARDNESS("mission_control", "hardness"),
    EXTRACTED("mission_control", "extracted"),
    BATCHES("mission_control", "batches"),
    ARRIVES_IN("mission_control", "arrives_in"),
    NEXT_BATCH_IN("mission_control", "next_batch_in"),
    MINING_COMPLETE("mission_control", "mining_complete"),
    DURATION("mission_control", "duration"),
    CARGO("mission_control", "cargo"),
    RECEIVER("mission_control", "receiver"),
    RECEIVER_POSITION("mission_control", "receiver_position"),
    NOT_LINKED("mission_control", "not_linked"),
    NAME("mission_control", "name"),

    BUTTON_RELINK("mission_control", "button.relink"),
    BUTTON_UNLINK("mission_control", "button.unlink"),
    BUTTON_DECOMMISSION("mission_control", "button.decommission"),
    RELINK_TOOLTIP("mission_control", "relink_tooltip"),
    DECOMMISSION_CONFIRM("mission_control", "decommission_confirm"),

    RESULT_RENAMED("mission_control", "result.renamed"),
    RESULT_RELINKED("mission_control", "result.relinked"),
    RESULT_UNLINKED("mission_control", "result.unlinked"),
    RESULT_DECOMMISSIONED("mission_control", "result.decommissioned"),
    RESULT_NOT_YOURS("mission_control", "result.not_yours"),
    RESULT_NO_CARD("mission_control", "result.no_card");

    private final String key;

    MissionLang(String type, String path) {
        this.key = Util.makeDescriptionId(type, CryoSat.rl(path));
    }

    @NotNull
    @Override
    public String getTranslationKey() {
        return key;
    }
}
