package io.github.valajar.cryosat.space;

import io.github.valajar.cryosat.CryoSat;
import mekanism.api.text.ILangEntry;
import net.minecraft.Util;
import org.jetbrains.annotations.NotNull;

/**
 * Translation keys shared by the space features: rocket parts, mission problems and orbital asset states. Each space
 * block keeps its own lang enum next to it, so parallel work does not collide in one file. English text lives in
 * {@code datagen/space/SpaceLangData}.
 */
public enum SpaceLang implements ILangEntry {
    PART_TOOLTIP("tooltip", "rocket_part"),
    PART_ENGINE("rocket_part", "engine"),
    PART_FUEL_TANK("rocket_part", "fuel_tank"),
    PART_GUIDANCE_COMPUTER("rocket_part", "guidance_computer"),
    PART_PAYLOAD("rocket_part", "payload"),

    PROBLEM_MISSING_ENGINE("mission", "problem.missing_engine"),
    PROBLEM_MISSING_FUEL_TANK("mission", "problem.missing_fuel_tank"),
    PROBLEM_MISSING_GUIDANCE("mission", "problem.missing_guidance"),
    PROBLEM_MISSING_PAYLOAD("mission", "problem.missing_payload"),
    PROBLEM_PAYLOAD_TOO_ADVANCED("mission", "problem.payload_too_advanced"),
    PROBLEM_TIER_CANNOT_LAUNCH("mission", "problem.tier_cannot_launch"),
    PROBLEM_UNKNOWN_DESTINATION("mission", "problem.unknown_destination"),
    PROBLEM_DESTINATION_OUT_OF_REACH("mission", "problem.destination_out_of_reach"),
    PROBLEM_PAYLOAD_NOT_USABLE("mission", "problem.payload_not_usable"),
    PROBLEM_NOTHING_TO_MINE("mission", "problem.nothing_to_mine"),

    STATE_TRAVELLING("mission", "state.travelling"),
    STATE_MINING("mission", "state.mining"),
    STATE_RETIRED("mission", "state.retired"),

    DEFAULT_MINER_NAME("mission", "default_name.asteroid_miner"),
    LAUNCH_ANNOUNCEMENT("mission", "launch_announcement");

    private final String key;

    SpaceLang(String type, String path) {
        this.key = Util.makeDescriptionId(type, CryoSat.rl(path));
    }

    @NotNull
    @Override
    public String getTranslationKey() {
        return key;
    }
}
