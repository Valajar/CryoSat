package io.github.valajar.cryosat.space.launch;

import io.github.valajar.cryosat.registries.CryoBlocks;
import java.util.List;
import mekanism.api.IContentsListener;
import mekanism.common.attachments.containers.ContainerType;
import mekanism.common.capabilities.holder.chemical.IChemicalTankHolder;
import mekanism.common.capabilities.holder.slot.IInventorySlotHolder;
import mekanism.common.util.WorldUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

/**
 * Fuels the pad: accepts the oxidiser and propellant chemicals from Mekanism tubes and solid propellant grains from pipes.
 * Right clicking it opens the controller's Rocket Assembly screen (with the controller's security).
 */
public class TileEntityLaunchPadFuelPort extends TileEntityLaunchPadPlating {

    public TileEntityLaunchPadFuelPort(BlockPos pos, BlockState state) {
        super(CryoBlocks.LAUNCH_PAD_FUEL_PORT, pos, state);
        delaySupplier = NO_DELAY;
    }

    @NotNull
    @Override
    public IChemicalTankHolder getInitialChemicalTanks(IContentsListener listener) {
        return side -> getMultiblock().getChemicalTanks(side);
    }

    @NotNull
    @Override
    protected IInventorySlotHolder getInitialInventory(IContentsListener listener) {
        return side -> {
            LaunchPadMultiblockData pad = getMultiblock();
            if (side == null) {
                return pad.getInventorySlots(null);
            }
            return pad.isFormed() ? List.of(pad.solidFuelSlot) : List.of();
        };
    }

    @Override
    public boolean persists(ContainerType<?, ?, ?> type) {
        return type != ContainerType.CHEMICAL && super.persists(type);
    }

    @Override
    public ItemInteractionResult onActivate(Player player, InteractionHand hand, ItemStack stack) {
        LaunchPadMultiblockData pad = getMultiblock();
        if (player.isShiftKeyDown() || !pad.isFormed() || pad.controller == null) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        TileEntityLaunchController controller = WorldUtils.getTileEntity(TileEntityLaunchController.class, level, pad.controller);
        if (controller == null) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        InteractionResult result = controller.openGui(player);
        return result.consumesAction() ? ItemInteractionResult.SUCCESS : ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }
}
