package io.github.valajar.cryosat.superconductor.separator;

import io.github.valajar.cryosat.registries.CryoBlocks;
import io.github.valajar.cryosat.space.CargoEntry;
import io.github.valajar.cryosat.space.ICargoProcessor;
import io.github.valajar.cryosat.space.ICargoReceiver;
import io.github.valajar.cryosat.space.OrbitalAsset;
import io.github.valajar.cryosat.space.SeparatorBonus;
import io.github.valajar.cryosat.superconductor.Cryostat;
import io.github.valajar.cryosat.superconductor.CryostatState;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import mekanism.api.IContentsListener;
import mekanism.api.RelativeSide;
import mekanism.common.attachments.component.AttachedSideConfig;
import mekanism.common.attachments.component.AttachedSideConfig.LightConfigInfo;
import mekanism.common.attachments.containers.ContainerType;
import mekanism.common.capabilities.holder.chemical.ChemicalTankHelper;
import mekanism.common.capabilities.holder.chemical.IChemicalTankHolder;
import mekanism.common.inventory.container.MekanismContainer;
import mekanism.common.inventory.container.sync.SyncableBoolean;
import mekanism.common.inventory.container.sync.SyncableEnum;
import mekanism.common.inventory.container.sync.SyncableInt;
import mekanism.common.inventory.container.sync.SyncableLong;
import mekanism.common.lib.transmitter.TransmissionType;
import mekanism.common.tile.component.TileComponentEjector;
import mekanism.common.tile.component.config.ConfigInfo;
import mekanism.common.tile.component.config.DataType;
import mekanism.common.tile.prefab.TileEntityConfigurableMachine;
import mekanism.common.util.MekanismUtils;
import mekanism.common.util.WorldUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Sorts cargo batches as they arrive at an adjacent {@link ICargoReceiver}: while its {@link Cryostat} is superconducting,
 * each batch gets {@link SeparatorBonus} (+50 percent and the asteroid class's extras). It works through
 * {@link ICargoProcessor} only, so anything that offers batches to its neighbours (the Cargo Receiver now, Space Hub docks
 * later) can use it.
 * <p>
 * Coolant: a steady {@link #TRICKLE} keeps it cold, and each processed batch costs {@link #COOLANT_PER_BATCH}, never an
 * amount per item. Without enough for a batch, the batch passes through untouched. No energy; redstone control pauses the
 * separating but the magnet is kept cold, so switching it back on needs no new cool down.
 * <p>
 * Chemical side config: liquid nitrogen in on every side but the top, nitrogen boil off out of the top with auto eject.
 */
public class TileEntitySuperconductingSeparator extends TileEntityConfigurableMachine implements ICargoProcessor {
    /** Liquid nitrogen in mB per tick that keeps the magnet cold. */
    public static final long TRICKLE = 1;
    /** Liquid nitrogen in mB for each batch separated. */
    public static final long COOLANT_PER_BATCH = 250;
    private static final int RECEIVER_CHECK_INTERVAL = 20;

    /** Chemical input on every side but the top, boil off output on top with auto eject. */
    public static final AttachedSideConfig DEFAULT_SIDE_CONFIG;

    static {
        Map<RelativeSide, DataType> sides = new EnumMap<>(RelativeSide.class);
        for (RelativeSide side : RelativeSide.values()) {
            sides.put(side, side == RelativeSide.TOP ? DataType.OUTPUT : DataType.INPUT);
        }
        Map<TransmissionType, LightConfigInfo> config = new EnumMap<>(TransmissionType.class);
        config.put(TransmissionType.CHEMICAL, new LightConfigInfo(sides, true));
        DEFAULT_SIDE_CONFIG = new AttachedSideConfig(config);
    }

    // Assigned from getInitialChemicalTanks, which runs inside the super constructor: no field initialiser here
    private Cryostat cryostat;

    private int batchesProcessed;
    private boolean receiverAdjacent;
    private int receiverCheckIn;

    public TileEntitySuperconductingSeparator(BlockPos pos, BlockState state) {
        super(CryoBlocks.SUPERCONDUCTING_SEPARATOR, pos, state);
        configComponent.setupIOConfig(TransmissionType.CHEMICAL, cryostat.coolantTank, cryostat.boilOffTank, RelativeSide.TOP);
        applyDefaultSideConfig();
        ejectorComponent = new TileComponentEjector(this);
        ejectorComponent.setOutputData(configComponent, TransmissionType.CHEMICAL);
    }

    /** Mekanism leaves every side disabled until an item applies its config; give block placed separators the defaults. */
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
    public IChemicalTankHolder getInitialChemicalTanks(IContentsListener listener) {
        ChemicalTankHelper builder = ChemicalTankHelper.forSideWithConfig(this);
        cryostat = new Cryostat(TRICKLE, listener);
        // Same order as the block item's attachment tanks in CryoBlocks
        builder.addTank(cryostat.coolantTank);
        builder.addTank(cryostat.boilOffTank);
        return builder.build();
    }

    @Override
    protected boolean onUpdateServer() {
        boolean sendUpdatePacket = super.onUpdateServer();
        cryostat.tick();
        if (--receiverCheckIn <= 0) {
            receiverCheckIn = RECEIVER_CHECK_INTERVAL;
            receiverAdjacent = findReceiver();
        }
        setActive(cryostat.isSuperconducting());
        return sendUpdatePacket;
    }

    private boolean findReceiver() {
        for (Direction direction : Direction.values()) {
            if (WorldUtils.getTileEntity(level, worldPosition.relative(direction)) instanceof ICargoReceiver) {
                return true;
            }
        }
        return false;
    }

    @Nullable
    @Override
    public List<CargoEntry> processBatch(List<CargoEntry> batch, OrbitalAsset from, RandomSource random) {
        if (!cryostat.isSuperconducting() || !canFunction() || !cryostat.use(COOLANT_PER_BATCH)) {
            return null;
        }
        batchesProcessed++;
        markForSave();
        return SeparatorBonus.apply(batch, from, random);
    }

    @Override
    public int getRedstoneLevel() {
        return MekanismUtils.redstoneLevelFromContents(cryostat.coolantTank.getStored(), cryostat.coolantTank.getCapacity());
    }

    @Override
    protected boolean makesComparatorDirty(ContainerType<?, ?, ?> type) {
        return type == ContainerType.CHEMICAL;
    }

    @Override
    public void saveAdditional(@NotNull CompoundTag nbt, @NotNull HolderLookup.Provider provider) {
        super.saveAdditional(nbt, provider);
        cryostat.save(nbt);
        nbt.putInt("batches_processed", batchesProcessed);
    }

    @Override
    public void loadAdditional(@NotNull CompoundTag nbt, @NotNull HolderLookup.Provider provider) {
        super.loadAdditional(nbt, provider);
        cryostat.load(nbt);
        batchesProcessed = nbt.getInt("batches_processed");
    }

    @Override
    public void addContainerTrackers(MekanismContainer container) {
        super.addContainerTrackers(container);
        container.track(SyncableEnum.create(CryostatState.BY_ID, CryostatState.NO_COOLANT, cryostat::getState, cryostat::setState));
        container.track(SyncableInt.create(cryostat::getCooldown, cryostat::setCooldown));
        container.track(SyncableLong.create(cryostat::getLastUsed, cryostat::setLastUsed));
        container.track(SyncableInt.create(this::getBatchesProcessed, value -> batchesProcessed = value));
        container.track(SyncableBoolean.create(this::isReceiverAdjacent, value -> receiverAdjacent = value));
    }

    public Cryostat getCryostat() {
        return cryostat;
    }

    /** Batches this separator has worked on since it was placed. */
    public int getBatchesProcessed() {
        return batchesProcessed;
    }

    /** Whether a Cargo Receiver (or anything else that offers batches) is next to it; checked once a second. */
    public boolean isReceiverAdjacent() {
        return receiverAdjacent;
    }
}
