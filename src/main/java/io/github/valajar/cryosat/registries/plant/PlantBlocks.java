package io.github.valajar.cryosat.registries.plant;

import io.github.valajar.cryosat.CryoSat;
import io.github.valajar.cryosat.tile.liquefaction.TileEntityCryoCasing;
import io.github.valajar.cryosat.tile.liquefaction.TileEntityCryoValve;
import io.github.valajar.cryosat.tile.liquefaction.TileEntityCryocooler;
import io.github.valajar.cryosat.tile.liquefaction.TileEntityHeatRejectionPort;
import io.github.valajar.cryosat.tile.liquefaction.TileEntityLiquefactionController;
import mekanism.common.block.prefab.BlockBasicMultiblock;
import mekanism.common.item.block.ItemBlockTooltip;
import mekanism.common.registration.impl.BlockDeferredRegister;
import mekanism.common.registration.impl.BlockRegistryObject;
import net.minecraft.world.level.material.MapColor;

public final class PlantBlocks {
    public static final BlockDeferredRegister BLOCKS = new BlockDeferredRegister(CryoSat.MODID);

    public static final BlockRegistryObject<BlockBasicMultiblock<TileEntityCryoCasing>, ItemBlockTooltip<BlockBasicMultiblock<TileEntityCryoCasing>>> CRYO_CASING =
          BLOCKS.registerDetails("cryo_casing", () -> new BlockBasicMultiblock<>(PlantBlockTypes.CRYO_CASING, p -> p.mapColor(MapColor.METAL)));
    public static final BlockRegistryObject<BlockBasicMultiblock<TileEntityCryoValve>, ItemBlockTooltip<BlockBasicMultiblock<TileEntityCryoValve>>> CRYO_VALVE =
          BLOCKS.registerDetails("cryo_valve", () -> new BlockBasicMultiblock<>(PlantBlockTypes.CRYO_VALVE, p -> p.mapColor(MapColor.METAL)));
    public static final BlockRegistryObject<BlockBasicMultiblock<TileEntityCryocooler>, ItemBlockTooltip<BlockBasicMultiblock<TileEntityCryocooler>>> CRYOCOOLER =
          BLOCKS.registerDetails("cryocooler", () -> new BlockBasicMultiblock<>(PlantBlockTypes.CRYOCOOLER, p -> p.mapColor(MapColor.METAL)));
    public static final BlockRegistryObject<BlockBasicMultiblock<TileEntityHeatRejectionPort>, ItemBlockTooltip<BlockBasicMultiblock<TileEntityHeatRejectionPort>>> HEAT_REJECTION_PORT =
          BLOCKS.registerDetails("heat_rejection_port", () -> new BlockBasicMultiblock<>(PlantBlockTypes.HEAT_REJECTION_PORT, p -> p.mapColor(MapColor.METAL)));
    public static final BlockRegistryObject<BlockBasicMultiblock<TileEntityLiquefactionController>, ItemBlockTooltip<BlockBasicMultiblock<TileEntityLiquefactionController>>> LIQUEFACTION_CONTROLLER =
          BLOCKS.registerDetails("liquefaction_controller", () -> new BlockBasicMultiblock<>(PlantBlockTypes.LIQUEFACTION_CONTROLLER, p -> p.mapColor(MapColor.METAL)));

    private PlantBlocks() {
    }
}
