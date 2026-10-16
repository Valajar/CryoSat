package io.github.valajar.cryosat.registries;

import io.github.valajar.cryosat.CryoSat;
import io.github.valajar.cryosat.machine.compressor.AtmosphericCompressorBlockEntity;
import mekanism.common.attachments.component.AttachedEjector;
import mekanism.common.attachments.containers.ContainerType;
import mekanism.common.attachments.containers.chemical.ChemicalTanksBuilder;
import mekanism.common.attachments.containers.item.ItemSlotsBuilder;
import mekanism.common.block.prefab.BlockTile;
import mekanism.common.content.blocktype.Machine;
import mekanism.common.item.block.ItemBlockTooltip;
import mekanism.common.registration.impl.BlockDeferredRegister;
import mekanism.common.registration.impl.BlockRegistryObject;
import mekanism.common.registries.MekanismDataComponents;
import mekanism.common.resource.BlockResourceInfo;

/**
 * Blocks and their items, registered through Mekanism's {@link BlockDeferredRegister} so block items get Mekanism's
 * default components (security, redstone control, upgrades) and attachment containers, which carry a machine's energy,
 * tanks and slots over when it is broken and placed again.
 */
public final class CryoBlocks {
    public static final BlockDeferredRegister BLOCKS = new BlockDeferredRegister(CryoSat.MODID);

    public static final BlockRegistryObject<BlockTile<AtmosphericCompressorBlockEntity, Machine<AtmosphericCompressorBlockEntity>>, ItemBlockTooltip<BlockTile<AtmosphericCompressorBlockEntity, Machine<AtmosphericCompressorBlockEntity>>>> ATMOSPHERIC_COMPRESSOR =
          BLOCKS.register("atmospheric_compressor", () -> new BlockTile<>(CryoBlockTypes.ATMOSPHERIC_COMPRESSOR, properties -> properties.mapColor(BlockResourceInfo.STEEL.getMapColor())),
                (block, properties) -> new ItemBlockTooltip<>(block, true, properties
                      .component(MekanismDataComponents.EJECTOR, AttachedEjector.DEFAULT)
                      .component(MekanismDataComponents.SIDE_CONFIG, AtmosphericCompressorBlockEntity.DEFAULT_SIDE_CONFIG)
                )
          ).forItemHolder(holder -> holder
                .addAttachmentOnlyContainers(ContainerType.CHEMICAL, () -> ChemicalTanksBuilder.builder()
                      .addBasic(AtmosphericCompressorBlockEntity.TANK_CAPACITY)
                      .build()
                ).addAttachmentOnlyContainers(ContainerType.ITEM, () -> ItemSlotsBuilder.builder()
                      .addChemicalDrainSlot(0)
                      .addEnergy()
                      .build()
                )
          );

    private CryoBlocks() {
    }
}
