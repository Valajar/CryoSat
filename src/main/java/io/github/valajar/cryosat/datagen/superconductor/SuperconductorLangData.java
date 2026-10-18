package io.github.valajar.cryosat.datagen.superconductor;

import io.github.valajar.cryosat.registries.CryoItems;
import io.github.valajar.cryosat.superconductor.SuperconductorLang;
import java.util.function.BiConsumer;
import net.neoforged.neoforge.common.data.LanguageProvider;

/**
 * English text shared by superconducting blocks, added by the main language provider.
 */
public final class SuperconductorLangData {
    public static void addTo(LanguageProvider provider) {
        provider.addItem(CryoItems.SUPERCONDUCTING_COIL, "Superconducting Coil");

        BiConsumer<SuperconductorLang, String> add = (lang, text) -> provider.add(lang.getTranslationKey(), text);
        add.accept(SuperconductorLang.STATE_NO_COOLANT, "Quenched: needs liquid nitrogen");
        add.accept(SuperconductorLang.STATE_COOLING_DOWN, "Cooling down");
        add.accept(SuperconductorLang.STATE_SUPERCONDUCTING, "Superconducting");
        add.accept(SuperconductorLang.COOLDOWN_LEFT, "Ready in %1$s s");
        add.accept(SuperconductorLang.COOLANT_USE, "Coolant: %1$s mB/t");
    }

    private SuperconductorLangData() {
    }
}
