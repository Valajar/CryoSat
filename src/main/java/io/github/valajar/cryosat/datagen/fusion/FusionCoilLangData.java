package io.github.valajar.cryosat.datagen.fusion;

import io.github.valajar.cryosat.registries.CryoBlocks;
import io.github.valajar.cryosat.superconductor.fusion.FusionCoilLang;
import java.util.function.BiConsumer;
import net.minecraft.Util;
import net.minecraft.core.registries.BuiltInRegistries;
import net.neoforged.neoforge.common.data.LanguageProvider;

/**
 * English text for the Fusion Field Coils, added by the main language provider.
 */
public final class FusionCoilLangData {
    public static void addTo(LanguageProvider provider) {
        provider.addBlock(CryoBlocks.FIELD_COIL, "Field Coil");
        provider.addBlock(CryoBlocks.FIELD_COIL_CONTROLLER, "Field Coil Controller");
        // GUI title, looked up by Mekanism under container.<ns>.<name>
        provider.add(Util.makeDescriptionId("container", BuiltInRegistries.BLOCK.getKey(CryoBlocks.FIELD_COIL_CONTROLLER.get())), "Field Coil Controller");

        BiConsumer<FusionCoilLang, String> add = (lang, text) -> provider.add(lang.getTranslationKey(), text);
        add.accept(FusionCoilLang.DESCRIPTION_FIELD_COIL, "Superconducting magnet winding for a ring around a Mekanism fusion reactor. Build a closed "
                                                          + "square loop of them level with the reactor's middle layer, one or two blocks out from it, "
                                                          + "with a Field Coil Controller in the loop.");
        add.accept(FusionCoilLang.DESCRIPTION_CONTROLLER, "Runs a ring of Field Coils around a fusion reactor. While kept cold with liquid nitrogen, "
                                                          + "each complete loop adds 25 percent of the reactor's energy output. Up to three loops: the "
                                                          + "reactor's middle layer and the layers above and below it.");

        add.accept(FusionCoilLang.LOOPS, "Loops: %1$s of %2$s, boost %3$s%%");
        add.accept(FusionCoilLang.BONUS, "Bonus: %1$s/t");
        add.accept(FusionCoilLang.REACTOR_OUTPUT, "Reactor output: %1$s/t");
        add.accept(FusionCoilLang.REACTOR_IDLE, "Reactor not burning");
        add.accept(FusionCoilLang.REDSTONE_OFF, "Turned off by redstone");
        add.accept(FusionCoilLang.STEAM_NOTE, "Steam from water cooling is not boosted");
        add.accept(FusionCoilLang.COOLANT_RULE, "Coolant: 2 mB/t per loop, plus 1 mB per 100 kJ of bonus");
        add.accept(FusionCoilLang.BOOST_RULE, "Each complete loop adds 25 percent of the reactor's energy output");
        add.accept(FusionCoilLang.INCOMPLETE_LOOP, "Unfinished loop at %1$s, %2$s, %3$s");

        add.accept(FusionCoilLang.RING_COMPLETE, "Ring complete, gap %1$s");
        add.accept(FusionCoilLang.NEEDS_GENERATORS, "Needs Mekanism Generators");
        add.accept(FusionCoilLang.NO_REACTOR, "No fusion reactor inside the ring");
        add.accept(FusionCoilLang.REACTOR_NOT_FORMED, "Fusion reactor is not formed");
        add.accept(FusionCoilLang.WRONG_LAYER, "Place the controller level with the reactor's middle three layers");
        add.accept(FusionCoilLang.WRONG_GAP, "Gap to the reactor is %1$s, needs 1 or 2");
        add.accept(FusionCoilLang.BROKEN_LOOP, "Loop broken at %1$s, %2$s, %3$s");
        add.accept(FusionCoilLang.GAP_BLOCKED, "Gap blocked at %1$s, %2$s, %3$s");
        add.accept(FusionCoilLang.SECOND_CONTROLLER, "Second controller at %1$s, %2$s, %3$s");
    }

    private FusionCoilLangData() {
    }
}
