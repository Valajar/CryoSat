package io.github.valajar.cryosat.superconductor.fusion;

import io.github.valajar.cryosat.registries.CryoBlocks;
import io.github.valajar.cryosat.superconductor.Cryostat;
import io.github.valajar.cryosat.superconductor.CryostatState;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import mekanism.api.Action;
import mekanism.api.IContentsListener;
import mekanism.api.RelativeSide;
import mekanism.common.attachments.component.AttachedSideConfig;
import mekanism.common.attachments.component.AttachedSideConfig.LightConfigInfo;
import mekanism.common.block.attribute.Attribute;
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
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The controller of a Fusion Field Coil ring set (see {@link FieldCoilRing} for the shape). It holds the ring's
 * {@link Cryostat}: liquid nitrogen comes in and nitrogen boil off goes out through chemical side configuration, with auto
 * eject.
 * <p>
 * While superconducting, enabled by redstone control, and the reactor inside is formed and burning, every tick it adds
 * {@link #BOOST_PER_LOOP} per complete loop of the reactor's current thermocouple output ({@code getPassiveGeneration(false,
 * true)}, the same number the reactor GUI shows as its production rate) straight into the reactor's own energy buffer, so the
 * reactor's ports send it out with the rest. Water cooled reactors turn part of their heat into steam instead; that steam is
 * not boosted, only the energy the thermocouples still make.
 * <p>
 * Coolant: a trickle of {@link #TRICKLE_PER_LOOP} mB/t per loop (at least one loop's worth while the ring is broken), plus
 * 1 mB per {@link #JOULES_PER_MB} of bonus energy. If the coolant for the bonus is short in a tick, that tick gets no bonus;
 * only running out of the trickle quenches the ring.
 */
public class TileEntityFieldCoilController extends TileEntityConfigurableMachine {
    public static final double BOOST_PER_LOOP = 0.25;
    public static final long TRICKLE_PER_LOOP = 2;
    /** Bonus energy per mB of liquid nitrogen boiled off: 100 kJ. Reliquefying 1 mB costs the plant only some tens of Joules. */
    public static final double JOULES_PER_MB = 100_000;
    public static final int SCAN_INTERVAL = 20;

    /**
     * Default chemical sides, relative to the front: liquid nitrogen in from the bottom and back, nitrogen out of the top and
     * front with auto eject. In a three loop ring the middle loop's controller has coils above and below it, so the back (the
     * gap side, where tubes may run) and the front stay usable.
     */
    public static final AttachedSideConfig DEFAULT_SIDE_CONFIG;

    static {
        Map<TransmissionType, LightConfigInfo> config = new EnumMap<>(TransmissionType.class);
        config.put(TransmissionType.CHEMICAL, new LightConfigInfo(Map.of(
              RelativeSide.BOTTOM, DataType.INPUT,
              RelativeSide.BACK, DataType.INPUT,
              RelativeSide.TOP, DataType.OUTPUT,
              RelativeSide.FRONT, DataType.OUTPUT
        ), true));
        DEFAULT_SIDE_CONFIG = new AttachedSideConfig(config);
    }

    // Assigned from getInitialChemicalTanks, which runs inside the super constructor: no field initialiser here
    private Cryostat cryostat;

    private final FusionReactorAccess access = FusionReactorAccess.get();
    private FieldCoilRing.Scan scan = FieldCoilRing.Scan.fail(RingProblem.NO_REACTOR);
    private boolean needsScan = true;
    private double coolantRemainder;
    /** Coils this controller frosted, so it can thaw them when the ring changes or the controller is removed. */
    private final Set<BlockPos> frosted = new HashSet<>();
    private boolean coilsFrosted;

    // Synced to the GUI
    private RingProblem problem = RingProblem.NO_REACTOR;
    private long problemPos;
    private int gap;
    private int loops;
    private boolean hasIncomplete;
    private long incompletePos;
    private long lastBonus;
    private long lastCoolant;
    private long lastPassive;
    private boolean reactorBurning;
    private boolean waterCooled;

    public TileEntityFieldCoilController(BlockPos pos, BlockState state) {
        super(CryoBlocks.FIELD_COIL_CONTROLLER, pos, state);
        configComponent.setupIOConfig(TransmissionType.CHEMICAL, cryostat.coolantTank, cryostat.boilOffTank, RelativeSide.TOP);
        applyDefaultSideConfig();
        ejectorComponent = new TileComponentEjector(this);
        ejectorComponent.setOutputData(configComponent, TransmissionType.CHEMICAL);
    }

    /** Mekanism leaves every side disabled until an item applies its config; give block placed controllers the defaults. */
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
        cryostat = new Cryostat(TRICKLE_PER_LOOP, listener);
        builder.addTank(cryostat.coolantTank);
        builder.addTank(cryostat.boilOffTank);
        return builder.build();
    }

    @Override
    protected boolean onUpdateServer() {
        boolean sendUpdatePacket = super.onUpdateServer();
        if (needsScan || (level.getGameTime() + worldPosition.hashCode()) % SCAN_INTERVAL == 0) {
            rescan();
        }
        cryostat.setTrickle(TRICKLE_PER_LOOP * Math.max(1, loops));
        cryostat.tick();

        lastCoolant = cryostat.getState() == CryostatState.NO_COOLANT ? 0 : cryostat.getTrickle();
        lastBonus = 0;
        lastPassive = 0;
        reactorBurning = false;
        waterCooled = false;
        FusionReactorAccess.Reactor reactor = null;
        if (scan.works()) {
            reactor = access.get(level, scan.reactorMin(), scan.reactorMax());
            if (reactor == null) {
                // The reactor came apart: look again next tick
                needsScan = true;
            }
        }
        if (reactor != null) {
            reactorBurning = reactor.isBurning();
            waterCooled = reactor.isWaterCooled();
            lastPassive = reactor.passiveGeneration();
            if (reactorBurning && canFunction() && cryostat.isSuperconducting()) {
                boost(reactor);
            }
        }
        setActive(cryostat.isSuperconducting());
        boolean frost = cryostat.isSuperconducting() && scan.works();
        if (frost != coilsFrosted) {
            updateCoils();
        }
        return sendUpdatePacket;
    }

    private void boost(FusionReactorAccess.Reactor reactor) {
        long bonus = bonusFor(loops, lastPassive);
        // Never boil coolant off for energy the reactor's full buffer would refuse
        bonus = reactor.insertEnergy(bonus, Action.SIMULATE);
        if (bonus <= 0) {
            return;
        }
        double cost = bonus / JOULES_PER_MB + coolantRemainder;
        long whole = (long) cost;
        if (!cryostat.use(whole)) {
            return;
        }
        coolantRemainder = cost - whole;
        lastCoolant += whole;
        lastBonus = reactor.insertEnergy(bonus, Action.EXECUTE);
    }

    /** Joules per tick added to a reactor making {@code passive} J/t with {@code loops} complete loops. */
    public static long bonusFor(int loops, long passive) {
        return (long) (BOOST_PER_LOOP * Math.min(loops, FieldCoilRing.MAX_LOOPS) * passive);
    }

    /** Checks the ring now and updates what the GUI shows. */
    public void rescan() {
        needsScan = false;
        scan = FieldCoilRing.scan(level, worldPosition, access);
        problem = scan.problem();
        problemPos = scan.at() == null ? 0 : scan.at().asLong();
        gap = scan.gap();
        loops = scan.loops();
        hasIncomplete = scan.incomplete() != null;
        incompletePos = hasIncomplete ? scan.incomplete().asLong() : 0;
        updateCoils();
    }

    /** Frosts the coils of a working, cold ring and thaws every coil this controller frosted that no longer belongs to it. */
    private void updateCoils() {
        coilsFrosted = cryostat.isSuperconducting() && scan.works();
        Set<BlockPos> wanted = new HashSet<>();
        if (coilsFrosted) {
            for (BlockPos pos : scan.coils()) {
                if (!pos.equals(worldPosition)) {
                    wanted.add(pos);
                }
            }
        }
        for (BlockPos pos : frosted) {
            if (!wanted.contains(pos)) {
                setCoilActive(pos, false);
            }
        }
        for (BlockPos pos : wanted) {
            setCoilActive(pos, true);
        }
        frosted.clear();
        frosted.addAll(wanted);
    }

    private void setCoilActive(BlockPos pos, boolean active) {
        if (level == null || !level.isLoaded(pos)) {
            return;
        }
        BlockState state = level.getBlockState(pos);
        if (state.is(CryoBlocks.FIELD_COIL.get()) && Attribute.isActive(state) != active) {
            // Clients only: a frost change is cosmetic, so neighbours (including this controller) are not notified
            level.setBlock(pos, Attribute.setActive(state, active), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    public void onNeighborChange(Block block, BlockPos neighborPos) {
        super.onNeighborChange(block, neighborPos);
        needsScan = true;
    }

    @Override
    public void onAdded() {
        super.onAdded();
        needsScan = true;
    }

    @Override
    public void blockRemoved() {
        super.blockRemoved();
        if (!isRemote()) {
            for (BlockPos pos : frosted) {
                setCoilActive(pos, false);
            }
            frosted.clear();
        }
    }

    @Override
    public void saveAdditional(@NotNull CompoundTag nbt, @NotNull HolderLookup.Provider provider) {
        super.saveAdditional(nbt, provider);
        cryostat.save(nbt);
        nbt.putDouble("coolant_remainder", coolantRemainder);
    }

    @Override
    public void loadAdditional(@NotNull CompoundTag nbt, @NotNull HolderLookup.Provider provider) {
        super.loadAdditional(nbt, provider);
        cryostat.load(nbt);
        coolantRemainder = nbt.getDouble("coolant_remainder");
    }

    @Override
    public void addContainerTrackers(MekanismContainer container) {
        super.addContainerTrackers(container);
        container.track(SyncableEnum.create(CryostatState.BY_ID, CryostatState.NO_COOLANT, cryostat::getState, cryostat::setState));
        container.track(SyncableInt.create(cryostat::getCooldown, cryostat::setCooldown));
        container.track(SyncableLong.create(() -> lastCoolant, value -> lastCoolant = value));
        container.track(SyncableEnum.create(RingProblem.BY_ID, RingProblem.NO_REACTOR, () -> problem, value -> problem = value));
        container.track(SyncableLong.create(() -> problemPos, value -> problemPos = value));
        container.track(SyncableInt.create(() -> gap, value -> gap = value));
        container.track(SyncableInt.create(() -> loops, value -> loops = value));
        container.track(SyncableBoolean.create(() -> hasIncomplete, value -> hasIncomplete = value));
        container.track(SyncableLong.create(() -> incompletePos, value -> incompletePos = value));
        container.track(SyncableLong.create(() -> lastBonus, value -> lastBonus = value));
        container.track(SyncableLong.create(() -> lastPassive, value -> lastPassive = value));
        container.track(SyncableBoolean.create(() -> reactorBurning, value -> reactorBurning = value));
        container.track(SyncableBoolean.create(() -> waterCooled, value -> waterCooled = value));
    }

    public Cryostat getCryostat() {
        return cryostat;
    }

    /** The last ring check (server only). */
    public FieldCoilRing.Scan getScan() {
        return scan;
    }

    public RingProblem getProblem() {
        return problem;
    }

    @Nullable
    public BlockPos getProblemPos() {
        return problem.hasPosition() ? BlockPos.of(problemPos) : null;
    }

    public int getGap() {
        return gap;
    }

    public int getLoops() {
        return loops;
    }

    @Nullable
    public BlockPos getIncompletePos() {
        return hasIncomplete ? BlockPos.of(incompletePos) : null;
    }

    /** Joules added to the reactor during the last tick. */
    public long getLastBonus() {
        return lastBonus;
    }

    /** Liquid nitrogen drained during the last tick: the trickle plus the coolant for the bonus. */
    public long getLastCoolant() {
        return lastCoolant;
    }

    /** The reactor's own thermocouple output read during the last tick, before the bonus. */
    public long getLastPassive() {
        return lastPassive;
    }

    public boolean isReactorBurning() {
        return reactorBurning;
    }

    public boolean isWaterCooled() {
        return waterCooled;
    }

    /** Coils of the counted loops, for tests. */
    public List<BlockPos> getRingCoils() {
        return scan.coils();
    }
}
