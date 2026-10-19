package io.github.valajar.cryosat.space;

import mekanism.api.text.IHasTextComponent;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

/**
 * Why a rocket cannot be assembled or launched. Shown in the Rocket Assembly GUI.
 */
public enum MissionProblem implements IHasTextComponent {
    MISSING_ENGINE(SpaceLang.PROBLEM_MISSING_ENGINE),
    MISSING_FUEL_TANK(SpaceLang.PROBLEM_MISSING_FUEL_TANK),
    MISSING_GUIDANCE(SpaceLang.PROBLEM_MISSING_GUIDANCE),
    MISSING_PAYLOAD(SpaceLang.PROBLEM_MISSING_PAYLOAD),
    PAYLOAD_TOO_ADVANCED(SpaceLang.PROBLEM_PAYLOAD_TOO_ADVANCED),
    TIER_CANNOT_LAUNCH(SpaceLang.PROBLEM_TIER_CANNOT_LAUNCH),
    UNKNOWN_DESTINATION(SpaceLang.PROBLEM_UNKNOWN_DESTINATION),
    DESTINATION_OUT_OF_REACH(SpaceLang.PROBLEM_DESTINATION_OUT_OF_REACH),
    PAYLOAD_NOT_USABLE(SpaceLang.PROBLEM_PAYLOAD_NOT_USABLE),
    NOTHING_TO_MINE(SpaceLang.PROBLEM_NOTHING_TO_MINE),
    WRONG_DIMENSION(SpaceLang.PROBLEM_WRONG_DIMENSION);

    private final SpaceLang lang;

    MissionProblem(SpaceLang lang) {
        this.lang = lang;
    }

    @NotNull
    @Override
    public Component getTextComponent() {
        return lang.translate();
    }
}
