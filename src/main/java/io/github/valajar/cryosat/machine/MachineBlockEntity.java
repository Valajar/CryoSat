package io.github.valajar.cryosat.machine;

import io.github.valajar.cryosat.capability.EnergyInputHandler;
import io.github.valajar.cryosat.capability.MachineEnergyContainer;
import io.github.valajar.cryosat.capability.SidedChemicalHandler;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import mekanism.api.RelativeSide;
import mekanism.api.chemical.IChemicalHandler;
import mekanism.api.chemical.IChemicalTank;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * Shared plumbing for CryoSat machines: a Joule energy buffer, Mekanism chemical tanks exposed per relative side, a
 * status line for the GUI, the active block state, saving, and throttled client sync.
 */
public abstract class MachineBlockEntity extends BlockEntity {
    private static final int SYNC_INTERVAL = 10;

    protected final MachineEnergyContainer energy;
    private final EnergyInputHandler energyInput;
    private final List<IChemicalTank> tanks = new ArrayList<>();
    private final Map<Direction, IChemicalHandler> chemicalHandlers = new EnumMap<>(Direction.class);
    private MachineStatus status = MachineStatus.IDLE;
    private boolean syncNeeded;

    protected MachineBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, long energyCapacity) {
        super(type, pos, state);
        this.energy = new MachineEnergyContainer(energyCapacity, this::markContentsChanged);
        this.energyInput = new EnergyInputHandler(energy);
    }

    protected <T extends IChemicalTank> T addTank(T tank) {
        tanks.add(tank);
        return tank;
    }

    /** Tanks this machine exposes on the given side, relative to its facing. */
    protected abstract List<IChemicalTank> tanksOn(RelativeSide side);

    /** Whether energy can be inserted from the given side, relative to its facing. */
    protected abstract boolean acceptsEnergyFrom(RelativeSide side);

    protected abstract void serverTick();

    public final void tickServer() {
        serverTick();
        if (syncNeeded && level != null && level.getGameTime() % SYNC_INTERVAL == 0) {
            syncNeeded = false;
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    protected void markContentsChanged() {
        setChanged();
        syncNeeded = true;
    }

    public Direction getFacing() {
        return getBlockState().getValue(MachineBlock.FACING);
    }

    protected RelativeSide relative(Direction side) {
        return RelativeSide.fromDirections(getFacing(), side);
    }

    // --- capabilities -----------------------------------------------------------------------------

    @Nullable
    public IChemicalHandler getChemicalHandler(@Nullable Direction side) {
        if (side == null) {
            return new SidedChemicalHandler(this::getTanks, null);
        }
        if (tanksOn(relative(side)).isEmpty()) {
            return null;
        }
        return chemicalHandlers.computeIfAbsent(side, s -> new SidedChemicalHandler(() -> tanksOn(relative(s)), s));
    }

    @Nullable
    public EnergyInputHandler getEnergyHandler(@Nullable Direction side) {
        return side == null || acceptsEnergyFrom(relative(side)) ? energyInput : null;
    }

    // --- state ------------------------------------------------------------------------------------

    public List<IChemicalTank> getTanks() {
        return Collections.unmodifiableList(tanks);
    }

    public MachineEnergyContainer getEnergy() {
        return energy;
    }

    public MachineStatus getStatus() {
        return status;
    }

    protected void setStatus(MachineStatus status) {
        if (this.status != status) {
            this.status = status;
            markContentsChanged();
        }
        setActive(status == MachineStatus.RUNNING);
    }

    private void setActive(boolean active) {
        BlockState state = getBlockState();
        if (level != null && state.getValue(MachineBlock.ACTIVE) != active) {
            level.setBlock(worldPosition, state.setValue(MachineBlock.ACTIVE, active), 3);
        }
    }

    // --- saving and sync --------------------------------------------------------------------------

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("energy", energy.serializeNBT(registries));
        ListTag tankList = new ListTag();
        for (IChemicalTank tank : tanks) {
            tankList.add(tank.serializeNBT(registries));
        }
        tag.put("tanks", tankList);
        tag.putString("status", status.getSerializedName());
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        energy.deserializeNBT(registries, tag.getCompound("energy"));
        ListTag tankList = tag.getList("tanks", Tag.TAG_COMPOUND);
        for (int i = 0; i < tanks.size() && i < tankList.size(); i++) {
            tanks.get(i).deserializeNBT(registries, tankList.getCompound(i));
        }
        status = MachineStatus.byName(tag.getString("status"));
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
