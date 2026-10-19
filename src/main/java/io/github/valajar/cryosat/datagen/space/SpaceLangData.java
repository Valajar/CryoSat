package io.github.valajar.cryosat.datagen.space;

import io.github.valajar.cryosat.CryoSat;
import io.github.valajar.cryosat.registries.CryoItems;
import io.github.valajar.cryosat.space.AsteroidClass;
import io.github.valajar.cryosat.space.AsteroidSize;
import net.minecraft.Util;
import io.github.valajar.cryosat.space.RocketTier;
import io.github.valajar.cryosat.space.SpaceLang;
import java.util.function.BiConsumer;
import net.neoforged.neoforge.common.data.LanguageProvider;

/**
 * English text shared by the space features, added by the main language provider.
 */
public final class SpaceLangData {
    public static void addTo(LanguageProvider provider) {
        for (RocketTier tier : RocketTier.values()) {
            String name = tier.baseTier().getSimpleName();
            provider.addItem(CryoItems.ROCKET_ENGINES.get(tier), name + " Rocket Engine");
            provider.addItem(CryoItems.FUEL_TANKS.get(tier), name + " Fuel Tank");
            provider.addItem(CryoItems.ASTEROID_MINERS.get(tier), name + " Asteroid Miner");
            provider.addItem(CryoItems.SURVEY_SATELLITES.get(tier), name + " Survey Satellite");
        }
        provider.addItem(CryoItems.GUIDANCE_COMPUTER, "Guidance Computer");
        provider.addItem(CryoItems.PROPELLANT_MIX, "Propellant Mix");
        provider.addItem(CryoItems.SOLID_PROPELLANT_GRAIN, "Solid Propellant Grain");

        BiConsumer<SpaceLang, String> add = (lang, text) -> provider.add(lang.getTranslationKey(), text);
        add.accept(SpaceLang.PART_TOOLTIP, "Rocket part: %1$s");
        add.accept(SpaceLang.PART_ENGINE, "Engine");
        add.accept(SpaceLang.PART_FUEL_TANK, "Fuel Tank");
        add.accept(SpaceLang.PART_GUIDANCE_COMPUTER, "Guidance Computer");
        add.accept(SpaceLang.PART_PAYLOAD, "Payload");

        add.accept(SpaceLang.PROBLEM_MISSING_ENGINE, "Needs an engine");
        add.accept(SpaceLang.PROBLEM_MISSING_FUEL_TANK, "Needs a fuel tank");
        add.accept(SpaceLang.PROBLEM_MISSING_GUIDANCE, "Needs a guidance computer");
        add.accept(SpaceLang.PROBLEM_MISSING_PAYLOAD, "Needs a payload");
        add.accept(SpaceLang.PROBLEM_PAYLOAD_TOO_ADVANCED, "Payload is a higher tier than the rocket");
        add.accept(SpaceLang.PROBLEM_TIER_CANNOT_LAUNCH, "This rocket tier cannot launch yet");
        add.accept(SpaceLang.PROBLEM_UNKNOWN_DESTINATION, "Unknown destination");
        add.accept(SpaceLang.PROBLEM_DESTINATION_OUT_OF_REACH, "Destination needs a higher tier rocket");
        add.accept(SpaceLang.PROBLEM_PAYLOAD_NOT_USABLE, "This payload has nothing to do there");
        add.accept(SpaceLang.PROBLEM_NOTHING_TO_MINE, "No asteroids this rocket can reach");
        add.accept(SpaceLang.PROBLEM_WRONG_DIMENSION, "Cannot launch there from this dimension");

        add.accept(SpaceLang.STATE_TRAVELLING, "Travelling");
        add.accept(SpaceLang.STATE_MINING, "Mining");
        add.accept(SpaceLang.STATE_RETIRED, "Retired");
        add.accept(SpaceLang.STATE_ORBITING, "Orbiting");

        add.accept(SpaceLang.DEFAULT_MINER_NAME, "%1$s Miner #%2$s");
        add.accept(SpaceLang.DEFAULT_SATELLITE_NAME, "%1$s Satellite #%2$s");
        add.accept(SpaceLang.LAUNCH_ANNOUNCEMENT, "%1$s launched %2$s");

        // Names of the default space data; datapack additions fall back to a title cased id
        provider.add(Util.makeDescriptionId("destination", AsteroidClass.ASTEROID_BELT), "Asteroid Belt");
        provider.add(Util.makeDescriptionId("destination", CryoSat.rl("earth_orbit")), "Earth Orbit");
        provider.add(Util.makeDescriptionId("asteroid", CryoSat.rl("chondrite")), "Chondrite");
        provider.add(SpaceLang.sizeKey(AsteroidSize.SMALL), "Small");
        provider.add(SpaceLang.sizeKey(AsteroidSize.MEDIUM), "Medium");
        provider.add(SpaceLang.sizeKey(AsteroidSize.LARGE), "Large");
        provider.add(SpaceLang.sizeKey(AsteroidSize.HUGE), "Huge");
    }

    private SpaceLangData() {
    }
}
