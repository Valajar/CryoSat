package io.github.valajar.cryosat.space.mission;

import io.github.valajar.cryosat.registries.CryoBlocks;
import java.util.List;
import java.util.UUID;
import mekanism.api.IContentsListener;
import mekanism.common.capabilities.holder.slot.IInventorySlotHolder;
import mekanism.common.capabilities.holder.slot.InventorySlotHelper;
import mekanism.common.inventory.container.MekanismContainer;
import mekanism.common.inventory.slot.BasicInventorySlot;
import mekanism.common.tile.base.TileEntityMekanism;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

/**
 * Mission Control: lists the viewing player's orbital assets and runs their actions. Needs no power. The block is active
 * while its owner has an asset in orbit that still has work to do or cargo to deliver.
 */
public class TileEntityMissionControl extends TileEntityMekanism {
    /** Item position of the Uplink Card slot in the GUI. */
    public static final int CARD_SLOT_X = 8;
    public static final int CARD_SLOT_Y = 114;
    private static final int ACTIVE_CHECK_INTERVAL = 20;

    // Assigned from getInitialInventory, which runs inside the super constructor: no initialiser here
    private BasicInventorySlot cardSlot;

    /** Client copy of the viewing player's assets, filled by container sync. */
    private List<MissionSummary> clientMissions = List.of();

    public TileEntityMissionControl(BlockPos pos, BlockState state) {
        super(CryoBlocks.MISSION_CONTROL, pos, state);
    }

    @NotNull
    @Override
    protected IInventorySlotHolder getInitialInventory(IContentsListener listener) {
        InventorySlotHelper builder = InventorySlotHelper.forSide(this::getDirection);
        builder.addSlot(cardSlot = BasicInventorySlot.at(MissionControl::isUplinkCard, listener, CARD_SLOT_X, CARD_SLOT_Y, 1));
        return builder.build();
    }

    @Override
    protected boolean onUpdateServer() {
        boolean sendUpdatePacket = super.onUpdateServer();
        if (level.getGameTime() % ACTIVE_CHECK_INTERVAL == 0) {
            setActive(hasWorkInOrbit());
        }
        return sendUpdatePacket;
    }

    private boolean hasWorkInOrbit() {
        return level.getServer() != null && MissionControl.hasWorkInOrbit(level.getServer(), getOwnerUUID());
    }

    /**
     * Runs an action for a player, using the card in the slot for relinking. Server only.
     */
    public MissionResult perform(UUID player, MissionAction action, UUID asset, String name) {
        return MissionControl.perform(level.getServer(), player, action, asset, name, cardSlot.getStack());
    }

    @Override
    public void addContainerTrackers(MekanismContainer container) {
        super.addContainerTrackers(container);
        // Each viewer sees their own assets, so the getter asks for the container's player
        container.track(SyncableMissionList.create(
              () -> isRemote() || level.getServer() == null ? clientMissions : MissionControl.summaries(level.getServer(), container.getPlayerUUID()),
              list -> clientMissions = list));
    }

    public BasicInventorySlot getCardSlot() {
        return cardSlot;
    }

    public ItemStack getCard() {
        return cardSlot.getStack();
    }

    /** The viewing player's assets as last synced. Client only. */
    public List<MissionSummary> getClientMissions() {
        return clientMissions;
    }
}
