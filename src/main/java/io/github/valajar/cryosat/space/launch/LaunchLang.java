package io.github.valajar.cryosat.space.launch;

import io.github.valajar.cryosat.CryoSat;
import mekanism.api.text.ILangEntry;
import net.minecraft.Util;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

/**
 * Translation keys of the Launch Pad and its Rocket Assembly screen. English text lives in {@code datagen/launch/LaunchLangData}.
 */
public enum LaunchLang implements ILangEntry {
    ROCKET_ASSEMBLY("gui", "rocket_assembly"),
    DESCRIPTION_PLATING("description", "launch_pad_plating"),
    DESCRIPTION_CONTROLLER("description", "launch_controller"),
    DESCRIPTION_FUEL_PORT("description", "launch_pad_fuel_port"),

    INVALID_INNER_PART("multiblock", "launch_pad.inner_part"),

    BUTTON_ASSEMBLE("gui", "launch_pad.assemble"),
    BUTTON_DISASSEMBLE("gui", "launch_pad.disassemble"),
    BUTTON_LAUNCH("gui", "launch_pad.launch"),
    DESTINATION("gui", "launch_pad.destination"),
    DESTINATION_TOOLTIP("gui", "launch_pad.destination.tooltip"),
    NO_ROCKET("gui", "launch_pad.no_rocket"),
    ROCKET("gui", "launch_pad.rocket"),
    PAYLOAD("gui", "launch_pad.payload"),
    FUEL_GRAINS("gui", "launch_pad.fuel.grains"),
    FUEL_OXIDISER("gui", "launch_pad.fuel.oxidiser"),
    FUEL_PROPELLANT("gui", "launch_pad.fuel.propellant"),
    GAUGE_OXIDISER("gui", "launch_pad.gauge.oxidiser"),
    GAUGE_PROPELLANT("gui", "launch_pad.gauge.propellant"),
    SLOT_UPLINK("gui", "launch_pad.slot.uplink"),
    SLOT_SOLID_FUEL("gui", "launch_pad.slot.solid_fuel"),

    STATUS_ASSEMBLY_PROBLEM("status", "launch_pad.assembly_problem"),
    STATUS_READY_TO_ASSEMBLE("status", "launch_pad.ready_to_assemble"),
    STATUS_MISSION_PROBLEM("status", "launch_pad.mission_problem"),
    STATUS_NEEDS_FUEL("status", "launch_pad.needs_fuel"),
    STATUS_OBSTRUCTED("status", "launch_pad.obstructed"),
    STATUS_NO_OWNER("status", "launch_pad.no_owner"),
    STATUS_READY("status", "launch_pad.ready"),
    STATUS_LAUNCHING("status", "launch_pad.launching");

    private final String key;

    LaunchLang(String type, String path) {
        this.key = Util.makeDescriptionId(type, CryoSat.rl(path));
    }

    @NotNull
    @Override
    public String getTranslationKey() {
        return key;
    }

    /** Translation key of a destination's display name: {@code destination.<namespace>.<path>}. */
    public static String destinationKey(ResourceLocation destination) {
        return Util.makeDescriptionId("destination", destination);
    }

    /** A destination's display name, falling back to its id for datapack destinations without a translation. */
    public static Component destinationName(ResourceLocation destination) {
        return Component.translatableWithFallback(destinationKey(destination), destination.toString());
    }
}
