package io.github.valajar.cryosat.capability;

import mekanism.api.MekanismAPI;
import mekanism.api.chemical.IChemicalHandler;
import mekanism.api.energy.IStrictEnergyHandler;
import mekanism.api.heat.IHeatHandler;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.capabilities.BlockCapability;

/**
 * Mekanism's block capabilities, obtained without touching Mekanism's internal classes.
 * <p>
 * Mekanism declares these capabilities outside its API package. NeoForge identifies a capability by its name and handler
 * type, so creating one with the same name and type returns the very same instance Mekanism uses.
 */
public final class MekCapabilities {
    public static final BlockCapability<IChemicalHandler, Direction> CHEMICAL = BlockCapability.createSided(mek("chemical_handler"), IChemicalHandler.class);
    public static final BlockCapability<IHeatHandler, Direction> HEAT = BlockCapability.createSided(mek("heat_handler"), IHeatHandler.class);
    public static final BlockCapability<IStrictEnergyHandler, Direction> STRICT_ENERGY = BlockCapability.createSided(mek("strict_energy_handler"), IStrictEnergyHandler.class);

    private static ResourceLocation mek(String path) {
        return ResourceLocation.fromNamespaceAndPath(MekanismAPI.MEKANISM_MODID, path);
    }

    private MekCapabilities() {
    }
}
