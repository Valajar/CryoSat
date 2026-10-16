package io.github.valajar.cryosat.registries.plant;

import io.github.valajar.cryosat.tile.liquefaction.TileEntityCryoCasing;
import io.github.valajar.cryosat.tile.liquefaction.TileEntityCryoValve;
import io.github.valajar.cryosat.tile.liquefaction.TileEntityCryocooler;
import io.github.valajar.cryosat.tile.liquefaction.TileEntityHeatRejectionPort;
import io.github.valajar.cryosat.tile.liquefaction.TileEntityLiquefactionController;
import mekanism.common.block.attribute.AttributeStateFacing;
import mekanism.common.block.attribute.Attributes;
import mekanism.common.content.blocktype.BlockTypeTile;
import mekanism.common.content.blocktype.BlockTypeTile.BlockTileBuilder;

/**
 * Mekanism block types (attribute bundles) for the Liquefaction Plant's parts.
 */
public final class PlantBlockTypes {
    public static final long CRYOCOOLER_ENERGY_STORAGE = 40_000;

    public static final BlockTypeTile<TileEntityCryoCasing> CRYO_CASING = BlockTileBuilder
          .createBlock(() -> PlantTileEntityTypes.CRYO_CASING, CryoLang.DESCRIPTION_CRYO_CASING)
          .withGui(() -> PlantContainerTypes.LIQUEFACTION_PLANT, CryoLang.LIQUEFACTION_PLANT)
          .externalMultiblock()
          .build();
    public static final BlockTypeTile<TileEntityCryoValve> CRYO_VALVE = BlockTileBuilder
          .createBlock(() -> PlantTileEntityTypes.CRYO_VALVE, CryoLang.DESCRIPTION_CRYO_VALVE)
          .withGui(() -> PlantContainerTypes.LIQUEFACTION_PLANT, CryoLang.LIQUEFACTION_PLANT)
          .with(Attributes.COMPARATOR)
          .externalMultiblock()
          .build();
    public static final BlockTypeTile<TileEntityCryocooler> CRYOCOOLER = BlockTileBuilder
          .createBlock(() -> PlantTileEntityTypes.CRYOCOOLER, CryoLang.DESCRIPTION_CRYOCOOLER)
          .withGui(() -> PlantContainerTypes.LIQUEFACTION_PLANT, CryoLang.LIQUEFACTION_PLANT)
          .withEnergyConfig(() -> CRYOCOOLER_ENERGY_STORAGE)
          .with(Attributes.ACTIVE)
          .externalMultiblock()
          .build();
    public static final BlockTypeTile<TileEntityHeatRejectionPort> HEAT_REJECTION_PORT = BlockTileBuilder
          .createBlock(() -> PlantTileEntityTypes.HEAT_REJECTION_PORT, CryoLang.DESCRIPTION_HEAT_REJECTION_PORT)
          .withGui(() -> PlantContainerTypes.LIQUEFACTION_PLANT, CryoLang.LIQUEFACTION_PLANT)
          .with(Attributes.ACTIVE)
          .externalMultiblock()
          .build();
    public static final BlockTypeTile<TileEntityLiquefactionController> LIQUEFACTION_CONTROLLER = BlockTileBuilder
          .createBlock(() -> PlantTileEntityTypes.LIQUEFACTION_CONTROLLER, CryoLang.DESCRIPTION_LIQUEFACTION_CONTROLLER)
          .withGui(() -> PlantContainerTypes.LIQUEFACTION_PLANT, CryoLang.LIQUEFACTION_PLANT)
          .with(Attributes.ACTIVE, new AttributeStateFacing())
          .externalMultiblock()
          .build();

    private PlantBlockTypes() {
    }
}
