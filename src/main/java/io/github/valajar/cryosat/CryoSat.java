package io.github.valajar.cryosat;

import com.mojang.logging.LogUtils;
import io.github.valajar.cryosat.atmosphere.AtmosphereManager;
import io.github.valajar.cryosat.capability.MekCapabilities;
import io.github.valajar.cryosat.machine.MachineBlockEntity;
import io.github.valajar.cryosat.multiblock.CryoMultiblocks;
import io.github.valajar.cryosat.registries.plant.PlantBlocks;
import io.github.valajar.cryosat.registries.plant.PlantContainerTypes;
import io.github.valajar.cryosat.registries.plant.PlantTileEntityTypes;
import io.github.valajar.cryosat.registries.CryoBlockEntities;
import io.github.valajar.cryosat.registries.CryoBlocks;
import io.github.valajar.cryosat.registries.CryoChemicals;
import io.github.valajar.cryosat.registries.CryoCreativeTabs;
import io.github.valajar.cryosat.registries.CryoItems;
import io.github.valajar.cryosat.registries.CryoMenus;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import org.slf4j.Logger;

@Mod(CryoSat.MODID)
public class CryoSat {
    public static final String MODID = "cryosat";
    public static final Logger LOGGER = LogUtils.getLogger();

    public CryoSat(IEventBus modEventBus, ModContainer modContainer) {
        CryoChemicals.CHEMICALS.register(modEventBus);
        CryoBlocks.BLOCKS.register(modEventBus);
        CryoItems.ITEMS.register(modEventBus);
        CryoBlockEntities.BLOCK_ENTITIES.register(modEventBus);
        CryoMenus.MENUS.register(modEventBus);
        CryoCreativeTabs.TABS.register(modEventBus);
        // Liquefaction Plant, registered through Mekanism's helpers
        PlantBlocks.BLOCKS.register(modEventBus);
        PlantTileEntityTypes.TILE_ENTITY_TYPES.register(modEventBus);
        PlantContainerTypes.CONTAINER_TYPES.register(modEventBus);
        CryoMultiblocks.init();
        modEventBus.addListener(CryoSat::registerCapabilities);
        NeoForge.EVENT_BUS.addListener(CryoSat::addReloadListeners);
    }

    private static void registerCapabilities(RegisterCapabilitiesEvent event) {
        registerMachine(event, CryoBlockEntities.ATMOSPHERIC_COMPRESSOR.get());
    }

    private static void registerMachine(RegisterCapabilitiesEvent event, BlockEntityType<? extends MachineBlockEntity> type) {
        event.registerBlockEntity(MekCapabilities.CHEMICAL, type, (machine, side) -> machine.getChemicalHandler(side));
        event.registerBlockEntity(MekCapabilities.STRICT_ENERGY, type, (machine, side) -> machine.getEnergyHandler(side));
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, type, (machine, side) -> machine.getEnergyHandler(side));
    }

    private static void addReloadListeners(AddReloadListenerEvent event) {
        event.addListener(AtmosphereManager.INSTANCE);
    }

    public static ResourceLocation rl(String path) {
        return ResourceLocation.fromNamespaceAndPath(MODID, path);
    }
}
