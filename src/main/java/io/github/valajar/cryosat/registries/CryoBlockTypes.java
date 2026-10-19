package io.github.valajar.cryosat.registries;

// Cargo Receiver (space workstream A)
import io.github.valajar.cryosat.space.receiver.TileEntityCargoReceiver;
import io.github.valajar.cryosat.space.receiver.ReceiverLang;

import io.github.valajar.cryosat.tile.liquefaction.TileEntityCryoCasing;
import io.github.valajar.cryosat.tile.liquefaction.TileEntityCryoValve;
import io.github.valajar.cryosat.tile.liquefaction.TileEntityCryocooler;
import io.github.valajar.cryosat.tile.liquefaction.TileEntityHeatRejectionPort;
import io.github.valajar.cryosat.tile.liquefaction.TileEntityLiquefactionController;
import mekanism.common.block.attribute.AttributeStateFacing;
import mekanism.common.block.attribute.Attributes;
import mekanism.common.content.blocktype.BlockTypeTile;
import mekanism.common.content.blocktype.BlockTypeTile.BlockTileBuilder;

import io.github.valajar.cryosat.CryoLang;
import io.github.valajar.cryosat.space.launch.LaunchLang;
import io.github.valajar.cryosat.space.launch.TileEntityLaunchController;
import io.github.valajar.cryosat.space.launch.TileEntityLaunchPadFuelPort;
import io.github.valajar.cryosat.space.launch.TileEntityLaunchPadPlating;
import io.github.valajar.cryosat.machine.compressor.AtmosphericCompressorBlockEntity;
import mekanism.common.content.blocktype.Machine;
import mekanism.common.content.blocktype.Machine.MachineBuilder;
import mekanism.common.lib.transmitter.TransmissionType;
import mekanism.common.registries.MekanismSounds;
import io.github.valajar.cryosat.space.mission.TileEntityMissionControl;
import io.github.valajar.cryosat.space.mission.MissionLang;
// Superconducting Separator (superconductor workstream A)
import io.github.valajar.cryosat.superconductor.separator.TileEntitySuperconductingSeparator;
import io.github.valajar.cryosat.superconductor.separator.SeparatorLang;
import io.github.valajar.cryosat.superconductor.fusion.FusionCoilLang;
import io.github.valajar.cryosat.superconductor.fusion.TileEntityFieldCoilController;
import mekanism.common.content.blocktype.BlockType;
import mekanism.common.content.blocktype.BlockType.BlockTypeBuilder;

/**
 * Mekanism block types: the attributes (GUI, energy, side config, upgrades, sound, facing, active state, security,
 * redstone control, ...) that Mekanism's block, tile, item and GUI classes read to decide what a block supports.
 * {@link MachineBuilder#createMachine} already adds facing, active light, security, redstone control, comparator output
 * and the Speed, Energy and Muffling upgrades.
 */
public final class CryoBlockTypes {
    public static final Machine<AtmosphericCompressorBlockEntity> ATMOSPHERIC_COMPRESSOR = MachineBuilder
          .createMachine(() -> CryoBlockEntities.ATMOSPHERIC_COMPRESSOR, CryoLang.DESCRIPTION_ATMOSPHERIC_COMPRESSOR)
          .withGui(() -> CryoMenus.ATMOSPHERIC_COMPRESSOR)
          .withSound(MekanismSounds.OSMIUM_COMPRESSOR)
          .withEnergyConfig(() -> AtmosphericCompressorBlockEntity.BASE_ENERGY_PER_TICK, () -> AtmosphericCompressorBlockEntity.ENERGY_CAPACITY)
          .withSideConfig(TransmissionType.ITEM, TransmissionType.CHEMICAL, TransmissionType.ENERGY)
          .build();

    // Liquefaction Plant
    public static final BlockTypeTile<TileEntityCryoCasing> CRYO_CASING = BlockTileBuilder
          .createBlock(() -> CryoBlockEntities.CRYO_CASING, CryoLang.DESCRIPTION_CRYO_CASING)
          .withGui(() -> CryoMenus.LIQUEFACTION_PLANT, CryoLang.LIQUEFACTION_PLANT)
          .externalMultiblock()
          .build();
    public static final BlockTypeTile<TileEntityCryoValve> CRYO_VALVE = BlockTileBuilder
          .createBlock(() -> CryoBlockEntities.CRYO_VALVE, CryoLang.DESCRIPTION_CRYO_VALVE)
          .withGui(() -> CryoMenus.LIQUEFACTION_PLANT, CryoLang.LIQUEFACTION_PLANT)
          .with(Attributes.COMPARATOR)
          .externalMultiblock()
          .build();
    public static final BlockTypeTile<TileEntityCryocooler> CRYOCOOLER = BlockTileBuilder
          .createBlock(() -> CryoBlockEntities.CRYOCOOLER, CryoLang.DESCRIPTION_CRYOCOOLER)
          .withGui(() -> CryoMenus.LIQUEFACTION_PLANT, CryoLang.LIQUEFACTION_PLANT)
          .with(Attributes.ACTIVE)
          .externalMultiblock()
          .build();
    public static final BlockTypeTile<TileEntityHeatRejectionPort> HEAT_REJECTION_PORT = BlockTileBuilder
          .createBlock(() -> CryoBlockEntities.HEAT_REJECTION_PORT, CryoLang.DESCRIPTION_HEAT_REJECTION_PORT)
          .withGui(() -> CryoMenus.LIQUEFACTION_PLANT, CryoLang.LIQUEFACTION_PLANT)
          .with(Attributes.ACTIVE)
          .externalMultiblock()
          .build();
    public static final BlockTypeTile<TileEntityLiquefactionController> LIQUEFACTION_CONTROLLER = BlockTileBuilder
          .createBlock(() -> CryoBlockEntities.LIQUEFACTION_CONTROLLER, CryoLang.DESCRIPTION_LIQUEFACTION_CONTROLLER)
          .withGui(() -> CryoMenus.LIQUEFACTION_PLANT, CryoLang.LIQUEFACTION_PLANT)
          .with(Attributes.ACTIVE, new AttributeStateFacing())
          .externalMultiblock()
          .build();

    // Cargo Receiver (space workstream A): no energy, no upgrades, no active state
    public static final BlockTypeTile<TileEntityCargoReceiver> CARGO_RECEIVER = BlockTileBuilder
          .createBlock(() -> CryoBlockEntities.CARGO_RECEIVER, ReceiverLang.DESCRIPTION_CARGO_RECEIVER)
          .withGui(() -> CryoMenus.CARGO_RECEIVER)
          .with(new AttributeStateFacing(), Attributes.INVENTORY, Attributes.SECURITY, Attributes.REDSTONE, Attributes.COMPARATOR)
          .withSideConfig(TransmissionType.ITEM)
          .build();

    // Mission Control (workstream D): no energy, so a plain tile block type rather than a Machine
    public static final BlockTypeTile<TileEntityMissionControl> MISSION_CONTROL = BlockTileBuilder
          .createBlock(() -> CryoBlockEntities.MISSION_CONTROL, MissionLang.DESCRIPTION)
          .withGui(() -> CryoMenus.MISSION_CONTROL)
          .with(Attributes.ACTIVE_LIGHT, new AttributeStateFacing(), Attributes.INVENTORY, Attributes.SECURITY)
          .build();

    // Launch Pad (workstream B)
    public static final BlockTypeTile<TileEntityLaunchPadPlating> LAUNCH_PAD_PLATING = BlockTileBuilder
          .createBlock(() -> CryoBlockEntities.LAUNCH_PAD_PLATING, LaunchLang.DESCRIPTION_PLATING)
          .externalMultiblock()
          .build();
    public static final BlockTypeTile<TileEntityLaunchPadFuelPort> LAUNCH_PAD_FUEL_PORT = BlockTileBuilder
          .createBlock(() -> CryoBlockEntities.LAUNCH_PAD_FUEL_PORT, LaunchLang.DESCRIPTION_FUEL_PORT)
          .externalMultiblock()
          .build();
    public static final BlockTypeTile<TileEntityLaunchController> LAUNCH_CONTROLLER = BlockTileBuilder
          .createBlock(() -> CryoBlockEntities.LAUNCH_CONTROLLER, LaunchLang.DESCRIPTION_CONTROLLER)
          .withGui(() -> CryoMenus.LAUNCH_CONTROLLER, LaunchLang.ROCKET_ASSEMBLY)
          .with(Attributes.ACTIVE, Attributes.SECURITY, new AttributeStateFacing())
          .externalMultiblock()
          .build();

    // Superconducting Separator (superconductor workstream A): no energy and no upgrades, kept cold by its Cryostat
    public static final BlockTypeTile<TileEntitySuperconductingSeparator> SUPERCONDUCTING_SEPARATOR = BlockTileBuilder
          .createBlock(() -> CryoBlockEntities.SUPERCONDUCTING_SEPARATOR, SeparatorLang.DESCRIPTION)
          .withGui(() -> CryoMenus.SUPERCONDUCTING_SEPARATOR)
          .with(Attributes.ACTIVE_LIGHT, new AttributeStateFacing(), Attributes.SECURITY, Attributes.REDSTONE, Attributes.COMPARATOR)
          .withSideConfig(TransmissionType.CHEMICAL)
          .build();

    // Fusion Field Coils (superconductor workstream C): a plain ring block that frosts over while the ring is cold, and the
    // ring's controller with the cryostat, chemical side config, redstone control and security, but no energy or upgrades
    public static final BlockType FIELD_COIL = BlockTypeBuilder
          .createBlock(FusionCoilLang.DESCRIPTION_FIELD_COIL)
          .with(Attributes.ACTIVE)
          .build();
    public static final BlockTypeTile<TileEntityFieldCoilController> FIELD_COIL_CONTROLLER = BlockTileBuilder
          .createBlock(() -> CryoBlockEntities.FIELD_COIL_CONTROLLER, FusionCoilLang.DESCRIPTION_CONTROLLER)
          .withGui(() -> CryoMenus.FIELD_COIL_CONTROLLER)
          .with(Attributes.ACTIVE, new AttributeStateFacing(), Attributes.SECURITY, Attributes.REDSTONE)
          .withSideConfig(TransmissionType.CHEMICAL)
          .build();

    // SQUID Array (satellite workstream C): like the Superconducting Separator, no energy or upgrades, kept cold by its Cryostat
    public static final BlockTypeTile<io.github.valajar.cryosat.superconductor.squid.TileEntitySquidArray> SQUID_ARRAY = BlockTileBuilder
          .createBlock(() -> CryoBlockEntities.SQUID_ARRAY, io.github.valajar.cryosat.superconductor.squid.SquidLang.DESCRIPTION)
          .withGui(() -> CryoMenus.SQUID_ARRAY)
          .with(Attributes.ACTIVE_LIGHT, new AttributeStateFacing(), Attributes.SECURITY, Attributes.REDSTONE, Attributes.COMPARATOR)
          .withSideConfig(TransmissionType.CHEMICAL)
          .build();

    // Imaging Terminal (satellite workstream A): energy per request, Energy upgrades only; a redstone signal repeats the last
    // request instead of Mekanism's redstone control, and there is no comparator output
    public static final Machine<io.github.valajar.cryosat.space.survey.TileEntityImagingTerminal> IMAGING_TERMINAL = MachineBuilder
          .createMachine(() -> CryoBlockEntities.IMAGING_TERMINAL, io.github.valajar.cryosat.space.survey.SurveyLang.DESCRIPTION)
          .withGui(() -> CryoMenus.IMAGING_TERMINAL)
          .withEnergyConfig(() -> io.github.valajar.cryosat.space.survey.TileEntityImagingTerminal.BASE_ENERGY_PER_REQUEST,
                () -> io.github.valajar.cryosat.space.survey.TileEntityImagingTerminal.ENERGY_CAPACITY)
          .withSideConfig(TransmissionType.ENERGY)
          .withSupportedUpgrades(mekanism.api.Upgrade.ENERGY)
          .without(Attributes.AttributeRedstone.class, Attributes.AttributeComparator.class)
          .build();

    private CryoBlockTypes() {
    }
}
