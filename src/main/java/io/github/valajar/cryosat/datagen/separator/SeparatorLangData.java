package io.github.valajar.cryosat.datagen.separator;

import io.github.valajar.cryosat.registries.CryoBlocks;
import io.github.valajar.cryosat.superconductor.separator.SeparatorLang;
import java.util.function.BiConsumer;
import net.minecraft.Util;
import net.minecraft.core.registries.BuiltInRegistries;
import net.neoforged.neoforge.common.data.LanguageProvider;

/**
 * English text for the Superconducting Separator, added by the main language provider. The coil's name and the shared
 * cryostat text live in {@code SuperconductorLangData}.
 */
public final class SeparatorLangData {
    public static void addTo(LanguageProvider provider) {
        provider.addBlock(CryoBlocks.SUPERCONDUCTING_SEPARATOR, "Superconducting Separator");
        // GUI title, looked up by Mekanism under container.<ns>.<name>
        provider.add(Util.makeDescriptionId("container", BuiltInRegistries.BLOCK.getKey(CryoBlocks.SUPERCONDUCTING_SEPARATOR.get())), "Superconducting Separator");

        BiConsumer<SeparatorLang, String> add = (lang, text) -> provider.add(lang.getTranslationKey(), text);
        add.accept(SeparatorLang.DESCRIPTION, "Sorts cargo from orbit as it reaches a Cargo Receiver next to it: every batch comes out 50 percent "
                                              + "bigger, with a chance of rare extras. Needs liquid nitrogen to stay cold and gives most of it back as "
                                              + "nitrogen gas.");

        add.accept(SeparatorLang.BATCHES, "Batches separated: %1$s");
        add.accept(SeparatorLang.NO_RECEIVER, "Place next to a Cargo Receiver");
        add.accept(SeparatorLang.REDSTONE_OFF, "Paused by redstone");
        add.accept(SeparatorLang.READY, "Waiting for cargo");
        add.accept(SeparatorLang.TOOLTIP_BONUS, "Each batch arriving next door: +50 percent and the asteroid's extras");
        add.accept(SeparatorLang.TOOLTIP_PER_BATCH, "Liquid nitrogen per batch: %1$s mB");
        add.accept(SeparatorLang.TOOLTIP_TRICKLE, "Keeping cold: %1$s mB/t");
    }

    private SeparatorLangData() {
    }
}
