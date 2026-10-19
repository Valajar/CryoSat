package io.github.valajar.cryosat.datagen.squid;

import io.github.valajar.cryosat.registries.CryoBlocks;
import io.github.valajar.cryosat.superconductor.squid.SquidLang;
import java.util.function.BiConsumer;
import net.minecraft.Util;
import net.minecraft.core.registries.BuiltInRegistries;
import net.neoforged.neoforge.common.data.LanguageProvider;

/**
 * English text for the SQUID Array, added by the main language provider. The shared cryostat text lives in
 * {@code SuperconductorLangData}.
 */
public final class SquidLangData {
    public static void addTo(LanguageProvider provider) {
        provider.addBlock(CryoBlocks.SQUID_ARRAY, "SQUID Array");
        // GUI title, looked up by Mekanism under container.<ns>.<name>
        provider.add(Util.makeDescriptionId("container", BuiltInRegistries.BLOCK.getKey(CryoBlocks.SQUID_ARRAY.get())), "SQUID Array");

        BiConsumer<SquidLang, String> add = (lang, text) -> provider.add(lang.getTranslationKey(), text);
        add.accept(SquidLang.DESCRIPTION, "A SQUID (superconducting quantum interference device) is a magnetometer sensitive enough to pick up "
                                          + "buried structures. Next to an Imaging Terminal it lets surveys find ancient cities, strongholds, "
                                          + "trial chambers and mineshafts. Needs liquid nitrogen to stay cold and gives most of it back as "
                                          + "nitrogen gas.");

        add.accept(SquidLang.SCANS, "Deep scans: %1$s");
        add.accept(SquidLang.NO_TERMINAL, "Place next to an Imaging Terminal");
        add.accept(SquidLang.REDSTONE_OFF, "Paused by redstone");
        add.accept(SquidLang.LOW_COOLANT, "Not enough coolant for a scan");
        add.accept(SquidLang.READY, "Ready for deep scans");
        add.accept(SquidLang.TOOLTIP_PURPOSE, "Lets the Imaging Terminal next door search underground");
        add.accept(SquidLang.TOOLTIP_PER_SCAN, "Liquid nitrogen per scan: %1$s mB");
        add.accept(SquidLang.TOOLTIP_TRICKLE, "Keeping cold: %1$s mB/t");
    }

    private SquidLangData() {
    }
}
