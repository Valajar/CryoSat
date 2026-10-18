package io.github.valajar.cryosat.space.receiver;

import io.github.valajar.cryosat.registries.CryoBlocks;
import io.github.valajar.cryosat.space.ICargoReceiver;
import io.github.valajar.cryosat.space.OrbitalAsset;
import io.github.valajar.cryosat.space.OrbitalRegistry;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import mekanism.api.Action;
import mekanism.api.AutomationType;
import mekanism.api.IContentsListener;
import mekanism.api.RelativeSide;
import mekanism.api.inventory.IInventorySlot;
import mekanism.common.attachments.component.AttachedSideConfig;
import mekanism.common.attachments.component.AttachedSideConfig.LightConfigInfo;
import mekanism.common.capabilities.holder.slot.IInventorySlotHolder;
import mekanism.common.capabilities.holder.slot.InventorySlotHelper;
import mekanism.common.inventory.container.MekanismContainer;
import mekanism.common.inventory.slot.OutputInventorySlot;
import mekanism.common.lib.transmitter.TransmissionType;
import mekanism.common.tile.component.TileComponentEjector;
import mekanism.common.tile.component.config.ConfigInfo;
import mekanism.common.tile.component.config.DataType;
import mekanism.common.tile.component.config.slot.InventorySlotInfo;
import mekanism.common.tile.prefab.TileEntityConfigurableMachine;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import net.minecraft.util.RandomSource;
import net.minecraft.core.Direction;
import mekanism.common.util.WorldUtils;
import io.github.valajar.cryosat.space.ICargoProcessor;
import io.github.valajar.cryosat.space.CargoEntry;

/**
 * Catches cargo sent down by orbital assets linked to it with an Uplink Card. A plain Mekanism machine without energy:
 * 27 output slots behind item side configuration with auto eject, redstone control, security and a comparator output by
 * fill level.
 * <p>
 * Security: cargo is accepted from every asset linked here, whoever owns it. Linking is the gate, since storing this
 * receiver on an Uplink Card needs access to it. While redstone control turns the receiver off, it accepts nothing and
 * cargo waits in orbit.
 */
public class TileEntityCargoReceiver extends TileEntityConfigurableMachine implements ICargoReceiver {
    public static final int ROWS = 3;
    public static final int COLUMNS = 9;
    public static final int SLOTS = ROWS * COLUMNS;
    /** Position of the top left slot in the GUI. */
    public static final int GRID_X = 8;
    public static final int GRID_Y = 18;
    /** Extra GUI height over a standard Mekanism GUI for the mission screen; the menu moves the player inventory down by it. */
    public static final int GUI_EXTRA_HEIGHT = 40;

    /** Item output on every side except the front, auto eject off. */
    public static final AttachedSideConfig DEFAULT_SIDE_CONFIG;

    static {
        Map<RelativeSide, DataType> sides = new EnumMap<>(RelativeSide.class);
        for (RelativeSide side : RelativeSide.values()) {
            if (side != RelativeSide.FRONT) {
                sides.put(side, DataType.OUTPUT);
            }
        }
        Map<TransmissionType, LightConfigInfo> config = new EnumMap<>(TransmissionType.class);
        config.put(TransmissionType.ITEM, new LightConfigInfo(sides, false));
        DEFAULT_SIDE_CONFIG = new AttachedSideConfig(config);
    }

    // Assigned from getInitialInventory, which runs inside the super constructor: no field initialiser here
    private List<IInventorySlot> cargoSlots;

    /** Client side copy of the linked missions, synced while the GUI is open. */
    private List<LinkedMission> clientMissions = List.of();

    public TileEntityCargoReceiver(BlockPos pos, BlockState state) {
        super(CryoBlocks.CARGO_RECEIVER, pos, state);
        ConfigInfo itemConfig = configComponent.getConfig(TransmissionType.ITEM);
        if (itemConfig != null) {
            itemConfig.addSlotInfo(DataType.OUTPUT, new InventorySlotInfo(false, true, cargoSlots));
        }
        applyDefaultSideConfig();
        ejectorComponent = new TileComponentEjector(this);
        ejectorComponent.setOutputData(configComponent, TransmissionType.ITEM)
              .setCanEject(type -> canFunction());
    }

    /** Mekanism leaves every side disabled until an item applies its config; give block placed receivers the defaults. */
    private void applyDefaultSideConfig() {
        DEFAULT_SIDE_CONFIG.configInfo().forEach((type, defaults) -> {
            ConfigInfo info = configComponent.getConfig(type);
            if (info != null) {
                info.setEjecting(defaults.isEjecting());
                defaults.sideConfig().forEach((side, dataType) -> info.setDataType(dataType, side));
            }
        });
    }

    @NotNull
    @Override
    protected IInventorySlotHolder getInitialInventory(IContentsListener listener) {
        InventorySlotHelper builder = InventorySlotHelper.forSideWithConfig(this);
        List<IInventorySlot> slots = new ArrayList<>(SLOTS);
        for (int row = 0; row < ROWS; row++) {
            for (int column = 0; column < COLUMNS; column++) {
                OutputInventorySlot slot = OutputInventorySlot.at(listener, GRID_X + column * 18, GRID_Y + row * 18);
                builder.addSlot(slot);
                slots.add(slot);
            }
        }
        cargoSlots = Collections.unmodifiableList(slots);
        return builder.build();
    }

    /**
     * Fills stacks of the same item first, then empty slots, like a hopper would.
     */
    @Override
    public List<CargoEntry> processBatch(List<CargoEntry> batch, OrbitalAsset from, RandomSource random) {
        // The first adjacent processor that is working (a cold Superconducting Separator) handles the batch
        for (Direction direction : Direction.values()) {
            if (WorldUtils.getTileEntity(level, worldPosition.relative(direction)) instanceof ICargoProcessor processor) {
                List<CargoEntry> processed = processor.processBatch(batch, from, random);
                if (processed != null) {
                    return processed;
                }
            }
        }
        return batch;
    }

    @Override
    public ItemStack receiveCargo(ItemStack stack, OrbitalAsset from) {
        if (stack.isEmpty() || !canFunction()) {
            return stack;
        }
        ItemStack rest = stack;
        for (IInventorySlot slot : cargoSlots) {
            if (!slot.isEmpty() && ItemStack.isSameItemSameComponents(slot.getStack(), rest)) {
                rest = slot.insertItem(rest, Action.EXECUTE, AutomationType.INTERNAL);
                if (rest.isEmpty()) {
                    return ItemStack.EMPTY;
                }
            }
        }
        for (IInventorySlot slot : cargoSlots) {
            if (slot.isEmpty()) {
                rest = slot.insertItem(rest, Action.EXECUTE, AutomationType.INTERNAL);
                if (rest.isEmpty()) {
                    return ItemStack.EMPTY;
                }
            }
        }
        return rest;
    }

    public List<IInventorySlot> getCargoSlots() {
        return cargoSlots;
    }

    /**
     * @return on the server, the unfinished missions whose receiver is this block, read from the {@link OrbitalRegistry}; on
     * the client, the last synced copy
     */
    public List<LinkedMission> getLinkedMissions() {
        if (level instanceof ServerLevel serverLevel) {
            GlobalPos self = GlobalPos.of(serverLevel.dimension(), worldPosition);
            return OrbitalRegistry.get(serverLevel.getServer()).all().stream()
                  .filter(asset -> self.equals(asset.receiver) && !asset.isDone())
                  .map(LinkedMission::of)
                  .toList();
        }
        return clientMissions;
    }

    @Override
    public void addContainerTrackers(MekanismContainer container) {
        super.addContainerTrackers(container);
        container.track(new LinkedMission.Syncable(this::getLinkedMissions, missions -> clientMissions = missions));
    }
}
