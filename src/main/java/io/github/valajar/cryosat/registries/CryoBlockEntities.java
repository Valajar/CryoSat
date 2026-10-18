package io.github.valajar.cryosat.registries;

// Cargo Receiver (space workstream A)
import io.github.valajar.cryosat.space.receiver.TileEntityCargoReceiver;

import io.github.valajar.cryosat.tile.liquefaction.TileEntityCryoCasing;
import io.github.valajar.cryosat.tile.liquefaction.TileEntityCryoValve;
import io.github.valajar.cryosat.tile.liquefaction.TileEntityCryocooler;
import io.github.valajar.cryosat.tile.liquefaction.TileEntityHeatRejectionPort;
import io.github.valajar.cryosat.tile.liquefaction.TileEntityLiquefactionController;

import io.github.valajar.cryosat.CryoSat;
import io.github.valajar.cryosat.space.launch.TileEntityLaunchController;
import io.github.valajar.cryosat.space.launch.TileEntityLaunchPadFuelPort;
import io.github.valajar.cryosat.space.launch.TileEntityLaunchPadPlating;
import io.github.valajar.cryosat.machine.compressor.AtmosphericCompressorBlockEntity;
import mekanism.common.capabilities.Capabilities;
import mekanism.common.registration.impl.TileEntityTypeDeferredRegister;
import mekanism.common.registration.impl.TileEntityTypeRegistryObject;
import mekanism.common.tile.base.TileEntityMekanism;
import io.github.valajar.cryosat.space.mission.TileEntityMissionControl;
// Superconducting Separator (superconductor workstream A)
import io.github.valajar.cryosat.superconductor.separator.TileEntitySuperconductingSeparator;
import io.github.valajar.cryosat.superconductor.fusion.TileEntityFieldCoilController;

/**
 * Block entity types, registered through Mekanism's {@link TileEntityTypeDeferredRegister}. {@code mekBuilder} attaches
 * Mekanism's capability providers (chemical, energy in Joules and FE, items, fluids, heat, security), which resolve per
 * side through the tile's side configuration, and registers them in {@code RegisterCapabilitiesEvent} by itself.
 */
public final class CryoBlockEntities {
    public static final TileEntityTypeDeferredRegister BLOCK_ENTITIES = new TileEntityTypeDeferredRegister(CryoSat.MODID);

    public static final TileEntityTypeRegistryObject<AtmosphericCompressorBlockEntity> ATMOSPHERIC_COMPRESSOR = BLOCK_ENTITIES
          .mekBuilder(CryoBlocks.ATMOSPHERIC_COMPRESSOR, AtmosphericCompressorBlockEntity::new)
          .clientTicker(TileEntityMekanism::tickClient)
          .serverTicker(TileEntityMekanism::tickServer)
          .withSimple(Capabilities.CONFIG_CARD)
          .build();

    // Liquefaction Plant
    public static final TileEntityTypeRegistryObject<TileEntityCryoCasing> CRYO_CASING = BLOCK_ENTITIES
          .mekBuilder(CryoBlocks.CRYO_CASING, TileEntityCryoCasing::new)
          .clientTicker(TileEntityMekanism::tickClient)
          .serverTicker(TileEntityMekanism::tickServer)
          .withSimple(Capabilities.CONFIGURABLE)
          .build();
    public static final TileEntityTypeRegistryObject<TileEntityCryoValve> CRYO_VALVE = BLOCK_ENTITIES
          .mekBuilder(CryoBlocks.CRYO_VALVE, TileEntityCryoValve::new)
          .clientTicker(TileEntityMekanism::tickClient)
          .serverTicker(TileEntityMekanism::tickServer)
          .withSimple(Capabilities.CONFIGURABLE)
          .build();
    public static final TileEntityTypeRegistryObject<TileEntityCryocooler> CRYOCOOLER = BLOCK_ENTITIES
          .mekBuilder(CryoBlocks.CRYOCOOLER, TileEntityCryocooler::new)
          .clientTicker(TileEntityMekanism::tickClient)
          .serverTicker(TileEntityMekanism::tickServer)
          .withSimple(Capabilities.CONFIGURABLE)
          .build();
    public static final TileEntityTypeRegistryObject<TileEntityHeatRejectionPort> HEAT_REJECTION_PORT = BLOCK_ENTITIES
          .mekBuilder(CryoBlocks.HEAT_REJECTION_PORT, TileEntityHeatRejectionPort::new)
          .clientTicker(TileEntityMekanism::tickClient)
          .serverTicker(TileEntityMekanism::tickServer)
          .withSimple(Capabilities.CONFIGURABLE)
          .build();
    public static final TileEntityTypeRegistryObject<TileEntityLiquefactionController> LIQUEFACTION_CONTROLLER = BLOCK_ENTITIES
          .mekBuilder(CryoBlocks.LIQUEFACTION_CONTROLLER, TileEntityLiquefactionController::new)
          .clientTicker(TileEntityMekanism::tickClient)
          .serverTicker(TileEntityMekanism::tickServer)
          .withSimple(Capabilities.CONFIGURABLE)
          .build();

    // Cargo Receiver (space workstream A)
    public static final TileEntityTypeRegistryObject<TileEntityCargoReceiver> CARGO_RECEIVER = BLOCK_ENTITIES
          .mekBuilder(CryoBlocks.CARGO_RECEIVER, TileEntityCargoReceiver::new)
          .clientTicker(TileEntityMekanism::tickClient)
          .serverTicker(TileEntityMekanism::tickServer)
          .withSimple(Capabilities.CONFIG_CARD)
          .build();

    // Mission Control (workstream D)
    public static final TileEntityTypeRegistryObject<TileEntityMissionControl> MISSION_CONTROL = BLOCK_ENTITIES
          .mekBuilder(CryoBlocks.MISSION_CONTROL, TileEntityMissionControl::new)
          .clientTicker(TileEntityMekanism::tickClient)
          .serverTicker(TileEntityMekanism::tickServer)
          .build();

    // Launch Pad (workstream B)
    public static final TileEntityTypeRegistryObject<TileEntityLaunchPadPlating> LAUNCH_PAD_PLATING = BLOCK_ENTITIES
          .mekBuilder(CryoBlocks.LAUNCH_PAD_PLATING, TileEntityLaunchPadPlating::new)
          .clientTicker(TileEntityMekanism::tickClient)
          .serverTicker(TileEntityMekanism::tickServer)
          .withSimple(Capabilities.CONFIGURABLE)
          .build();
    public static final TileEntityTypeRegistryObject<TileEntityLaunchPadFuelPort> LAUNCH_PAD_FUEL_PORT = BLOCK_ENTITIES
          .mekBuilder(CryoBlocks.LAUNCH_PAD_FUEL_PORT, TileEntityLaunchPadFuelPort::new)
          .clientTicker(TileEntityMekanism::tickClient)
          .serverTicker(TileEntityMekanism::tickServer)
          .withSimple(Capabilities.CONFIGURABLE)
          .build();
    public static final TileEntityTypeRegistryObject<TileEntityLaunchController> LAUNCH_CONTROLLER = BLOCK_ENTITIES
          .mekBuilder(CryoBlocks.LAUNCH_CONTROLLER, TileEntityLaunchController::new)
          .clientTicker(TileEntityMekanism::tickClient)
          .serverTicker(TileEntityMekanism::tickServer)
          .withSimple(Capabilities.CONFIGURABLE)
          .build();

    // Superconducting Separator (superconductor workstream A)
    public static final TileEntityTypeRegistryObject<TileEntitySuperconductingSeparator> SUPERCONDUCTING_SEPARATOR = BLOCK_ENTITIES
          .mekBuilder(CryoBlocks.SUPERCONDUCTING_SEPARATOR, TileEntitySuperconductingSeparator::new)
          .clientTicker(TileEntityMekanism::tickClient)
          .serverTicker(TileEntityMekanism::tickServer)
          .withSimple(Capabilities.CONFIG_CARD)
          .build();

    // Fusion Field Coils (superconductor workstream C)
    public static final TileEntityTypeRegistryObject<TileEntityFieldCoilController> FIELD_COIL_CONTROLLER = BLOCK_ENTITIES
          .mekBuilder(CryoBlocks.FIELD_COIL_CONTROLLER, TileEntityFieldCoilController::new)
          .clientTicker(TileEntityMekanism::tickClient)
          .serverTicker(TileEntityMekanism::tickServer)
          .withSimple(Capabilities.CONFIG_CARD)
          .build();

    private CryoBlockEntities() {
    }
}
