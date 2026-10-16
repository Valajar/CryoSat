package io.github.valajar.cryosat.machine;

import io.github.valajar.cryosat.CryoSat;
import net.minecraft.network.chat.Component;
import net.minecraft.util.StringRepresentable;

/**
 * Why a machine is or is not working, shown in its GUI. Shared across machines so the wording stays consistent.
 */
public enum MachineStatus implements StringRepresentable {
    IDLE("idle"),
    RUNNING("running"),
    NO_POWER("no_power"),
    OUTPUT_FULL("output_full"),
    OUTPUT_MISMATCH("output_mismatch"),
    NO_ATMOSPHERE("no_atmosphere"),
    NO_AIR("no_air");

    private final String name;

    MachineStatus(String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return name;
    }

    public String translationKey() {
        return "status." + CryoSat.MODID + "." + name;
    }

    public Component text() {
        return Component.translatable(translationKey());
    }

    public static MachineStatus byName(String name) {
        for (MachineStatus status : values()) {
            if (status.name.equals(name)) {
                return status;
            }
        }
        return IDLE;
    }
}
