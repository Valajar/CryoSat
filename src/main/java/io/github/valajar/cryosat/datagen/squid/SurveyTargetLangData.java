package io.github.valajar.cryosat.datagen.squid;

import io.github.valajar.cryosat.CryoSat;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.Util;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.common.data.LanguageProvider;

/**
 * English names of the survey targets CryoSat ships in {@code data/cryosat/cryosat/survey_target/}, under
 * {@code survey_target.cryosat.<id>}. Datapack targets without a name fall back to their title cased id.
 */
public final class SurveyTargetLangData {
    /** File id path to English name, one per shipped survey target file. */
    public static final Map<String, String> NAMES;

    static {
        Map<String, String> names = new LinkedHashMap<>();
        names.put("village", "Village");
        names.put("pillager_outpost", "Pillager Outpost");
        names.put("woodland_mansion", "Woodland Mansion");
        names.put("ocean_monument", "Ocean Monument");
        names.put("desert_pyramid", "Desert Pyramid");
        names.put("jungle_pyramid", "Jungle Temple");
        names.put("swamp_hut", "Swamp Hut");
        names.put("igloo", "Igloo");
        names.put("ruined_portal", "Ruined Portal");
        names.put("shipwreck", "Shipwreck");
        names.put("ocean_ruins", "Ocean Ruins");
        names.put("trail_ruins", "Trail Ruins");
        names.put("ancient_city", "Ancient City");
        names.put("stronghold", "Stronghold");
        names.put("trial_chambers", "Trial Chambers");
        names.put("mineshaft", "Mineshaft");
        NAMES = Map.copyOf(names);
    }

    public static void addTo(LanguageProvider provider) {
        NAMES.forEach((path, name) -> provider.add(key(CryoSat.rl(path)), name));
    }

    private static String key(ResourceLocation id) {
        return Util.makeDescriptionId("survey_target", id);
    }

    private SurveyTargetLangData() {
    }
}
