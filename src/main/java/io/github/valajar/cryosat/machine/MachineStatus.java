package io.github.valajar.cryosat.machine;

import io.github.valajar.cryosat.CryoSat;
import java.util.function.IntFunction;
import mekanism.api.text.ILangEntry;
import net.minecraft.util.ByIdMap;

/**
 * Why a machine is or is not working, shown in its GUI. Shared across machines so the wording stays consistent.
 */
public enum MachineStatus implements ILangEntry {
    IDLE("idle"),
    RUNNING("running"),
    NO_POWER("no_power"),
    OUTPUT_FULL("output_full"),
    OUTPUT_MISMATCH("output_mismatch"),
    NO_ATMOSPHERE("no_atmosphere"),
    NO_AIR("no_air");

    public static final IntFunction<MachineStatus> BY_ID = ByIdMap.continuous(MachineStatus::ordinal, values(), ByIdMap.OutOfBoundsStrategy.ZERO);

    private final String name;

    MachineStatus(String name) {
        this.name = name;
    }

    @Override
    public String getTranslationKey() {
        return "status." + CryoSat.MODID + "." + name;
    }
}
