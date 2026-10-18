package io.github.valajar.cryosat.space.receiver;

import io.github.valajar.cryosat.CryoSat;
import mekanism.api.text.ILangEntry;
import net.minecraft.Util;
import org.jetbrains.annotations.NotNull;

/**
 * Translation keys of the Cargo Receiver and the Uplink Card. English text lives in
 * {@code datagen/receiver/ReceiverLangData}.
 */
public enum ReceiverLang implements ILangEntry {
    DESCRIPTION_CARGO_RECEIVER("description", "cargo_receiver"),

    MISSIONS("gui", "cargo_receiver.missions"),
    NO_MISSIONS("gui", "cargo_receiver.no_missions"),
    MISSION_LINE("gui", "cargo_receiver.mission"),
    MISSION_LINE_WAITING("gui", "cargo_receiver.mission_waiting"),
    MORE_MISSIONS("gui", "cargo_receiver.more_missions"),
    MISSION_BATCHES("gui", "cargo_receiver.batches"),
    MISSION_WAITING("gui", "cargo_receiver.waiting"),

    UPLINK_TARGET("tooltip", "uplink_card.target"),
    UPLINK_NO_TARGET("tooltip", "uplink_card.no_target"),
    UPLINK_USAGE("tooltip", "uplink_card.usage"),
    UPLINK_SET("uplink_card", "set"),
    UPLINK_CLEARED("uplink_card", "cleared");

    private final String key;

    ReceiverLang(String type, String path) {
        this.key = Util.makeDescriptionId(type, CryoSat.rl(path));
    }

    @NotNull
    @Override
    public String getTranslationKey() {
        return key;
    }
}
