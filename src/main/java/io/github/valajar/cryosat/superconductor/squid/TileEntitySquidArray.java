package io.github.valajar.cryosat.superconductor.squid;

import io.github.valajar.cryosat.CryoSat;
import io.github.valajar.cryosat.registries.CryoBlocks;
import io.github.valajar.cryosat.space.IDeepScanModule;
import io.github.valajar.cryosat.superconductor.Cryostat;
import io.github.valajar.cryosat.superconductor.CryostatState;
import java.util.EnumMap;
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
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

/**
 * SQUID Array: a superconducting magnetometer that lets an adjacent Imaging Terminal search for underground structures. It
 * only answers {@link IDeepScanModule}, so the terminal finds it by asking its neighbours.
 * <p>
 * Coolant: a steady {@link #TRICKLE} keeps it cold, and each deep scan costs {@link #COOLANT_PER_SCAN}. No energy; redstone
 * control refuses scans but keeps it cold, like the Superconducting Separator.
 * <p>
 * Chemical side config: liquid nitrogen in on every side but the top, nitrogen boil off out of the top with auto eject.
 */
public class TileEntitySquidArray extends TileEntityConfigurableMachine implements IDeepScanModule {
    /** Liquid nitrogen in mB per tick that keeps the sensor cold. */
    public static final long TRICKLE = 1;
    /** Liquid nitrogen in mB for each deep scan. */
    public static final long COOLANT_PER_SCAN = 500;
    /** The Imaging Terminal's block id; its tile lives in another part of the mod, so it is recognised by id. */
    public static final ResourceLocation IMAGING_TERMINAL = CryoSat.rl("imaging_terminal");
    private static final int TERMINAL_CHECK_INTERVAL = 20;

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

    private int scansDone;
    private boolean terminalAdjacent;
    private int terminalCheckIn;

    public TileEntitySquidArray(BlockPos pos, BlockState state) {
        super(CryoBlocks.SQUID_ARRAY, pos, state);
        configComponent.setupIOConfig(TransmissionType.CHEMICAL, cryostat.coolantTank, cryostat.boilOffTank, RelativeSide.TOP);
        applyDefaultSideConfig();
        ejectorComponent = new TileComponentEjector(this);
        ejectorComponent.setOutputData(configComponent, TransmissionType.CHEMICAL);
    }

    /** Mekanism leaves every side disabled until an item applies its config; give block placed arrays the defaults. */
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
        if (--terminalCheckIn <= 0) {
            terminalCheckIn = TERMINAL_CHECK_INTERVAL;
            terminalAdjacent = findTerminal();
        }
        setActive(cryostat.isSuperconducting());
        return sendUpdatePacket;
    }

    private boolean findTerminal() {
        for (Direction direction : Direction.values()) {
            BlockState neighbour = WorldUtils.getBlockState(level, worldPosition.relative(direction)).orElse(null);
            if (neighbour != null && IMAGING_TERMINAL.equals(BuiltInRegistries.BLOCK.getKey(neighbour.getBlock()))) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean canDeepScan() {
        return cryostat.isSuperconducting() && canFunction() && cryostat.coolantTank.getStored() >= COOLANT_PER_SCAN;
    }

    @Override
    public boolean useDeepScan() {
        if (!cryostat.isSuperconducting() || !canFunction() || !cryostat.use(COOLANT_PER_SCAN)) {
            return false;
        }
        scansDone++;
        markForSave();
        return true;
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
        nbt.putInt("scans_done", scansDone);
    }

    @Override
    public void loadAdditional(@NotNull CompoundTag nbt, @NotNull HolderLookup.Provider provider) {
        super.loadAdditional(nbt, provider);
        cryostat.load(nbt);
        scansDone = nbt.getInt("scans_done");
    }

    @Override
    public void addContainerTrackers(MekanismContainer container) {
        super.addContainerTrackers(container);
        container.track(SyncableEnum.create(CryostatState.BY_ID, CryostatState.NO_COOLANT, cryostat::getState, cryostat::setState));
        container.track(SyncableInt.create(cryostat::getCooldown, cryostat::setCooldown));
        container.track(SyncableLong.create(cryostat::getLastUsed, cryostat::setLastUsed));
        container.track(SyncableInt.create(this::getScansDone, value -> scansDone = value));
        container.track(SyncableBoolean.create(this::isTerminalAdjacent, value -> terminalAdjacent = value));
    }

    public Cryostat getCryostat() {
        return cryostat;
    }

    /** Deep scans this array has run since it was placed. */
    public int getScansDone() {
        return scansDone;
    }

    /** Whether an Imaging Terminal is next to it; checked once a second. */
    public boolean isTerminalAdjacent() {
        return terminalAdjacent;
    }
}
