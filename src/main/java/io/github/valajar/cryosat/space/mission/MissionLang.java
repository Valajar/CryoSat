package io.github.valajar.cryosat.space.mission;

import io.github.valajar.cryosat.CryoSat;
import io.github.valajar.cryosat.space.AsteroidSize;
import java.util.Arrays;
import java.util.stream.Collectors;
import mekanism.api.text.ILangEntry;
import net.minecraft.Util;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

/**
 * Translation keys of Mission Control. English text lives in {@code datagen/mission/MissionLangData}.
 */
public enum MissionLang implements ILangEntry {
    DESCRIPTION("description", "mission_control"),

    NO_MISSIONS("mission_control", "no_missions"),
    SELECT_MISSION("mission_control", "select_mission"),
    DESTINATION("mission_control", "destination"),
    ASTEROID("mission_control", "asteroid"),
    HARDNESS("mission_control", "hardness"),
    EXTRACTED("mission_control", "extracted"),
    BATCHES("mission_control", "batches"),
    ARRIVES_IN("mission_control", "arrives_in"),
    NEXT_BATCH_IN("mission_control", "next_batch_in"),
    MINING_COMPLETE("mission_control", "mining_complete"),
    DURATION("mission_control", "duration"),
    CARGO("mission_control", "cargo"),
    RECEIVER("mission_control", "receiver"),
    RECEIVER_POSITION("mission_control", "receiver_position"),
    NOT_LINKED("mission_control", "not_linked"),
    NAME("mission_control", "name"),

    BUTTON_RELINK("mission_control", "button.relink"),
    BUTTON_UNLINK("mission_control", "button.unlink"),
    BUTTON_DECOMMISSION("mission_control", "button.decommission"),
    RELINK_TOOLTIP("mission_control", "relink_tooltip"),
    DECOMMISSION_CONFIRM("mission_control", "decommission_confirm"),

    RESULT_RENAMED("mission_control", "result.renamed"),
    RESULT_RELINKED("mission_control", "result.relinked"),
    RESULT_UNLINKED("mission_control", "result.unlinked"),
    RESULT_DECOMMISSIONED("mission_control", "result.decommissioned"),
    RESULT_NOT_YOURS("mission_control", "result.not_yours"),
    RESULT_NO_CARD("mission_control", "result.no_card");

    private final String key;

    MissionLang(String type, String path) {
        this.key = Util.makeDescriptionId(type, CryoSat.rl(path));
    }

    @NotNull
    @Override
    public String getTranslationKey() {
        return key;
    }

    /** Name of a destination, {@code destination.<ns>.<path>}; datapack destinations without a translation show their id as a title. */
    public static Component destinationName(ResourceLocation id) {
        return Component.translatableWithFallback(Util.makeDescriptionId("destination", id), titleCase(id.getPath()));
    }

    /** Name of an asteroid class, {@code asteroid.<ns>.<path>}, with the same fallback. */
    public static Component asteroidName(ResourceLocation id) {
        return Component.translatableWithFallback(Util.makeDescriptionId("asteroid", id), titleCase(id.getPath()));
    }

    public static Component sizeName(AsteroidSize size) {
        return Component.translatableWithFallback(sizeKey(size), titleCase(size.getSerializedName()));
    }

    public static String sizeKey(AsteroidSize size) {
        return Util.makeDescriptionId("asteroid_size", CryoSat.rl(size.getSerializedName()));
    }

    private static String titleCase(String path) {
        return Arrays.stream(path.split("[_/]")).filter(word -> !word.isEmpty())
              .map(word -> Character.toUpperCase(word.charAt(0)) + word.substring(1))
              .collect(Collectors.joining(" "));
    }
}
