package io.github.valajar.cryosat.registries;

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
    public static final long CRYOCOOLER_ENERGY_STORAGE = 40_000;

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
          .withEnergyConfig(() -> CRYOCOOLER_ENERGY_STORAGE)
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

    private CryoBlockTypes() {
    }
}
