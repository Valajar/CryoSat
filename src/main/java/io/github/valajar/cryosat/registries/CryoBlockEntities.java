package io.github.valajar.cryosat.registries;

import io.github.valajar.cryosat.CryoSat;
import io.github.valajar.cryosat.machine.compressor.AtmosphericCompressorBlockEntity;
import mekanism.common.capabilities.Capabilities;
import mekanism.common.registration.impl.TileEntityTypeDeferredRegister;
import mekanism.common.registration.impl.TileEntityTypeRegistryObject;
import mekanism.common.tile.base.TileEntityMekanism;

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

    private CryoBlockEntities() {
    }
}
