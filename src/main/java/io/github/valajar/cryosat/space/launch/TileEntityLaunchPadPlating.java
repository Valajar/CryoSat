package io.github.valajar.cryosat.space.launch;

import io.github.valajar.cryosat.multiblock.CryoMultiblocks;
import io.github.valajar.cryosat.registries.CryoBlocks;
import java.util.List;
import mekanism.api.IContentsListener;
import mekanism.common.capabilities.holder.slot.IInventorySlotHolder;
import mekanism.common.lib.multiblock.MultiblockManager;
import mekanism.common.tile.prefab.TileEntityMultiblock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

/**
 * Plain plating of the Launch Pad and the base of its other parts. Only the controller may be the multiblock master, so
 * the pad's state is always synced through the controller.
 */
public class TileEntityLaunchPadPlating extends TileEntityMultiblock<LaunchPadMultiblockData> {

    public TileEntityLaunchPadPlating(BlockPos pos, BlockState state) {
        this(CryoBlocks.LAUNCH_PAD_PLATING, pos, state);
    }

    public TileEntityLaunchPadPlating(Holder<Block> provider, BlockPos pos, BlockState state) {
        super(provider, pos, state);
    }

    @Override
    public LaunchPadMultiblockData createMultiblock() {
        return new LaunchPadMultiblockData(this);
    }

    @Override
    public MultiblockManager<LaunchPadMultiblockData> getManager() {
        return CryoMultiblocks.LAUNCH_PAD;
    }

    @Override
    public boolean canBeMaster() {
        return false;
    }

    /** The assembly slots only show in the GUI; plating exposes nothing to pipes. */
    @NotNull
    @Override
    protected IInventorySlotHolder getInitialInventory(IContentsListener listener) {
        return side -> side == null ? getMultiblock().getInventorySlots(null) : List.of();
    }
}
