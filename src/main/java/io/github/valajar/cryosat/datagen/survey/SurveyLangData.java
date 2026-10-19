package io.github.valajar.cryosat.datagen.survey;

import io.github.valajar.cryosat.registries.CryoBlocks;
import io.github.valajar.cryosat.space.survey.SurveyLang;
import java.util.function.BiConsumer;
import net.minecraft.Util;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.common.data.LanguageProvider;

/**
 * English text of the Imaging Terminal. Added by the main language provider.
 */
public final class SurveyLangData {
    public static void addTo(LanguageProvider provider) {
        Block block = CryoBlocks.IMAGING_TERMINAL.get();
        provider.add(block, "Imaging Terminal");
        // GUI title, which Mekanism looks up under container.<ns>.<name>
        provider.add(Util.makeDescriptionId("container", BuiltInRegistries.BLOCK.getKey(block)), "Imaging Terminal");

        BiConsumer<SurveyLang, String> add = (lang, text) -> provider.add(lang.getTranslationKey(), text);
        add.accept(SurveyLang.DESCRIPTION, "Requests imaging passes from your Survey Satellites to find structures. More and better satellites give "
                                           + "wider searches and more precise results. Underground structures need a SQUID Array next to the terminal. A "
                                           + "redstone signal repeats the last request.");

        add.accept(SurveyLang.NO_TARGETS, "No survey targets loaded");
        add.accept(SurveyLang.DEEP_TARGET, "Underground, needs a SQUID Array");
        add.accept(SurveyLang.BUTTON_REQUEST, "Request pass");
        add.accept(SurveyLang.BUTTON_LOAD, "Load");
        add.accept(SurveyLang.LOAD_TOOLTIP, "Store the selected log entry on the Survey Locator in the slot");

        add.accept(SurveyLang.SATELLITES, "Satellites: %1$s, %2$s free");
        add.accept(SurveyLang.NO_SATELLITES, "No Survey Satellites in orbit");
        add.accept(SurveyLang.COVERAGE, "Coverage %1$s, range %2$s");
        add.accept(SurveyLang.UNCERTAINTY, "Uncertainty: %1$s blocks");
        add.accept(SurveyLang.EXACT, "Exact positions");
        add.accept(SurveyLang.PASS, "%1$s in %2$s");
        add.accept(SurveyLang.MORE_PASSES, "and %1$s more passes");
        add.accept(SurveyLang.DURATION, "%1$sm %2$ss");
        add.accept(SurveyLang.ENERGY_PER_REQUEST, "Per request: %1$s");

        add.accept(SurveyLang.LOG_EMPTY, "No results yet");
        add.accept(SurveyLang.LOG_TITLE, "%1$s, %2$s");
        add.accept(SurveyLang.AGE_MINUTES, "%1$sm ago");
        add.accept(SurveyLang.AGE_HOURS, "%1$sh ago");
        add.accept(SurveyLang.LOG_FOUND, "%1$s, %2$s within %3$s, %4$s away");
        add.accept(SurveyLang.LOG_FOUND_EXACT, "%1$s, %2$s exact, %3$s away");
        add.accept(SurveyLang.LOG_NOT_FOUND, "Nothing new within range");
        add.accept(SurveyLang.LOG_NO_DEEP_SCAN, "Deep scan unavailable");

        add.accept(SurveyLang.REQUEST_SENT, "Imaging pass requested");
        add.accept(SurveyLang.REQUEST_UNKNOWN_TARGET, "That target cannot be found in this world");
        add.accept(SurveyLang.REQUEST_NEEDS_DEEP_SCAN, "Underground targets need a ready SQUID Array next to the terminal");
        add.accept(SurveyLang.REQUEST_NO_SATELLITES, "You have no Survey Satellites over this world");
        add.accept(SurveyLang.REQUEST_ALL_BUSY, "Every satellite is busy with a pass");
        add.accept(SurveyLang.REQUEST_NO_POWER, "Not enough energy for a request");
        add.accept(SurveyLang.REQUEST_NO_ACCESS, "You cannot use this terminal");
        add.accept(SurveyLang.LOADED, "Result stored on the Survey Locator");
        add.accept(SurveyLang.NO_LOCATOR, "Put a Survey Locator in the slot and select a found result");
    }

    private SurveyLangData() {
    }
}
