package io.github.valajar.cryosat.registries;

import io.github.valajar.cryosat.CryoLang;
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

    private CryoBlockTypes() {
    }
}
