package io.github.valajar.cryosat.registries;

import io.github.valajar.cryosat.tile.liquefaction.TileEntityCryoCasing;

import io.github.valajar.cryosat.CryoSat;
import io.github.valajar.cryosat.space.launch.TileEntityLaunchController;
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

    // Launch Pad (workstream B): Rocket Assembly, opened from the controller (fuel ports forward to it)
    public static final ContainerTypeRegistryObject<MekanismTileContainer<TileEntityLaunchController>> LAUNCH_CONTROLLER =
          MENUS.custom(CryoBlocks.LAUNCH_CONTROLLER, TileEntityLaunchController.class).offset(0, 50).build();

    private CryoMenus() {
    }
}
