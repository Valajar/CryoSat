package io.github.valajar.cryosat.registries;

// Cargo Receiver (space workstream A)
import io.github.valajar.cryosat.space.receiver.TileEntityCargoReceiver;

import io.github.valajar.cryosat.tile.liquefaction.TileEntityCryoCasing;

import io.github.valajar.cryosat.CryoSat;
import io.github.valajar.cryosat.machine.compressor.AtmosphericCompressorBlockEntity;
import mekanism.common.inventory.container.tile.MekanismTileContainer;
import mekanism.common.registration.impl.ContainerTypeDeferredRegister;
import mekanism.common.registration.impl.ContainerTypeRegistryObject;
import io.github.valajar.cryosat.space.mission.TileEntityMissionControl;

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
    // Mission Control (workstream D): a wide, tall screen, so the player inventory moves right and down
    /** Offset of the player inventory in the Mission Control screen, which is 230 by 243 instead of 176 by 166. */
    public static final int MISSION_CONTROL_INVENTORY_X = 27;
    public static final int MISSION_CONTROL_INVENTORY_Y = 77;
    public static final ContainerTypeRegistryObject<MekanismTileContainer<TileEntityMissionControl>> MISSION_CONTROL =
          MENUS.custom(CryoBlocks.MISSION_CONTROL, TileEntityMissionControl.class).offset(MISSION_CONTROL_INVENTORY_X, MISSION_CONTROL_INVENTORY_Y).build();

    private CryoMenus() {
    }
}
