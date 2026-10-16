package io.github.valajar.cryosat.registries.plant;

import io.github.valajar.cryosat.CryoSat;
import io.github.valajar.cryosat.tile.liquefaction.TileEntityCryoCasing;
import io.github.valajar.cryosat.tile.liquefaction.TileEntityCryoValve;
import io.github.valajar.cryosat.tile.liquefaction.TileEntityCryocooler;
import io.github.valajar.cryosat.tile.liquefaction.TileEntityHeatRejectionPort;
import io.github.valajar.cryosat.tile.liquefaction.TileEntityLiquefactionController;
import mekanism.common.capabilities.Capabilities;
import mekanism.common.registration.impl.TileEntityTypeDeferredRegister;
import mekanism.common.registration.impl.TileEntityTypeRegistryObject;
import mekanism.common.tile.base.TileEntityMekanism;

public final class PlantTileEntityTypes {
    public static final TileEntityTypeDeferredRegister TILE_ENTITY_TYPES = new TileEntityTypeDeferredRegister(CryoSat.MODID);

    public static final TileEntityTypeRegistryObject<TileEntityCryoCasing> CRYO_CASING = TILE_ENTITY_TYPES
          .mekBuilder(PlantBlocks.CRYO_CASING, TileEntityCryoCasing::new)
          .clientTicker(TileEntityMekanism::tickClient)
          .serverTicker(TileEntityMekanism::tickServer)
          .withSimple(Capabilities.CONFIGURABLE)
          .build();
    public static final TileEntityTypeRegistryObject<TileEntityCryoValve> CRYO_VALVE = TILE_ENTITY_TYPES
          .mekBuilder(PlantBlocks.CRYO_VALVE, TileEntityCryoValve::new)
          .clientTicker(TileEntityMekanism::tickClient)
          .serverTicker(TileEntityMekanism::tickServer)
          .withSimple(Capabilities.CONFIGURABLE)
          .build();
    public static final TileEntityTypeRegistryObject<TileEntityCryocooler> CRYOCOOLER = TILE_ENTITY_TYPES
          .mekBuilder(PlantBlocks.CRYOCOOLER, TileEntityCryocooler::new)
          .clientTicker(TileEntityMekanism::tickClient)
          .serverTicker(TileEntityMekanism::tickServer)
          .withSimple(Capabilities.CONFIGURABLE)
          .build();
    public static final TileEntityTypeRegistryObject<TileEntityHeatRejectionPort> HEAT_REJECTION_PORT = TILE_ENTITY_TYPES
          .mekBuilder(PlantBlocks.HEAT_REJECTION_PORT, TileEntityHeatRejectionPort::new)
          .clientTicker(TileEntityMekanism::tickClient)
          .serverTicker(TileEntityMekanism::tickServer)
          .withSimple(Capabilities.CONFIGURABLE)
          .build();
    public static final TileEntityTypeRegistryObject<TileEntityLiquefactionController> LIQUEFACTION_CONTROLLER = TILE_ENTITY_TYPES
          .mekBuilder(PlantBlocks.LIQUEFACTION_CONTROLLER, TileEntityLiquefactionController::new)
          .clientTicker(TileEntityMekanism::tickClient)
          .serverTicker(TileEntityMekanism::tickServer)
          .withSimple(Capabilities.CONFIGURABLE)
          .build();

    private PlantTileEntityTypes() {
    }
}
