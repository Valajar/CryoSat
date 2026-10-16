package io.github.valajar.cryosat.registries.plant;

import io.github.valajar.cryosat.CryoSat;
import io.github.valajar.cryosat.tile.liquefaction.TileEntityCryoCasing;
import mekanism.common.inventory.container.tile.MekanismTileContainer;
import mekanism.common.registration.impl.ContainerTypeDeferredRegister;
import mekanism.common.registration.impl.ContainerTypeRegistryObject;

public final class PlantContainerTypes {
    public static final ContainerTypeDeferredRegister CONTAINER_TYPES = new ContainerTypeDeferredRegister(CryoSat.MODID);

    public static final ContainerTypeRegistryObject<MekanismTileContainer<TileEntityCryoCasing>> LIQUEFACTION_PLANT =
          CONTAINER_TYPES.custom("liquefaction_plant", TileEntityCryoCasing.class).offset(0, 0).build();

    private PlantContainerTypes() {
    }
}
