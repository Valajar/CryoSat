package io.github.valajar.cryosat.space.survey;

import io.github.valajar.cryosat.CryoSat;
import io.github.valajar.cryosat.registries.CryoBlocks;
import io.github.valajar.cryosat.registries.CryoDataComponents;
import io.github.valajar.cryosat.space.Constellation;
import io.github.valajar.cryosat.space.IDeepScanModule;
import io.github.valajar.cryosat.space.ISurveyTerminal;
import io.github.valajar.cryosat.space.LocatorData;
import io.github.valajar.cryosat.space.OrbitalAsset;
import io.github.valajar.cryosat.space.OrbitalRegistry;
import io.github.valajar.cryosat.space.OrbitalTask;
import io.github.valajar.cryosat.space.SurveyTarget;
import io.github.valajar.cryosat.space.survey.TerminalView.CoverageInfo;
import io.github.valajar.cryosat.space.survey.TerminalView.PassInfo;
import io.github.valajar.cryosat.space.survey.TerminalView.TargetInfo;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;
import mekanism.api.Action;
import mekanism.api.AutomationType;
import mekanism.api.IContentsListener;
import mekanism.api.RelativeSide;
import mekanism.api.security.IBlockSecurityUtils;
import mekanism.common.attachments.component.AttachedSideConfig;
import mekanism.common.attachments.component.AttachedSideConfig.LightConfigInfo;
import mekanism.common.capabilities.energy.MachineEnergyContainer;
import mekanism.common.capabilities.holder.energy.EnergyContainerHelper;
import mekanism.common.capabilities.holder.energy.IEnergyContainerHolder;
import mekanism.common.capabilities.holder.slot.IInventorySlotHolder;
import mekanism.common.capabilities.holder.slot.InventorySlotHelper;
import mekanism.common.inventory.container.MekanismContainer;
import mekanism.common.inventory.slot.BasicInventorySlot;
import mekanism.common.inventory.slot.EnergyInventorySlot;
import mekanism.common.lib.transmitter.TransmissionType;
import mekanism.common.tile.component.TileComponentEjector;
import mekanism.common.tile.component.config.ConfigInfo;
import mekanism.common.tile.component.config.DataType;
import mekanism.common.tile.prefab.TileEntityConfigurableMachine;
import mekanism.common.util.WorldUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderSet;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.Structure;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Imaging Terminal: requests imaging passes from the viewing player's constellation and logs what they find. A Mekanism
 * machine with energy side configuration, upgrades (Energy) and security.
 * <p>
 * A request costs {@code 20 kJ * (1 + range / 4000)} (less with Energy upgrades), so wider constellations cost more per
 * request. When a pass completes, the satellite hands it here ({@link #receiveSurvey}): the terminal runs vanilla's explorer
 * map search around itself within the constellation's range, blurs the position by the constellation's uncertainty and
 * logs it. Underground targets need a SQUID Array ({@link IDeepScanModule}) next to the terminal, both to request and to
 * search. A rising redstone signal repeats the last request.
 */
public class TileEntityImagingTerminal extends TileEntityConfigurableMachine implements ISurveyTerminal {
    public static final long BASE_ENERGY_PER_REQUEST = 20_000;
    public static final long ENERGY_CAPACITY = 400_000;
    /** Range in blocks that adds the base cost once more. */
    public static final double RANGE_PER_EXTRA_COST = 4_000;
    public static final int MAX_LOG = 32;
    /** Item positions of the slots in the GUI. */
    public static final int LOCATOR_SLOT_X = 107;
    public static final int ENERGY_SLOT_X = 224;
    public static final int SLOT_Y = 130;
    private static final int ACTIVE_CHECK_INTERVAL = 20;

    /** Energy in from the back and the bottom. */
    public static final AttachedSideConfig DEFAULT_SIDE_CONFIG;

    static {
        Map<TransmissionType, LightConfigInfo> config = new EnumMap<>(TransmissionType.class);
        config.put(TransmissionType.ENERGY, new LightConfigInfo(Map.of(RelativeSide.BACK, DataType.INPUT, RelativeSide.BOTTOM, DataType.INPUT), false));
        DEFAULT_SIDE_CONFIG = new AttachedSideConfig(config);
    }

    // Assigned from the getInitial* methods, which run inside the super constructor: no field initialisers here
    private MachineEnergyContainer<TileEntityImagingTerminal> energyContainer;
    private EnergyInventorySlot energySlot;
    private BasicInventorySlot locatorSlot;

    /** Newest first, at most {@link #MAX_LOG}. On the client, the last synced copy. */
    private List<SurveyResult> log = new ArrayList<>();
    @Nullable
    private ResourceLocation lastTarget;
    @Nullable
    private UUID lastRequester;
    private boolean wasPowered;

    // Client copies, filled by container sync
    private List<TargetInfo> clientTargets = List.of();
    private List<PassInfo> clientPasses = List.of();
    private CoverageInfo clientCoverage = CoverageInfo.NONE;

    public TileEntityImagingTerminal(BlockPos pos, BlockState state) {
        super(CryoBlocks.IMAGING_TERMINAL, pos, state);
        configComponent.setupInputConfig(TransmissionType.ENERGY, energyContainer);
        DEFAULT_SIDE_CONFIG.configInfo().forEach((type, defaults) -> {
            // Mekanism leaves every side disabled until an item applies its config; give block placed terminals the defaults
            ConfigInfo info = configComponent.getConfig(type);
            if (info != null) {
                info.setEjecting(defaults.isEjecting());
                defaults.sideConfig().forEach((side, dataType) -> info.setDataType(dataType, side));
            }
        });
        ejectorComponent = new TileComponentEjector(this);
    }

    @NotNull
    @Override
    protected IEnergyContainerHolder getInitialEnergyContainers(IContentsListener listener) {
        EnergyContainerHelper builder = EnergyContainerHelper.forSideWithConfig(this);
        builder.addContainer(energyContainer = MachineEnergyContainer.input(this, listener));
        return builder.build();
    }

    @NotNull
    @Override
    protected IInventorySlotHolder getInitialInventory(IContentsListener listener) {
        InventorySlotHelper builder = InventorySlotHelper.forSide(this::getDirection);
        builder.addSlot(locatorSlot = BasicInventorySlot.at(TileEntityImagingTerminal::isLocator, listener, LOCATOR_SLOT_X, SLOT_Y, 1));
        builder.addSlot(energySlot = EnergyInventorySlot.fillOrConvert(energyContainer, this::getLevel, listener, ENERGY_SLOT_X, SLOT_Y));
        return builder.build();
    }

    /** @return whether a stack goes into the locator slot: anything that carries survey results, normally the Survey Locator */
    public static boolean isLocator(ItemStack stack) {
        return stack.has(CryoDataComponents.LOCATOR_DATA);
    }

    @Override
    protected boolean onUpdateServer() {
        boolean sendUpdatePacket = super.onUpdateServer();
        energySlot.fillContainerOrConvert();
        boolean powered = level.hasNeighborSignal(worldPosition);
        if (powered != wasPowered) {
            wasPowered = powered;
            markForSave();
            if (powered && lastTarget != null && lastRequester != null
                && IBlockSecurityUtils.INSTANCE.canAccess(lastRequester, level, worldPosition, this)) {
                request(lastRequester, lastTarget);
            }
        }
        if (level.getGameTime() % ACTIVE_CHECK_INTERVAL == 0) {
            setActive(!passes().isEmpty());
        }
        return sendUpdatePacket;
    }

    // Requests

    /** Joules one request costs with a constellation of this range. */
    public long requestCost(int range) {
        return (long) Math.ceil(energyContainer.getEnergyPerTick() * (1 + range / RANGE_PER_EXTRA_COST));
    }

    /**
     * Requests an imaging pass for a player. The caller has checked that the player may use this terminal. Server only.
     */
    public RequestResult request(UUID requester, ResourceLocation targetId) {
        MinecraftServer server = level.getServer();
        SurveyTarget target = SurveySearch.target(targetId);
        if (server == null || target == null || target.resolve(level.registryAccess()).isEmpty()) {
            return RequestResult.UNKNOWN_TARGET;
        }
        lastTarget = targetId;
        lastRequester = requester;
        markForSave();
        if (target.deepScan() && deepScanModules().noneMatch(IDeepScanModule::canDeepScan)) {
            return RequestResult.NEEDS_DEEP_SCAN;
        }
        Constellation constellation = Constellation.of(server, requester, level.dimension());
        if (constellation.satellites().isEmpty()) {
            return RequestResult.NO_SATELLITES;
        } else if (constellation.free() == 0) {
            return RequestResult.ALL_BUSY;
        }
        long cost = requestCost(constellation.range());
        if (energyContainer.extract(cost, Action.SIMULATE, AutomationType.INTERNAL) < cost) {
            return RequestResult.NO_POWER;
        }
        Optional<OrbitalTask> task = OrbitalRegistry.get(server).requestSurvey(server, requester, GlobalPos.of(level.dimension(), worldPosition), targetId);
        if (task.isEmpty()) {
            return RequestResult.ALL_BUSY;
        }
        energyContainer.extract(cost, Action.EXECUTE, AutomationType.INTERNAL);
        setActive(true);
        return RequestResult.REQUESTED;
    }

    private Stream<IDeepScanModule> deepScanModules() {
        List<IDeepScanModule> modules = new ArrayList<>(2);
        for (Direction direction : Direction.values()) {
            if (WorldUtils.getTileEntity(level, worldPosition.relative(direction)) instanceof IDeepScanModule module) {
                modules.add(module);
            }
        }
        return modules.stream();
    }

    // Results

    @Override
    public boolean receiveSurvey(OrbitalTask task, OrbitalAsset satellite) {
        if (!(level instanceof ServerLevel serverLevel) || !SurveySearch.tryStartSearch(serverLevel.getServer())) {
            return false;
        }
        long now = serverLevel.getGameTime();
        SurveyTarget target = SurveySearch.target(task.target());
        if (target != null && target.deepScan() && deepScanModules().noneMatch(IDeepScanModule::useDeepScan)) {
            addToLog(new SurveyResult(task.id(), task.target(), SurveyResult.Outcome.NO_DEEP_SCAN, BlockPos.ZERO, 0, now));
            return true;
        }
        // Precision and reach of the constellation as it is now; the delivering satellite always counts
        Constellation constellation = Constellation.of(serverLevel.getServer(), task.requester(), level.dimension());
        if (constellation.satellites().isEmpty()) {
            constellation = new Constellation(List.of(satellite));
        }
        Optional<HolderSet<Structure>> structures = target == null ? Optional.empty() : target.resolve(level.registryAccess());
        BlockPos found = null;
        if (structures.isPresent()) {
            found = SurveySearch.find(serverLevel, structures.get(), worldPosition, constellation.range());
        }
        if (found == null) {
            addToLog(new SurveyResult(task.id(), task.target(), SurveyResult.Outcome.NOT_FOUND, BlockPos.ZERO, 0, now));
        } else {
            int uncertainty = constellation.uncertainty();
            addToLog(new SurveyResult(task.id(), task.target(), SurveyResult.Outcome.FOUND, SurveySearch.blur(found, uncertainty, task.id()), uncertainty, now));
        }
        return true;
    }

    private void addToLog(SurveyResult result) {
        List<SurveyResult> updated = new ArrayList<>(MAX_LOG);
        updated.add(result);
        log.stream().limit(MAX_LOG - 1).forEach(updated::add);
        log = updated;
        markForSave();
    }

    public void dismiss(UUID entry) {
        if (log.stream().anyMatch(result -> result.id().equals(entry))) {
            log = log.stream().filter(result -> !result.id().equals(entry)).toList();
            markForSave();
        }
    }

    /** Puts a found result first on the locator in the slot, dropping its oldest targets past {@link LocatorData#MAX_TARGETS}. */
    public RequestResult loadOntoLocator(UUID entry) {
        ItemStack stack = locatorSlot.getStack();
        SurveyResult result = log.stream().filter(r -> r.id().equals(entry) && r.found()).findFirst().orElse(null);
        if (result == null || !isLocator(stack)) {
            return RequestResult.NO_LOCATOR;
        }
        LocatorData.Entry loaded = new LocatorData.Entry(GlobalPos.of(level.dimension(), result.pos()), result.uncertainty(), result.target());
        LocatorData current = stack.getOrDefault(CryoDataComponents.LOCATOR_DATA, LocatorData.EMPTY);
        List<LocatorData.Entry> targets = new ArrayList<>(LocatorData.MAX_TARGETS);
        targets.add(loaded);
        current.targets().stream().filter(existing -> !existing.equals(loaded)).limit(LocatorData.MAX_TARGETS - 1).forEach(targets::add);
        ItemStack updated = stack.copy();
        updated.set(CryoDataComponents.LOCATOR_DATA, new LocatorData(List.copyOf(targets), 0));
        locatorSlot.setStack(updated);
        return RequestResult.LOADED;
    }

    // What the screen shows

    /** Passes in progress for this terminal, soonest first. Server only. */
    public List<PassInfo> passes() {
        if (!(level instanceof ServerLevel serverLevel)) {
            return clientPasses;
        }
        GlobalPos self = GlobalPos.of(level.dimension(), worldPosition);
        return OrbitalRegistry.get(serverLevel.getServer()).all().stream()
              .map(OrbitalAsset::satellite)
              .filter(satellite -> satellite != null && satellite.task != null && satellite.task.terminal().equals(self))
              .map(satellite -> new PassInfo(satellite.task.target(), satellite.task.completesAt()))
              .sorted(Comparator.comparingLong(PassInfo::completesAt))
              .toList();
    }

    private List<TargetInfo> targetInfos() {
        boolean deepReady = deepScanModules().anyMatch(IDeepScanModule::canDeepScan);
        return SurveySearch.targets().entrySet().stream()
              .map(entry -> {
                  SurveyTarget target = entry.getValue();
                  boolean exists = target.resolve(level.registryAccess()).isPresent();
                  return new TargetInfo(entry.getKey(), target.icon(), target.deepScan(), exists && (!target.deepScan() || deepReady));
              })
              .sorted(Comparator.comparing(info -> info.id().toString()))
              .toList();
    }

    private CoverageInfo coverage(@Nullable UUID viewer) {
        if (viewer == null || level.getServer() == null) {
            return CoverageInfo.NONE;
        }
        Constellation constellation = Constellation.of(level.getServer(), viewer, level.dimension());
        return new CoverageInfo(constellation.satellites().size(), (int) constellation.free(), constellation.coverage(), constellation.uncertainty(),
              constellation.range(), requestCost(constellation.range()));
    }

    @Override
    public void addContainerTrackers(MekanismContainer container) {
        super.addContainerTrackers(container);
        container.track(new SyncableCodecList<>(TargetInfo.STREAM_CODEC, () -> isRemote() ? clientTargets : targetInfos(), list -> clientTargets = list));
        container.track(new SyncableCodecList<>(PassInfo.STREAM_CODEC, this::passes, list -> clientPasses = list));
        // Each viewer sees their own constellation
        container.track(new SyncableCodecList<>(CoverageInfo.STREAM_CODEC, () -> List.of(isRemote() ? clientCoverage : coverage(container.getPlayerUUID())),
              list -> clientCoverage = list.isEmpty() ? CoverageInfo.NONE : list.getFirst()));
        container.track(new SyncableCodecList<>(SurveyResult.STREAM_CODEC, () -> log, list -> log = list));
    }

    // Saving

    @Override
    public void saveAdditional(@NotNull CompoundTag nbt, @NotNull HolderLookup.Provider provider) {
        super.saveAdditional(nbt, provider);
        SurveyResult.CODEC.listOf().encodeStart(NbtOps.INSTANCE, log)
              .resultOrPartial(error -> CryoSat.LOGGER.error("Could not save imaging log: {}", error))
              .ifPresent(tag -> nbt.put("log", tag));
        if (lastTarget != null && lastRequester != null) {
            nbt.putString("last_target", lastTarget.toString());
            nbt.putUUID("last_requester", lastRequester);
        }
        nbt.putBoolean("was_powered", wasPowered);
    }

    @Override
    public void loadAdditional(@NotNull CompoundTag nbt, @NotNull HolderLookup.Provider provider) {
        super.loadAdditional(nbt, provider);
        log = nbt.contains("log") ? SurveyResult.CODEC.listOf().parse(NbtOps.INSTANCE, nbt.get("log"))
              .resultOrPartial(error -> CryoSat.LOGGER.error("Could not load imaging log: {}", error))
              .map(List::copyOf)
              .orElse(List.of()) : List.of();
        lastTarget = nbt.contains("last_target") ? ResourceLocation.tryParse(nbt.getString("last_target")) : null;
        lastRequester = nbt.hasUUID("last_requester") ? nbt.getUUID("last_requester") : null;
        wasPowered = nbt.getBoolean("was_powered");
    }

    // Getters

    public MachineEnergyContainer<TileEntityImagingTerminal> getEnergyContainer() {
        return energyContainer;
    }

    public BasicInventorySlot getLocatorSlot() {
        return locatorSlot;
    }

    /** Newest first; on the client, the last synced copy. */
    public List<SurveyResult> getLog() {
        return log;
    }

    @Nullable
    public ResourceLocation getLastTarget() {
        return lastTarget;
    }

    /** Client only. */
    public List<TargetInfo> getClientTargets() {
        return clientTargets;
    }

    /** Client only. */
    public CoverageInfo getClientCoverage() {
        return clientCoverage;
    }
}
