package io.github.valajar.cryosat.machine.compressor;

import io.github.valajar.cryosat.atmosphere.Atmosphere;
import io.github.valajar.cryosat.atmosphere.AtmosphereManager;
import io.github.valajar.cryosat.machine.MachineStatus;
import io.github.valajar.cryosat.registries.CryoBlocks;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import mekanism.api.Action;
import mekanism.api.AutomationType;
import mekanism.api.IContentsListener;
import mekanism.api.RelativeSide;
import mekanism.api.Upgrade;
import mekanism.api.chemical.BasicChemicalTank;
import mekanism.api.chemical.ChemicalStack;
import mekanism.api.chemical.IChemicalTank;
import mekanism.api.math.MathUtils;
import mekanism.common.attachments.component.AttachedSideConfig;
import mekanism.common.attachments.component.AttachedSideConfig.LightConfigInfo;
import mekanism.common.attachments.containers.ContainerType;
import mekanism.common.capabilities.energy.MachineEnergyContainer;
import mekanism.common.capabilities.holder.chemical.ChemicalTankHelper;
import mekanism.common.capabilities.holder.chemical.IChemicalTankHolder;
import mekanism.common.capabilities.holder.energy.EnergyContainerHelper;
import mekanism.common.capabilities.holder.energy.IEnergyContainerHolder;
import mekanism.common.capabilities.holder.slot.IInventorySlotHolder;
import mekanism.common.capabilities.holder.slot.InventorySlotHelper;
import mekanism.common.inventory.container.MekanismContainer;
import mekanism.common.inventory.container.slot.SlotOverlay;
import mekanism.common.inventory.container.sync.SyncableDouble;
import mekanism.common.inventory.container.sync.SyncableEnum;
import mekanism.common.inventory.container.sync.SyncableLong;
import mekanism.common.inventory.slot.EnergyInventorySlot;
import mekanism.common.inventory.slot.chemical.ChemicalInventorySlot;
import mekanism.common.lib.transmitter.TransmissionType;
import mekanism.common.tile.component.TileComponentEjector;
import mekanism.common.tile.component.config.ConfigInfo;
import mekanism.common.tile.component.config.DataType;
import mekanism.common.tile.component.config.slot.InventorySlotInfo;
import mekanism.common.tile.prefab.TileEntityConfigurableMachine;
import mekanism.common.util.MekanismUtils;
import mekanism.common.util.UpgradeUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

/**
 * Pulls the local {@link Atmosphere} out of the surrounding air into its output tank. Built on Mekanism's configurable
 * machine: side configuration for items, chemicals and energy, auto eject, upgrades, redstone control and security all
 * come from Mekanism.
 * <p>
 * Upgrades follow Mekanism's usual maths: Speed multiplies the output rate by {@code m^(speed/8)} and the energy per tick
 * by {@code m^(2*speed/8)}, Energy divides the energy per tick by {@code m^(energy/8)}, where {@code m} is Mekanism's
 * {@code maxUpgradeMultiplier} (10 by default). So energy per mB is {@code 10 J * m^((speed - energy)/8)}.
 */
public class AtmosphericCompressorBlockEntity extends TileEntityConfigurableMachine {
    public static final long ENERGY_CAPACITY = 40_000;
    public static final long TANK_CAPACITY = 10_000;
    /** Output in mB per tick with full air exposure at sea level, an atmosphere rate of 1 and no upgrades. */
    public static final double BASE_RATE = 10;
    public static final long ENERGY_PER_MB = 10;
    /** Energy per tick at {@link #BASE_RATE} without upgrades; the machine's base energy usage. */
    public static final long BASE_ENERGY_PER_TICK = (long) (BASE_RATE * ENERGY_PER_MB);
    /** Air blocks in the surrounding 3x3x3 needed for full output. */
    public static final int FULL_AIR_BLOCKS = 8;
    public static final double MIN_ALTITUDE_FACTOR = 0.4;
    private static final int AIR_CHECK_INTERVAL = 20;

    /**
     * Default side configuration, relative to the front: chemical output on top with auto eject, energy in from the back
     * and bottom, and Mekanism's usual item layout (inputs all round, output on the right, energy slot from the back).
     * The block item carries it as its default component, and it is also applied to compressors placed without an item.
     */
    public static final AttachedSideConfig DEFAULT_SIDE_CONFIG;

    static {
        Map<TransmissionType, LightConfigInfo> config = new EnumMap<>(TransmissionType.class);
        config.put(TransmissionType.ITEM, LightConfigInfo.MACHINE);
        config.put(TransmissionType.CHEMICAL, new LightConfigInfo(Map.of(RelativeSide.TOP, DataType.OUTPUT), true));
        config.put(TransmissionType.ENERGY, new LightConfigInfo(Map.of(RelativeSide.BACK, DataType.INPUT, RelativeSide.BOTTOM, DataType.INPUT), false));
        DEFAULT_SIDE_CONFIG = new AttachedSideConfig(config);
    }

    // Assigned from the getInitial* methods, which run inside the super constructor: no field initialisers here
    private IChemicalTank outputTank;
    private MachineEnergyContainer<AtmosphericCompressorBlockEntity> energyContainer;
    private ChemicalInventorySlot outputSlot;
    private EnergyInventorySlot energySlot;

    private MachineStatus status = MachineStatus.IDLE;
    private double speedMultiplier = 1;
    private double pending;
    private int airBlocks = -1;
    /** mB per tick produced during the last tick, 0 when stalled. */
    private double currentRate;
    private long lastEnergyUsed;

    public AtmosphericCompressorBlockEntity(BlockPos pos, BlockState state) {
        super(CryoBlocks.ATMOSPHERIC_COMPRESSOR, pos, state);
        ConfigInfo itemConfig = configComponent.getConfig(TransmissionType.ITEM);
        if (itemConfig != null) {
            itemConfig.addSlotInfo(DataType.INPUT, new InventorySlotInfo(true, false, outputSlot));
            itemConfig.addSlotInfo(DataType.OUTPUT, new InventorySlotInfo(false, true, outputSlot));
            itemConfig.addSlotInfo(DataType.ENERGY, new InventorySlotInfo(true, true, energySlot));
        }
        configComponent.setupOutputConfig(TransmissionType.CHEMICAL, outputTank, RelativeSide.TOP);
        configComponent.setupInputConfig(TransmissionType.ENERGY, energyContainer);
        applyDefaultSideConfig();

        ejectorComponent = new TileComponentEjector(this);
        ejectorComponent.setOutputData(configComponent, TransmissionType.CHEMICAL);
    }

    /** Mekanism leaves every side disabled until an item applies its config; give block placed compressors the defaults. */
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
        builder.addTank(outputTank = BasicChemicalTank.output(TANK_CAPACITY, listener));
        return builder.build();
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
        InventorySlotHelper builder = InventorySlotHelper.forSideWithConfig(this);
        builder.addSlot(outputSlot = ChemicalInventorySlot.drain(outputTank, listener, 28, 35));
        builder.addSlot(energySlot = EnergyInventorySlot.fillOrConvert(energyContainer, this::getLevel, listener, 143, 35));
        outputSlot.setSlotOverlay(SlotOverlay.PLUS);
        return builder.build();
    }

    @Override
    protected boolean onUpdateServer() {
        boolean sendUpdatePacket = super.onUpdateServer();
        energySlot.fillContainerOrConvert();
        outputSlot.drainTank();
        if (airBlocks < 0 || level.getGameTime() % AIR_CHECK_INTERVAL == 0) {
            airBlocks = countAirBlocks();
        }
        currentRate = 0;
        lastEnergyUsed = 0;
        status = produce();
        setActive(status == MachineStatus.RUNNING);
        return sendUpdatePacket;
    }

    private MachineStatus produce() {
        if (!canFunction()) {
            return MachineStatus.IDLE;
        }
        Atmosphere atmosphere = AtmosphereManager.INSTANCE.get(level.dimension());
        if (atmosphere == null) {
            return MachineStatus.NO_ATMOSPHERE;
        }
        if (airBlocks == 0) {
            return MachineStatus.NO_AIR;
        }
        if (!outputTank.isEmpty() && !outputTank.isTypeEqual(atmosphere.chemical())) {
            return MachineStatus.OUTPUT_MISMATCH;
        }
        if (outputTank.getNeeded() == 0) {
            return MachineStatus.OUTPUT_FULL;
        }
        // Fraction of the full (upgraded) rate the surroundings allow; energy use scales with it
        double environment = atmosphere.rate() * Math.min(1, airBlocks / (double) FULL_AIR_BLOCKS);
        if (atmosphere.altitudeScaling()) {
            environment *= altitudeFactor();
        }
        long cost = MathUtils.ceilToLong(energyContainer.getEnergyPerTick() * environment);
        if (energyContainer.extract(cost, Action.SIMULATE, AutomationType.INTERNAL) < cost) {
            return MachineStatus.NO_POWER;
        }
        lastEnergyUsed = energyContainer.extract(cost, Action.EXECUTE, AutomationType.INTERNAL);
        double rate = BASE_RATE * speedMultiplier * environment;
        pending += rate;
        long whole = Math.min((long) pending, outputTank.getNeeded());
        if (whole > 0) {
            outputTank.insert(new ChemicalStack(atmosphere.chemical(), whole), Action.EXECUTE, AutomationType.INTERNAL);
        }
        // Whatever did not fit into a full tank is lost rather than stored up
        pending = Math.min(pending - whole, 1);
        currentRate = rate;
        return MachineStatus.RUNNING;
    }

    /** 1 at or below sea level, falling linearly to {@link #MIN_ALTITUDE_FACTOR} at build height. */
    private double altitudeFactor() {
        int sea = level.getSeaLevel();
        int top = level.getMaxBuildHeight();
        int y = worldPosition.getY();
        if (y <= sea || top <= sea) {
            return 1;
        }
        double t = Math.min(1, (y - sea) / (double) (top - sea));
        return 1 - (1 - MIN_ALTITUDE_FACTOR) * t;
    }

    private int countAirBlocks() {
        int count = 0;
        for (BlockPos pos : BlockPos.betweenClosed(worldPosition.offset(-1, -1, -1), worldPosition.offset(1, 1, 1))) {
            if (!pos.equals(worldPosition) && level.getBlockState(pos).isAir()) {
                count++;
            }
        }
        return count;
    }

    @Override
    public void recalculateUpgrades(Upgrade upgrade) {
        super.recalculateUpgrades(upgrade);
        if (upgrade == Upgrade.SPEED) {
            // Same speed up Mekanism applies to its machines' operation time
            speedMultiplier = 1 / MekanismUtils.getTicksD(this, 1);
        }
    }

    @NotNull
    @Override
    public List<Component> getInfo(@NotNull Upgrade upgrade) {
        // Speed scales the rate by the multiplier, not by Mekanism's default 2^n for speed
        return UpgradeUtils.getMultScaledInfo(this, upgrade);
    }

    @Override
    public int getRedstoneLevel() {
        return MekanismUtils.redstoneLevelFromContents(outputTank.getStored(), outputTank.getCapacity());
    }

    @Override
    protected boolean makesComparatorDirty(ContainerType<?, ?, ?> type) {
        return type == ContainerType.CHEMICAL;
    }

    @Override
    public void addContainerTrackers(MekanismContainer container) {
        super.addContainerTrackers(container);
        container.track(SyncableEnum.create(MachineStatus.BY_ID, MachineStatus.IDLE, this::getStatus, value -> status = value));
        container.track(SyncableDouble.create(this::getCurrentRate, value -> currentRate = value));
        container.track(SyncableDouble.create(() -> speedMultiplier, value -> speedMultiplier = value));
        container.track(SyncableLong.create(this::getLastEnergyUsed, value -> lastEnergyUsed = value));
    }

    public IChemicalTank getOutputTank() {
        return outputTank;
    }

    public MachineEnergyContainer<AtmosphericCompressorBlockEntity> getEnergyContainer() {
        return energyContainer;
    }

    public MachineStatus getStatus() {
        return status;
    }

    /** mB per tick produced during the last tick, 0 when stalled. */
    public double getCurrentRate() {
        return currentRate;
    }

    /** Joules used during the last tick. */
    public long getLastEnergyUsed() {
        return lastEnergyUsed;
    }

    /** Joules per mB with the installed upgrades, independent of the surroundings. */
    public double getEnergyPerMb() {
        return energyContainer.getEnergyPerTick() / (BASE_RATE * speedMultiplier);
    }
}
