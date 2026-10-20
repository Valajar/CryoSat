package io.github.valajar.cryosat.registries;

// Uplink Card (space workstream A)
import io.github.valajar.cryosat.space.receiver.ItemUplinkCard;

// Survey Locator (satellite workstream B)
import io.github.valajar.cryosat.space.locator.ItemSurveyLocator;

import io.github.valajar.cryosat.CryoSat;
import io.github.valajar.cryosat.space.PayloadType;
import io.github.valajar.cryosat.space.RocketPartItem;
import io.github.valajar.cryosat.space.RocketPartItem.Kind;
import io.github.valajar.cryosat.space.RocketTier;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import mekanism.common.registration.impl.ItemDeferredRegister;
import mekanism.common.registration.impl.ItemRegistryObject;
import net.minecraft.world.item.Item;

/**
 * Standalone items, registered through Mekanism's {@link ItemDeferredRegister}. Block items are registered together
 * with their blocks in {@link CryoBlocks}.
 */
public final class CryoItems {
    public static final ItemDeferredRegister ITEMS = new ItemDeferredRegister(CryoSat.MODID);

    // Rocket parts, tiered ones named <tier>_<part> like Mekanism's tiered items
    public static final Map<RocketTier, ItemRegistryObject<RocketPartItem>> ROCKET_ENGINES = tiered("rocket_engine", Kind.ENGINE, null);
    public static final Map<RocketTier, ItemRegistryObject<RocketPartItem>> FUEL_TANKS = tiered("fuel_tank", Kind.FUEL_TANK, null);
    public static final ItemRegistryObject<RocketPartItem> GUIDANCE_COMPUTER = ITEMS.registerItem("guidance_computer",
          properties -> new RocketPartItem(properties, Kind.GUIDANCE_COMPUTER, null, null));
    public static final Map<RocketTier, ItemRegistryObject<RocketPartItem>> ASTEROID_MINERS = tiered("asteroid_miner", Kind.PAYLOAD, PayloadType.ASTEROID_MINER);
    public static final Map<RocketTier, ItemRegistryObject<RocketPartItem>> SURVEY_SATELLITES = tiered("survey_satellite", Kind.PAYLOAD, PayloadType.SURVEY_SATELLITE);

    // Basic tier propellant: sulfur, carbon and a slime binder, pressed into grains
    public static final ItemRegistryObject<Item> PROPELLANT_MIX = ITEMS.register("propellant_mix");
    public static final ItemRegistryObject<Item> SOLID_PROPELLANT_GRAIN = ITEMS.register("solid_propellant_grain");

    // Uplink Card (space workstream A): stores the Cargo Receiver a mission delivers to
    public static final ItemRegistryObject<ItemUplinkCard> UPLINK_CARD = ITEMS.registerItem("uplink_card", ItemUplinkCard::new);

    // Superconductors: the crafted coil every superconducting block is built from (high temperature grade, liquid nitrogen)
    public static final ItemRegistryObject<Item> SUPERCONDUCTING_COIL = ITEMS.register("superconducting_coil");

    // Survey Locator (satellite workstream B): a tech compass holding survey results from an Imaging Terminal
    public static final ItemRegistryObject<ItemSurveyLocator> SURVEY_LOCATOR = ITEMS.registerItem("survey_locator", ItemSurveyLocator::new);

    /** @return the payload item of the given type and tier */
    public static RocketPartItem payload(PayloadType type, RocketTier tier) {
        Map<RocketTier, ItemRegistryObject<RocketPartItem>> items = switch (type) {
            case ASTEROID_MINER -> ASTEROID_MINERS;
            case SURVEY_SATELLITE -> SURVEY_SATELLITES;
        };
        return items.get(tier).get();
    }

    private static Map<RocketTier, ItemRegistryObject<RocketPartItem>> tiered(String name, Kind kind, PayloadType payload) {
        Map<RocketTier, ItemRegistryObject<RocketPartItem>> items = new EnumMap<>(RocketTier.class);
        for (RocketTier tier : RocketTier.values()) {
            items.put(tier, ITEMS.registerItem(tier.getSerializedName() + "_" + name, properties -> new RocketPartItem(properties, kind, tier, payload)));
        }
        return Collections.unmodifiableMap(items);
    }

    private CryoItems() {
    }
}
