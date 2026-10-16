package io.github.valajar.cryosat.tile.liquefaction;

import io.github.valajar.cryosat.multiblock.CryoMultiblocks;
import io.github.valajar.cryosat.multiblock.liquefaction.LiquefactionMultiblockData;
import io.github.valajar.cryosat.registries.plant.PlantBlocks;
import mekanism.common.lib.multiblock.MultiblockManager;
import mekanism.common.tile.prefab.TileEntityMultiblock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Plain casing of the Liquefaction Plant and the base of all its other parts.
 */
public class TileEntityCryoCasing extends TileEntityMultiblock<LiquefactionMultiblockData> {

    public TileEntityCryoCasing(BlockPos pos, BlockState state) {
        this(PlantBlocks.CRYO_CASING, pos, state);
    }

    public TileEntityCryoCasing(Holder<Block> provider, BlockPos pos, BlockState state) {
        super(provider, pos, state);
    }

    @Override
    public LiquefactionMultiblockData createMultiblock() {
        return new LiquefactionMultiblockData(this);
    }

    @Override
    public MultiblockManager<LiquefactionMultiblockData> getManager() {
        return CryoMultiblocks.LIQUEFACTION_PLANT;
    }
}
