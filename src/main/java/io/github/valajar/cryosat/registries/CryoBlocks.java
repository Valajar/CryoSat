package io.github.valajar.cryosat.registries;

// Cargo Receiver (space workstream A)
import io.github.valajar.cryosat.space.receiver.TileEntityCargoReceiver;
import mekanism.common.content.blocktype.BlockTypeTile;

import io.github.valajar.cryosat.tile.liquefaction.TileEntityCryoCasing;
import io.github.valajar.cryosat.tile.liquefaction.TileEntityCryoValve;
import io.github.valajar.cryosat.tile.liquefaction.TileEntityCryocooler;
import io.github.valajar.cryosat.tile.liquefaction.TileEntityHeatRejectionPort;
import io.github.valajar.cryosat.tile.liquefaction.TileEntityLiquefactionController;
import mekanism.common.block.prefab.BlockBasicMultiblock;
import net.minecraft.world.level.material.MapColor;

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
import io.github.valajar.cryosat.space.mission.TileEntityMissionControl;
import io.github.valajar.cryosat.space.mission.MissionControl;

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

    // Liquefaction Plant
    public static final BlockRegistryObject<BlockBasicMultiblock<TileEntityCryoCasing>, ItemBlockTooltip<BlockBasicMultiblock<TileEntityCryoCasing>>> CRYO_CASING =
          BLOCKS.registerDetails("cryo_casing", () -> new BlockBasicMultiblock<>(CryoBlockTypes.CRYO_CASING, p -> p.mapColor(MapColor.METAL)));
    public static final BlockRegistryObject<BlockBasicMultiblock<TileEntityCryoValve>, ItemBlockTooltip<BlockBasicMultiblock<TileEntityCryoValve>>> CRYO_VALVE =
          BLOCKS.registerDetails("cryo_valve", () -> new BlockBasicMultiblock<>(CryoBlockTypes.CRYO_VALVE, p -> p.mapColor(MapColor.METAL)));
    public static final BlockRegistryObject<BlockBasicMultiblock<TileEntityCryocooler>, ItemBlockTooltip<BlockBasicMultiblock<TileEntityCryocooler>>> CRYOCOOLER =
          BLOCKS.registerDetails("cryocooler", () -> new BlockBasicMultiblock<>(CryoBlockTypes.CRYOCOOLER, p -> p.mapColor(MapColor.METAL)));
    public static final BlockRegistryObject<BlockBasicMultiblock<TileEntityHeatRejectionPort>, ItemBlockTooltip<BlockBasicMultiblock<TileEntityHeatRejectionPort>>> HEAT_REJECTION_PORT =
          BLOCKS.registerDetails("heat_rejection_port", () -> new BlockBasicMultiblock<>(CryoBlockTypes.HEAT_REJECTION_PORT, p -> p.mapColor(MapColor.METAL)));
    public static final BlockRegistryObject<BlockBasicMultiblock<TileEntityLiquefactionController>, ItemBlockTooltip<BlockBasicMultiblock<TileEntityLiquefactionController>>> LIQUEFACTION_CONTROLLER =
          BLOCKS.registerDetails("liquefaction_controller", () -> new BlockBasicMultiblock<>(CryoBlockTypes.LIQUEFACTION_CONTROLLER, p -> p.mapColor(MapColor.METAL)));

    // Cargo Receiver (space workstream A): the block item keeps the side config, ejector and the 27 cargo slots
    public static final BlockRegistryObject<BlockTile<TileEntityCargoReceiver, BlockTypeTile<TileEntityCargoReceiver>>, ItemBlockTooltip<BlockTile<TileEntityCargoReceiver, BlockTypeTile<TileEntityCargoReceiver>>>> CARGO_RECEIVER =
          BLOCKS.register("cargo_receiver", () -> new BlockTile<>(CryoBlockTypes.CARGO_RECEIVER, properties -> properties.mapColor(BlockResourceInfo.STEEL.getMapColor())),
                (block, properties) -> new ItemBlockTooltip<>(block, true, properties
                      .component(MekanismDataComponents.EJECTOR, AttachedEjector.DEFAULT)
                      .component(MekanismDataComponents.SIDE_CONFIG, TileEntityCargoReceiver.DEFAULT_SIDE_CONFIG)
                )
          ).forItemHolder(holder -> holder
                .addAttachmentOnlyContainers(ContainerType.ITEM, () -> ItemSlotsBuilder.builder()
                      .addOutput(TileEntityCargoReceiver.SLOTS)
                      .build()
                )
          );

    // Mission Control (workstream D): the block item keeps the Uplink Card in the slot, like Mekanism machines keep their slots
    public static final BlockRegistryObject<BlockTile<TileEntityMissionControl, BlockTypeTile<TileEntityMissionControl>>, ItemBlockTooltip<BlockTile<TileEntityMissionControl, BlockTypeTile<TileEntityMissionControl>>>> MISSION_CONTROL =
          BLOCKS.register("mission_control", () -> new BlockTile<>(CryoBlockTypes.MISSION_CONTROL, properties -> properties.mapColor(BlockResourceInfo.STEEL.getMapColor())),
                (block, properties) -> new ItemBlockTooltip<>(block, true, properties)
          ).forItemHolder(holder -> holder
                .addAttachmentOnlyContainers(ContainerType.ITEM, () -> ItemSlotsBuilder.builder()
                      .addInput(MissionControl::isUplinkCard)
                      .build()
                )
          );

    private CryoBlocks() {
    }
}
