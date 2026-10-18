package io.github.valajar.cryosat.multiblock.liquefaction;

import io.github.valajar.cryosat.recipe.LiquefactionRecipe;
import io.github.valajar.cryosat.tile.liquefaction.TileEntityCryoCasing;
import io.github.valajar.cryosat.tile.liquefaction.TileEntityCryoValve;
import java.util.ArrayList;
import java.util.List;
import mekanism.api.Action;
import mekanism.api.AutomationType;
import mekanism.api.chemical.ChemicalStack;
import mekanism.api.chemical.IChemicalHandler;
import mekanism.api.chemical.IChemicalTank;
import mekanism.api.energy.IEnergyContainer;
import mekanism.api.heat.HeatAPI;
import mekanism.common.capabilities.chemical.VariableCapacityChemicalTank;
import mekanism.common.capabilities.energy.VariableCapacityEnergyContainer;
import mekanism.common.capabilities.heat.VariableHeatCapacitor;
import mekanism.common.inventory.container.sync.dynamic.ContainerSync;
import mekanism.common.lib.multiblock.IValveHandler;
import mekanism.common.lib.multiblock.MultiblockData;
import mekanism.common.util.ChemicalUtil;
import mekanism.common.util.WorldUtils;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;

/**
 * Shared state of a formed Cryogenic Liquefaction Plant.
 * <p>
 * Power: the plant has one energy buffer, filled through any Cryocooler or Cryo Valve, that all its coolers draw on. Its size
 * scales with the number of coolers.
 * <p>
 * Thermal model: the plant interior has a cold temperature that heat leaks into from outside, proportional to the temperature
 * difference and the plant's surface. Cryocoolers pump heat out of the interior at a Carnot style energy cost that grows as the
 * interior gets colder, and dump both the pumped heat and the energy they spent into the "hot side", a heat capacitor exposed
 * through Heat Rejection Ports. If the hot side cannot get rid of that heat it climbs and cooling becomes more expensive, and
 * past {@link #HOT_LIMIT} the coolers stop. Liquefying chemicals releases latent heat back into the interior.
 */
public class LiquefactionMultiblockData extends MultiblockData implements IValveHandler {
    public static final int OUTPUT_TANKS = 3;
    /** Tank capacity per interior block, for the input and each output. */
    public static final long TANK_PER_BLOCK = 4_000;
    /** Interior heat capacity per interior block (heat per Kelvin). */
    public static final double COLD_CAPACITY_PER_BLOCK = 1_000;
    /** Heat leak per Kelvin of difference, per casing block and per glass block in the shell. */
    public static final double LEAK_PER_CASING = 0.02;
    public static final double LEAK_PER_GLASS = 0.08;
    /** Energy buffer per Cryocooler (one cooler's worth is always present). */
    public static final long ENERGY_PER_COOLER = 100_000;
    /** Maximum heat each Cryocooler removes per tick. */
    public static final double COOLER_HEAT = 200;
    /** Fraction of the ideal (Carnot) efficiency the coolers achieve. */
    public static final double CARNOT_EFFICIENCY = 0.5;
    /** Energy per unit of heat moved never drops below this, even when the hot side is colder than the interior. */
    public static final double MIN_ENERGY_PER_HEAT = 0.25;
    public static final double MIN_TEMPERATURE = 4;
    /** Above this hot side temperature the coolers shut down. */
    public static final double HOT_LIMIT = 1_200;
    /** Hot side heat capacity per Heat Rejection Port (one port's worth is always present). */
    public static final double HOT_CAPACITY_PER_PORT = 50_000;
    public static final double HOT_INVERSE_CONDUCTION = 1;
    public static final double HOT_INVERSE_INSULATION = 10_000;
    /** Maximum chemical liquefied per tick per interior block. */
    public static final long RATE_PER_BLOCK = 20;

    @ContainerSync
    public IChemicalTank inputTank;
    @ContainerSync
    public IChemicalTank outputTank1;
    @ContainerSync
    public IChemicalTank outputTank2;
    @ContainerSync
    public IChemicalTank outputTank3;
    private final List<IChemicalTank> outputTanks;
    @ContainerSync
    public VariableHeatCapacitor hotSide;
    @ContainerSync
    public IEnergyContainer energyContainer;

    /** Interior temperature in Kelvin; negative until the plant has formed once. */
    @ContainerSync
    public double coldTemperature = -1;
    @ContainerSync
    public double lastLeak;
    @ContainerSync
    public double lastCooling;
    @ContainerSync
    public long lastEnergyUsed;
    @ContainerSync
    public long lastProcessed;
    @ContainerSync
    public int coolers;
    @ContainerSync
    public int heatPorts;
    @ContainerSync
    public PlantStatus status = PlantStatus.IDLE;
    /** Temperature the current input needs, negative when there is nothing to liquefy. */
    @ContainerSync
    public double targetTemperature = -1;

    public int casingBlocks;
    public int glassBlocks;
    private double biomeAmbientTemp;
    private final double[] outputRemainders = new double[OUTPUT_TANKS];
    private final List<AdvancedCapabilityOutputTarget<IChemicalHandler, ValveMode>> outputTargets = new ArrayList<>();

    public LiquefactionMultiblockData(TileEntityCryoCasing tile) {
        super(tile);
        biomeAmbientTemp = HeatAPI.getAmbientTemp(tile.getLevel(), tile.getBlockPos());
        chemicalTanks.add(inputTank = VariableCapacityChemicalTank.input(this, this::getTankCapacity,
              stack -> getLevel() == null || LiquefactionRecipe.find(getLevel(), stack.getChemicalHolder()) != null, this));
        chemicalTanks.add(outputTank1 = VariableCapacityChemicalTank.output(this, this::getTankCapacity, stack -> true, this));
        chemicalTanks.add(outputTank2 = VariableCapacityChemicalTank.output(this, this::getTankCapacity, stack -> true, this));
        chemicalTanks.add(outputTank3 = VariableCapacityChemicalTank.output(this, this::getTankCapacity, stack -> true, this));
        outputTanks = List.of(outputTank1, outputTank2, outputTank3);
        // Only the coolers take energy out; anyone can put it in while the plant is formed
        energyContainers.add(energyContainer = VariableCapacityEnergyContainer.create(this::getEnergyCapacity,
              automationType -> automationType == AutomationType.INTERNAL, automationType -> isFormed(), this));
        heatCapacitors.add(hotSide = VariableHeatCapacitor.create(HOT_CAPACITY_PER_PORT, () -> HOT_INVERSE_CONDUCTION,
              () -> HOT_INVERSE_INSULATION, () -> biomeAmbientTemp, this));
    }

    @Override
    public void onCreated(Level world) {
        super.onCreated(world);
        biomeAmbientTemp = calculateAverageAmbientTemperature(world);
        hotSide.setHeatCapacity(HOT_CAPACITY_PER_PORT * Math.max(1, heatPorts), true);
        if (coldTemperature < 0) {
            coldTemperature = biomeAmbientTemp;
        }
    }

    public int getInteriorVolume() {
        return Math.max(1, (length() - 2) * (width() - 2) * (height() - 2));
    }

    public long getTankCapacity() {
        return TANK_PER_BLOCK * getInteriorVolume();
    }

    public long getEnergyCapacity() {
        return ENERGY_PER_COOLER * Math.max(1, coolers);
    }

    private double coldCapacity() {
        return COLD_CAPACITY_PER_BLOCK * getInteriorVolume();
    }

    public double getAmbientTemperature() {
        return biomeAmbientTemp;
    }

    public double getHotTemperature() {
        return hotSide.getTemperature();
    }

    public List<IChemicalTank> getOutputTanks() {
        return outputTanks;
    }

    public List<IChemicalTank> getValveTanks(ValveMode mode) {
        return mode == ValveMode.INPUT ? List.of(inputTank) : List.of(outputTanks.get(mode.outputIndex()));
    }

    // --- per tick -----------------------------------------------------------------------------

    @Override
    public boolean tick(Level world) {
        boolean needsPacket = super.tick(world);
        double leakCoefficient = LEAK_PER_CASING * casingBlocks + LEAK_PER_GLASS * glassBlocks;
        lastLeak = leakCoefficient * (biomeAmbientTemp - coldTemperature);
        coldTemperature = Math.max(MIN_TEMPERATURE, coldTemperature + lastLeak / coldCapacity());

        runCoolers();
        targetTemperature = getTargetTemperature();
        status = process();

        simulateHotSideEnvironment();
        updateHeatCapacitors(null);
        for (int i = 0; i < OUTPUT_TANKS; i++) {
            IChemicalTank tank = outputTanks.get(i);
            if (!tank.isEmpty() && !outputTargets.isEmpty()) {
                ChemicalUtil.emit(getActiveOutputs(outputTargets, ValveMode.output(i)), tank);
            }
        }
        return needsPacket;
    }

    private PlantStatus process() {
        lastProcessed = 0;
        if (getHotTemperature() >= HOT_LIMIT) {
            return PlantStatus.OVERHEATED;
        }
        if (inputTank.isEmpty()) {
            return coolers == 0 ? PlantStatus.NO_COOLERS : PlantStatus.IDLE;
        }
        LiquefactionRecipe recipe = LiquefactionRecipe.find(getLevel(), inputTank.getTypeHolder());
        if (recipe == null) {
            return PlantStatus.IDLE;
        }
        if (coldTemperature > recipe.maxTemperature()) {
            return coolers == 0 ? PlantStatus.NO_COOLERS : PlantStatus.COOLING_DOWN;
        }
        int[] tanks = assignOutputTanks(recipe);
        if (tanks == null) {
            return PlantStatus.OUTPUT_MISMATCH;
        }
        long amount = Math.min(inputTank.getStored(), RATE_PER_BLOCK * getInteriorVolume());
        for (int i = 0; i < recipe.outputs().size(); i++) {
            double fraction = recipe.fraction(i);
            if (fraction > 0) {
                int tank = tanks[i];
                amount = Math.min(amount, (long) Math.floor((outputTanks.get(tank).getNeeded() - outputRemainders[tank]) / fraction));
            }
        }
        if (amount <= 0) {
            return PlantStatus.OUTPUT_FULL;
        }
        inputTank.shrinkStack(amount, Action.EXECUTE);
        for (int i = 0; i < recipe.outputs().size(); i++) {
            int tank = tanks[i];
            outputRemainders[tank] += amount * recipe.fraction(i);
            long whole = (long) outputRemainders[tank];
            if (whole > 0) {
                outputTanks.get(tank).insert(recipe.outputs().get(i).copyWithAmount(whole), Action.EXECUTE, AutomationType.INTERNAL);
                outputRemainders[tank] -= whole;
            }
        }
        coldTemperature += amount * recipe.latentHeat() / coldCapacity();
        lastProcessed = amount;
        return PlantStatus.RUNNING;
    }

    /**
     * Picks an output tank for each recipe output: the tank already holding that chemical, otherwise the first empty one, so
     * switching from one gas to another only needs a free tank rather than emptying a specific one.
     *
     * @return the tank index per recipe output, or {@code null} if some output has nowhere to go
     */
    private int[] assignOutputTanks(LiquefactionRecipe recipe) {
        int[] tanks = new int[recipe.outputs().size()];
        boolean[] taken = new boolean[OUTPUT_TANKS];
        for (int i = 0; i < tanks.length; i++) {
            tanks[i] = -1;
            for (int t = 0; t < OUTPUT_TANKS; t++) {
                if (!taken[t] && !outputTanks.get(t).isEmpty() && outputTanks.get(t).isTypeEqual(recipe.outputs().get(i).getChemicalHolder())) {
                    tanks[i] = t;
                    break;
                }
            }
        }
        for (int i = 0; i < tanks.length; i++) {
            if (tanks[i] >= 0) {
                taken[tanks[i]] = true;
            }
        }
        for (int i = 0; i < tanks.length; i++) {
            for (int t = 0; t < OUTPUT_TANKS && tanks[i] < 0; t++) {
                if (!taken[t] && outputTanks.get(t).isEmpty()) {
                    tanks[i] = t;
                    taken[t] = true;
                }
            }
            if (tanks[i] < 0) {
                return null;
            }
        }
        return tanks;
    }

    /** Runs all Cryocoolers for this tick on the shared energy buffer. */
    private void runCoolers() {
        lastCooling = 0;
        lastEnergyUsed = 0;
        double hot = getHotTemperature();
        if (coolers == 0 || hot >= HOT_LIMIT || coldTemperature <= MIN_TEMPERATURE) {
            return;
        }
        double energyPerHeat = Math.max(MIN_ENERGY_PER_HEAT, (hot - coldTemperature) / coldTemperature / CARNOT_EFFICIENCY);
        double maxHeat = Math.min(COOLER_HEAT * coolers, (coldTemperature - MIN_TEMPERATURE) * coldCapacity());
        long available = energyContainer.extract(Long.MAX_VALUE, Action.SIMULATE, AutomationType.INTERNAL);
        double heat = Math.min(maxHeat, available / energyPerHeat);
        if (heat <= 0) {
            return;
        }
        long used = Math.min(available, (long) Math.ceil(heat * energyPerHeat));
        energyContainer.extract(used, Action.EXECUTE, AutomationType.INTERNAL);
        coldTemperature -= heat / coldCapacity();
        hotSide.handleHeat(heat + used);
        lastCooling = heat;
        lastEnergyUsed = used;
    }

    /** @return the temperature the chemical in the input tank needs, or a negative value if there is nothing to liquefy */
    public double getTargetTemperature() {
        if (inputTank.isEmpty() || getLevel() == null) {
            return -1;
        }
        LiquefactionRecipe recipe = LiquefactionRecipe.find(getLevel(), inputTank.getTypeHolder());
        return recipe == null ? -1 : recipe.maxTemperature();
    }

    /** Slow passive loss of hot side heat into the air around the plant. */
    private void simulateHotSideEnvironment() {
        double inverseConduction = HeatAPI.AIR_INVERSE_COEFFICIENT + HOT_INVERSE_INSULATION + HOT_INVERSE_CONDUCTION;
        double tempToTransfer = (getHotTemperature() - biomeAmbientTemp) / inverseConduction;
        hotSide.handleHeat(-tempToTransfer * hotSide.getHeatCapacity());
    }

    @Override
    protected void updateEjectors(Level world) {
        outputTargets.clear();
        for (ValveData valve : valves) {
            TileEntityCryoValve tile = WorldUtils.getTileEntity(TileEntityCryoValve.class, world, valve.location);
            if (tile != null) {
                tile.addChemicalTargetCapability(outputTargets, valve.side);
            }
        }
    }

    @Override
    protected int getMultiblockRedstoneLevel() {
        return (int) Math.round(15 * Math.max(0, Math.min(1, 1 - (coldTemperature - MIN_TEMPERATURE) / (biomeAmbientTemp - MIN_TEMPERATURE))));
    }

    // --- persistence ----------------------------------------------------------------------------

    void loadThermalState(double coldTemperature, double[] remainders) {
        this.coldTemperature = coldTemperature;
        System.arraycopy(remainders, 0, outputRemainders, 0, OUTPUT_TANKS);
    }

    double[] getOutputRemainders() {
        return outputRemainders.clone();
    }

    @Override
    public void readUpdateTag(CompoundTag tag, HolderLookup.Provider provider) {
        super.readUpdateTag(tag, provider);
        readValves(tag);
    }

    @Override
    public void writeUpdateTag(CompoundTag tag, HolderLookup.Provider provider) {
        super.writeUpdateTag(tag, provider);
        writeValves(tag);
    }
}
