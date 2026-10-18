package io.github.valajar.cryosat.datagen.receiver;

import io.github.valajar.cryosat.registries.CryoBlocks;
import io.github.valajar.cryosat.registries.CryoItems;
import io.github.valajar.cryosat.space.receiver.ReceiverLang;
import java.util.function.BiConsumer;
import net.minecraft.Util;
import net.minecraft.core.registries.BuiltInRegistries;
import net.neoforged.neoforge.common.data.LanguageProvider;

/**
 * English text for the Cargo Receiver and the Uplink Card, added by the main language provider.
 */
public final class ReceiverLangData {
    public static void addTo(LanguageProvider provider) {
        provider.addBlock(CryoBlocks.CARGO_RECEIVER, "Cargo Receiver");
        // GUI title, looked up by Mekanism under container.<ns>.<name>
        provider.add(Util.makeDescriptionId("container", BuiltInRegistries.BLOCK.getKey(CryoBlocks.CARGO_RECEIVER.get())), "Cargo Receiver");
        provider.addItem(CryoItems.UPLINK_CARD, "Uplink Card");

        BiConsumer<ReceiverLang, String> add = (lang, text) -> provider.add(lang.getTranslationKey(), text);
        add.accept(ReceiverLang.DESCRIPTION_CARGO_RECEIVER, "Catches cargo sent down by missions in orbit and holds it for pipes. Link missions to it with "
                                                            + "an Uplink Card. Cargo that does not fit waits in orbit until there is room.");

        add.accept(ReceiverLang.MISSIONS, "Linked missions: %1$s");
        add.accept(ReceiverLang.NO_MISSIONS, "No missions linked");
        add.accept(ReceiverLang.MISSION_LINE, "%1$s: %2$s");
        add.accept(ReceiverLang.MISSION_LINE_WAITING, "%1$s: %2$s, %3$s waiting");
        add.accept(ReceiverLang.MORE_MISSIONS, "and %1$s more");
        add.accept(ReceiverLang.MISSION_BATCHES, "Batches: %1$s of %2$s");
        add.accept(ReceiverLang.MISSION_WAITING, "Waiting in orbit: %1$s items");

        add.accept(ReceiverLang.UPLINK_TARGET, "Target: %1$s in %2$s");
        add.accept(ReceiverLang.UPLINK_NO_TARGET, "No target");
        add.accept(ReceiverLang.UPLINK_USAGE, "Right click a Cargo Receiver to link it, sneak right click the air to clear");
        add.accept(ReceiverLang.UPLINK_SET, "Uplink set to the Cargo Receiver at %1$s");
        add.accept(ReceiverLang.UPLINK_CLEARED, "Uplink target cleared");
    }

    private ReceiverLangData() {
    }
}
