package io.github.valajar.cryosat.tile.liquefaction;

import io.github.valajar.cryosat.multiblock.liquefaction.ValveMode;
import io.github.valajar.cryosat.registries.plant.CryoLang;
import io.github.valajar.cryosat.registries.plant.PlantBlocks;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import mekanism.api.IContentsListener;
import mekanism.api.chemical.IChemicalHandler;
import mekanism.api.text.EnumColor;
import mekanism.common.attachments.containers.ContainerType;
import mekanism.common.capabilities.Capabilities;
import mekanism.common.capabilities.holder.chemical.IChemicalTankHolder;
import mekanism.common.lib.multiblock.MultiblockData.AdvancedCapabilityOutputTarget;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.BlockCapabilityCache;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Chemical port of the plant. Sneak right click (or a Configurator) cycles between feeding the input tank and draining one of
 * the three output tanks; output modes also push into whatever is attached.
 */
public class TileEntityCryoValve extends TileEntityCryoCasing {
    private final Map<Direction, BlockCapabilityCache<IChemicalHandler, @Nullable Direction>> capabilityCaches = new EnumMap<>(Direction.class);
    private ValveMode mode = ValveMode.INPUT;

    public TileEntityCryoValve(BlockPos pos, BlockState state) {
        super(PlantBlocks.CRYO_VALVE, pos, state);
        delaySupplier = NO_DELAY;
    }

    @NotNull
    @Override
    public IChemicalTankHolder getInitialChemicalTanks(IContentsListener listener) {
        return side -> getMultiblock().getValveTanks(mode);
    }

    @Override
    public boolean persists(ContainerType<?, ?, ?> type) {
        return type != ContainerType.CHEMICAL && super.persists(type);
    }

    public ValveMode getMode() {
        return mode;
    }

    public void setMode(ValveMode mode) {
        if (this.mode != mode) {
            this.mode = mode;
            setChanged();
            invalidateCapabilitiesFull();
        }
    }

    public void addChemicalTargetCapability(List<AdvancedCapabilityOutputTarget<IChemicalHandler, ValveMode>> targets, Direction side) {
        BlockCapabilityCache<IChemicalHandler, @Nullable Direction> cache = capabilityCaches.get(side);
        if (cache == null) {
            cache = Capabilities.CHEMICAL.createCache((ServerLevel) level, worldPosition.relative(side), side.getOpposite());
            capabilityCaches.put(side, cache);
        }
        targets.add(new AdvancedCapabilityOutputTarget<>(cache, m -> m == mode));
    }

    @Override
    public InteractionResult onSneakRightClick(Player player) {
        if (!isRemote()) {
            setMode(mode.next());
            player.displayClientMessage(CryoLang.VALVE_MODE_CHANGE.translateColored(EnumColor.GRAY, mode), true);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void saveAdditional(@NotNull CompoundTag nbt, @NotNull HolderLookup.Provider provider) {
        super.saveAdditional(nbt, provider);
        nbt.putInt("valve_mode", mode.ordinal());
    }

    @Override
    public void loadAdditional(@NotNull CompoundTag nbt, @NotNull HolderLookup.Provider provider) {
        super.loadAdditional(nbt, provider);
        mode = ValveMode.values()[Math.floorMod(nbt.getInt("valve_mode"), ValveMode.values().length)];
    }

    @Override
    public int getRedstoneLevel() {
        return getMultiblock().getCurrentRedstoneLevel();
    }
}
