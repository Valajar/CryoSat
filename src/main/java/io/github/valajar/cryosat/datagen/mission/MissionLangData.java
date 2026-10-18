package io.github.valajar.cryosat.datagen.mission;

import io.github.valajar.cryosat.registries.CryoBlocks;
import io.github.valajar.cryosat.space.mission.MissionLang;
import java.util.function.BiConsumer;
import net.minecraft.Util;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.common.data.LanguageProvider;

/**
 * English text of Mission Control, plus the names of the default destination, asteroid class and asteroid sizes it
 * shows. Added by the main language provider.
 */
public final class MissionLangData {
    public static void addTo(LanguageProvider provider) {
        Block block = CryoBlocks.MISSION_CONTROL.get();
        provider.add(block, "Mission Control");
        // GUI title, which Mekanism looks up under container.<ns>.<name>
        provider.add(Util.makeDescriptionId("container", BuiltInRegistries.BLOCK.getKey(block)), "Mission Control");

        BiConsumer<MissionLang, String> add = (lang, text) -> provider.add(lang.getTranslationKey(), text);
        add.accept(MissionLang.DESCRIPTION, "Lists your missions in orbit. Rename them, link them to a Cargo Receiver with an Uplink Card, or "
                                            + "decommission them. Each player sees their own missions.");

        add.accept(MissionLang.NO_MISSIONS, "No missions in orbit");
        add.accept(MissionLang.SELECT_MISSION, "Select a mission");
        add.accept(MissionLang.DESTINATION, "Destination: %1$s");
        add.accept(MissionLang.ASTEROID, "Asteroid: %1$s, %2$s");
        add.accept(MissionLang.HARDNESS, "Hardness: %1$s");
        add.accept(MissionLang.EXTRACTED, "Extracted share: %1$s");
        add.accept(MissionLang.BATCHES, "Batches: %1$s of %2$s");
        add.accept(MissionLang.ARRIVES_IN, "Arrives in %1$s");
        add.accept(MissionLang.NEXT_BATCH_IN, "Next batch in %1$s");
        add.accept(MissionLang.MINING_COMPLETE, "Mining complete");
        add.accept(MissionLang.DURATION, "%1$sm %2$ss");
        add.accept(MissionLang.CARGO, "Cargo in orbit: %1$s");
        add.accept(MissionLang.RECEIVER, "Receiver: %1$s");
        add.accept(MissionLang.RECEIVER_POSITION, "%1$s, %2$s, %3$s in %4$s");
        add.accept(MissionLang.NOT_LINKED, "not linked");
        add.accept(MissionLang.NAME, "Name:");

        add.accept(MissionLang.BUTTON_RELINK, "Relink");
        add.accept(MissionLang.BUTTON_UNLINK, "Unlink");
        add.accept(MissionLang.BUTTON_DECOMMISSION, "Decommission");
        add.accept(MissionLang.RELINK_TOOLTIP, "Link the selected mission to the receiver stored on the Uplink Card in the slot");
        add.accept(MissionLang.DECOMMISSION_CONFIRM, "Decommission %1$s? It is gone for good, along with any cargo still in orbit.");

        add.accept(MissionLang.RESULT_RENAMED, "Mission renamed");
        add.accept(MissionLang.RESULT_RELINKED, "Mission linked to the receiver on the Uplink Card");
        add.accept(MissionLang.RESULT_UNLINKED, "Mission unlinked, cargo will wait in orbit");
        add.accept(MissionLang.RESULT_DECOMMISSIONED, "Mission decommissioned");
        add.accept(MissionLang.RESULT_NOT_YOURS, "That mission is not yours");
        add.accept(MissionLang.RESULT_NO_CARD, "Put a linked Uplink Card in the slot first");
    }

    private MissionLangData() {
    }
}
