package io.github.valajar.cryosat.datagen.launch;

import io.github.valajar.cryosat.registries.CryoBlocks;
import io.github.valajar.cryosat.space.launch.LaunchLang;
import java.util.function.BiConsumer;
import net.neoforged.neoforge.common.data.LanguageProvider;

/**
 * English text for the Launch Pad and Rocket Assembly, added by the main language provider.
 */
public final class LaunchLangData {
    public static void addTo(LanguageProvider provider) {
        provider.addBlock(CryoBlocks.LAUNCH_PAD_PLATING, "Launch Pad Plating");
        provider.addBlock(CryoBlocks.LAUNCH_PAD_FUEL_PORT, "Launch Pad Fuel Port");
        provider.addBlock(CryoBlocks.LAUNCH_CONTROLLER, "Launch Controller");

        BiConsumer<LaunchLang, String> add = (lang, text) -> provider.add(lang.getTranslationKey(), text);
        add.accept(LaunchLang.ROCKET_ASSEMBLY, "Rocket Assembly");
        add.accept(LaunchLang.DESCRIPTION_PLATING, "Deck of the Launch Pad, a flat 5x5 platform. Keep the 3x3 column above its centre clear for the rocket.");
        add.accept(LaunchLang.DESCRIPTION_CONTROLLER, "Brain of the Launch Pad and home of the Rocket Assembly. Exactly one per pad, on the outer ring. "
              + "A redstone pulse launches a ready rocket.");
        add.accept(LaunchLang.DESCRIPTION_FUEL_PORT, "Fuels the Launch Pad from Mekanism tubes and item pipes: oxidiser, propellant and solid propellant "
              + "grains. Goes on the outer ring of the pad.");

        add.accept(LaunchLang.INVALID_INNER_PART, "Controllers and fuel ports go on the outer ring of the pad, not at %1$s.");

        add.accept(LaunchLang.BUTTON_ASSEMBLE, "Assemble");
        add.accept(LaunchLang.BUTTON_DISASSEMBLE, "Dismantle");
        add.accept(LaunchLang.BUTTON_LAUNCH, "Launch");
        add.accept(LaunchLang.DESTINATION, "To: %1$s");
        add.accept(LaunchLang.DESTINATION_TOOLTIP, "Click for the next destination, right click for the previous one");
        add.accept(LaunchLang.NO_ROCKET, "No rocket assembled");
        add.accept(LaunchLang.ROCKET, "Rocket: %1$s");
        add.accept(LaunchLang.PAYLOAD, "Payload: %1$s");
        add.accept(LaunchLang.FUEL_GRAINS, "Grains: %1$s/%2$s");
        add.accept(LaunchLang.FUEL_OXIDISER, "Oxidiser: %1$s/%2$s mB");
        add.accept(LaunchLang.FUEL_PROPELLANT, "Propellant: %1$s/%2$s mB");
        add.accept(LaunchLang.GAUGE_OXIDISER, "Oxidiser");
        add.accept(LaunchLang.GAUGE_PROPELLANT, "Propellant");
        add.accept(LaunchLang.SLOT_UPLINK, "Uplink Card (optional): where the cargo goes");
        add.accept(LaunchLang.SLOT_SOLID_FUEL, "Solid Propellant Grains");

        add.accept(LaunchLang.STATUS_ASSEMBLY_PROBLEM, "Cannot assemble");
        add.accept(LaunchLang.STATUS_READY_TO_ASSEMBLE, "Ready to assemble");
        add.accept(LaunchLang.STATUS_MISSION_PROBLEM, "Cannot launch");
        add.accept(LaunchLang.STATUS_NEEDS_FUEL, "Needs fuel");
        add.accept(LaunchLang.STATUS_OBSTRUCTED, "Launch column blocked");
        add.accept(LaunchLang.STATUS_NO_OWNER, "Nobody owns this pad");
        add.accept(LaunchLang.STATUS_READY, "Ready for launch");
        add.accept(LaunchLang.STATUS_LAUNCHING, "Launching");
    }

    private LaunchLangData() {
    }
}
