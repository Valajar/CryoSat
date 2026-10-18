package io.github.valajar.cryosat.superconductor.fusion;

import io.github.valajar.cryosat.CryoSat;
import mekanism.api.text.ILangEntry;
import net.minecraft.Util;
import org.jetbrains.annotations.NotNull;

/**
 * Translation keys of the Fusion Field Coils. English text in {@code datagen/fusion/FusionCoilLangData}.
 */
public enum FusionCoilLang implements ILangEntry {
    DESCRIPTION_FIELD_COIL("description", "field_coil"),
    DESCRIPTION_CONTROLLER("description", "field_coil_controller"),

    LOOPS("gui", "field_coil.loops"),
    BONUS("gui", "field_coil.bonus"),
    REACTOR_OUTPUT("gui", "field_coil.reactor_output"),
    REACTOR_IDLE("gui", "field_coil.reactor_idle"),
    REDSTONE_OFF("gui", "field_coil.redstone_off"),
    STEAM_NOTE("gui", "field_coil.steam_note"),
    COOLANT_RULE("gui", "field_coil.coolant_rule"),
    BOOST_RULE("gui", "field_coil.boost_rule"),
    INCOMPLETE_LOOP("gui", "field_coil.incomplete_loop"),

    RING_COMPLETE("gui", "field_coil.problem.none"),
    NEEDS_GENERATORS("gui", "field_coil.problem.needs_generators"),
    NO_REACTOR("gui", "field_coil.problem.no_reactor"),
    REACTOR_NOT_FORMED("gui", "field_coil.problem.reactor_not_formed"),
    WRONG_LAYER("gui", "field_coil.problem.wrong_layer"),
    WRONG_GAP("gui", "field_coil.problem.wrong_gap"),
    BROKEN_LOOP("gui", "field_coil.problem.broken_loop"),
    GAP_BLOCKED("gui", "field_coil.problem.gap_blocked"),
    SECOND_CONTROLLER("gui", "field_coil.problem.second_controller");

    private final String key;

    FusionCoilLang(String type, String path) {
        this.key = Util.makeDescriptionId(type, CryoSat.rl(path));
    }

    @NotNull
    @Override
    public String getTranslationKey() {
        return key;
    }
}
