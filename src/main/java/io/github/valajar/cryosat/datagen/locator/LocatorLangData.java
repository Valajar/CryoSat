package io.github.valajar.cryosat.datagen.locator;

import io.github.valajar.cryosat.registries.CryoItems;
import io.github.valajar.cryosat.space.locator.LocatorLang;
import java.util.function.BiConsumer;
import net.neoforged.neoforge.common.data.LanguageProvider;

/**
 * English text for the Survey Locator, added by the main language provider.
 */
public final class LocatorLangData {
    public static void addTo(LanguageProvider provider) {
        provider.addItem(CryoItems.SURVEY_LOCATOR, "Survey Locator");

        BiConsumer<LocatorLang, String> add = (lang, text) -> provider.add(lang.getTranslationKey(), text);
        add.accept(LocatorLang.NO_TARGETS, "No targets. Load survey results at an Imaging Terminal.");
        add.accept(LocatorLang.ENTRY, "%1$s at %2$s, %3$s");
        add.accept(LocatorLang.EXACT, "exact");
        add.accept(LocatorLang.WITHIN, "within %1$s blocks");
        add.accept(LocatorLang.USAGE, "Sneak use to switch target");
        add.accept(LocatorLang.SELECTED, "Target %1$s of %2$s: %3$s at %4$s, %5$s");
    }

    private LocatorLangData() {
    }
}
