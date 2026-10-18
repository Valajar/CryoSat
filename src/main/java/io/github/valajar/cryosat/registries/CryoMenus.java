package io.github.valajar.cryosat.registries;

// Cargo Receiver (space workstream A)
import io.github.valajar.cryosat.space.receiver.TileEntityCargoReceiver;

import io.github.valajar.cryosat.tile.liquefaction.TileEntityCryoCasing;

import io.github.valajar.cryosat.CryoSat;
import io.github.valajar.cryosat.machine.compressor.AtmosphericCompressorBlockEntity;
import mekanism.common.inventory.container.tile.MekanismTileContainer;
import mekanism.common.registration.impl.ContainerTypeDeferredRegister;
import mekanism.common.registration.impl.ContainerTypeRegistryObject;

/**
 * Menu types, registered through Mekanism's {@link ContainerTypeDeferredRegister}. A {@link MekanismTileContainer} holds
 * the tile's slots and the player inventory and syncs everything the tile tracks in {@code addContainerTrackers}.
 */
public final class CryoMenus {
    public static final ContainerTypeDeferredRegister MENUS = new ContainerTypeDeferredRegister(CryoSat.MODID);

    public static final ContainerTypeRegistryObject<MekanismTileContainer<AtmosphericCompressorBlockEntity>> ATMOSPHERIC_COMPRESSOR =
          MENUS.register(CryoBlocks.ATMOSPHERIC_COMPRESSOR, AtmosphericCompressorBlockEntity.class);

    /** Shared by every part of the Liquefaction Plant, so the GUI opens from any of them. */
    public static final ContainerTypeRegistryObject<MekanismTileContainer<TileEntityCryoCasing>> LIQUEFACTION_PLANT =
          MENUS.custom("liquefaction_plant", TileEntityCryoCasing.class).build();

    // Cargo Receiver (space workstream A): taller than a standard GUI to fit the mission screen under the cargo grid
    public static final ContainerTypeRegistryObject<MekanismTileContainer<TileEntityCargoReceiver>> CARGO_RECEIVER =
          MENUS.custom(CryoBlocks.CARGO_RECEIVER, TileEntityCargoReceiver.class)
                .offset(0, TileEntityCargoReceiver.GUI_EXTRA_HEIGHT)
                .build();

    private CryoMenus() {
    }
}
