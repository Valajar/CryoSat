package io.github.valajar.cryosat.registries;

// Fusion Field Coils (superconductor workstream C)
import io.github.valajar.cryosat.superconductor.fusion.TileEntityFieldCoilController;


// Cargo Receiver (space workstream A)
import io.github.valajar.cryosat.space.receiver.TileEntityCargoReceiver;

import io.github.valajar.cryosat.tile.liquefaction.TileEntityCryoCasing;

import io.github.valajar.cryosat.CryoSat;
import io.github.valajar.cryosat.space.launch.TileEntityLaunchController;
import io.github.valajar.cryosat.machine.compressor.AtmosphericCompressorBlockEntity;
import mekanism.common.inventory.container.tile.MekanismTileContainer;
import mekanism.common.registration.impl.ContainerTypeDeferredRegister;
import mekanism.common.registration.impl.ContainerTypeRegistryObject;
import io.github.valajar.cryosat.space.mission.TileEntityMissionControl;
// Superconducting Separator (superconductor workstream A)
import io.github.valajar.cryosat.superconductor.separator.TileEntitySuperconductingSeparator;

/**
 * Menu types, registered through Mekanism's {@link ContainerTypeDeferredRegister}. A {@link MekanismTileContainer} holds
 * the tile's slots and the player inventory and syncs everything the tile tracks in {@code addContainerTrackers}.
 */
public final class CryoMenus {
    public static final ContainerTypeDeferredRegister MENUS = new ContainerTypeDeferredRegister(CryoSat.MODID);

    public static final ContainerTypeRegistryObject<MekanismTileContainer<AtmosphericCompressorBlockEntity>> ATMOSPHERIC_COMPRESSOR =
          MENUS.register(CryoBlocks.ATMOSPHERIC_COMPRESSOR, AtmosphericCompressorBlockEntity.class);

    /** Offset of the player inventory in the Liquefaction Plant screen, which is 34 wider and 22 taller than a standard one. */
    public static final int PLANT_INVENTORY_X = 17;
    public static final int PLANT_INVENTORY_Y = 22;
    /** Shared by every part of the Liquefaction Plant, so the GUI opens from any of them. */
    public static final ContainerTypeRegistryObject<MekanismTileContainer<TileEntityCryoCasing>> LIQUEFACTION_PLANT =
          MENUS.custom("liquefaction_plant", TileEntityCryoCasing.class)
                .offset(PLANT_INVENTORY_X, PLANT_INVENTORY_Y)
                .build();

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
    // Launch Pad (workstream B): Rocket Assembly, opened from the controller (fuel ports forward to it)
    public static final ContainerTypeRegistryObject<MekanismTileContainer<TileEntityLaunchController>> LAUNCH_CONTROLLER =
          MENUS.custom(CryoBlocks.LAUNCH_CONTROLLER, TileEntityLaunchController.class).offset(0, 50).build();
    // Superconducting Separator (superconductor workstream A): a standard sized Mekanism screen
    public static final ContainerTypeRegistryObject<MekanismTileContainer<TileEntitySuperconductingSeparator>> SUPERCONDUCTING_SEPARATOR =
          MENUS.register(CryoBlocks.SUPERCONDUCTING_SEPARATOR, TileEntitySuperconductingSeparator.class);

    // Fusion Field Coils (superconductor workstream C): taller than a standard GUI for the ring status screen
    /** Extra height of the Field Coil Controller screen over a standard one; the player inventory moves down by it. */
    public static final int FIELD_COIL_EXTRA_HEIGHT = 36;
    public static final ContainerTypeRegistryObject<MekanismTileContainer<TileEntityFieldCoilController>> FIELD_COIL_CONTROLLER =
          MENUS.custom(CryoBlocks.FIELD_COIL_CONTROLLER, TileEntityFieldCoilController.class).offset(0, FIELD_COIL_EXTRA_HEIGHT).build();

    // SQUID Array (satellite workstream C): a standard sized Mekanism screen
    public static final ContainerTypeRegistryObject<MekanismTileContainer<io.github.valajar.cryosat.superconductor.squid.TileEntitySquidArray>> SQUID_ARRAY =
          MENUS.register(CryoBlocks.SQUID_ARRAY, io.github.valajar.cryosat.superconductor.squid.TileEntitySquidArray.class);

    // Imaging Terminal (satellite workstream A): a wide, tall screen, so the player inventory moves right and down
    /** Offset of the player inventory in the Imaging Terminal screen, which is 248 by 243 instead of 176 by 166. */
    public static final int IMAGING_TERMINAL_INVENTORY_X = 36;
    public static final int IMAGING_TERMINAL_INVENTORY_Y = 77;
    public static final ContainerTypeRegistryObject<MekanismTileContainer<io.github.valajar.cryosat.space.survey.TileEntityImagingTerminal>> IMAGING_TERMINAL =
          MENUS.custom(CryoBlocks.IMAGING_TERMINAL, io.github.valajar.cryosat.space.survey.TileEntityImagingTerminal.class)
                .offset(IMAGING_TERMINAL_INVENTORY_X, IMAGING_TERMINAL_INVENTORY_Y).build();

    private CryoMenus() {
    }
}
