package io.github.valajar.cryosat.machine.compressor;

import io.github.valajar.cryosat.atmosphere.Atmosphere;
import io.github.valajar.cryosat.atmosphere.AtmosphereManager;
import io.github.valajar.cryosat.capability.MekCapabilities;
import io.github.valajar.cryosat.machine.MachineBlockEntity;
import io.github.valajar.cryosat.machine.MachineStatus;
import io.github.valajar.cryosat.registries.CryoBlockEntities;
import java.util.List;
import mekanism.api.Action;
import mekanism.api.AutomationType;
import mekanism.api.RelativeSide;
import mekanism.api.chemical.BasicChemicalTank;
import mekanism.api.chemical.ChemicalStack;
import mekanism.api.chemical.IChemicalHandler;
import mekanism.api.chemical.IChemicalTank;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Pulls the local {@link Atmosphere} out of the surrounding air and pushes it out of its top face.
 */
public class AtmosphericCompressorBlockEntity extends MachineBlockEntity {
    public static final long ENERGY_CAPACITY = 40_000;
    public static final long TANK_CAPACITY = 10_000;
    /** Output in mB per tick with full air exposure at sea level and an atmosphere rate of 1. */
    public static final double BASE_RATE = 10;
    public static final long ENERGY_PER_MB = 10;
    /** Air blocks in the surrounding 3x3x3 needed for full output. */
    public static final int FULL_AIR_BLOCKS = 8;
    public static final double MIN_ALTITUDE_FACTOR = 0.4;
    private static final long MAX_PUSH = 256;
    private static final int AIR_CHECK_INTERVAL = 20;

    private final IChemicalTank output;
    private double pending;
    private int airBlocks = -1;
    private double currentRate;

    public AtmosphericCompressorBlockEntity(BlockPos pos, BlockState state) {
        super(CryoBlockEntities.ATMOSPHERIC_COMPRESSOR.get(), pos, state, ENERGY_CAPACITY);
        output = addTank(BasicChemicalTank.createModern(TANK_CAPACITY,
              (stack, automation) -> true,
              (stack, automation) -> automation == AutomationType.INTERNAL,
              stack -> true, this::markContentsChanged));
    }

    @Override
    protected List<IChemicalTank> tanksOn(RelativeSide side) {
        return side == RelativeSide.TOP ? List.of(output) : List.of();
    }

    @Override
    protected boolean acceptsEnergyFrom(RelativeSide side) {
        return side == RelativeSide.BACK || side == RelativeSide.BOTTOM;
    }

    public IChemicalTank getOutput() {
        return output;
    }

    /** mB per tick produced during the last tick, 0 when stalled. */
    public double getCurrentRate() {
        return currentRate;
    }

    @Override
    protected void serverTick() {
        if (airBlocks < 0 || level.getGameTime() % AIR_CHECK_INTERVAL == 0) {
            airBlocks = countAirBlocks();
        }
        currentRate = 0;
        setStatus(produce());
        pushOutput();
    }

    private MachineStatus produce() {
        Atmosphere atmosphere = AtmosphereManager.INSTANCE.get(level.dimension());
        if (atmosphere == null) {
            return MachineStatus.NO_ATMOSPHERE;
        }
        if (airBlocks == 0) {
            return MachineStatus.NO_AIR;
        }
        if (!output.isEmpty() && !output.isTypeEqual(atmosphere.chemical())) {
            return MachineStatus.OUTPUT_MISMATCH;
        }
        if (output.getNeeded() == 0) {
            return MachineStatus.OUTPUT_FULL;
        }
        double rate = BASE_RATE * atmosphere.rate() * Math.min(1, airBlocks / (double) FULL_AIR_BLOCKS);
        if (atmosphere.altitudeScaling()) {
            rate *= altitudeFactor();
        }
        long cost = (long) Math.ceil(rate * ENERGY_PER_MB);
        if (!energy.tryUse(cost)) {
            return MachineStatus.NO_POWER;
        }
        pending += rate;
        long whole = Math.min((long) pending, output.getNeeded());
        if (whole > 0) {
            output.insert(new ChemicalStack(atmosphere.chemical(), whole), Action.EXECUTE, AutomationType.INTERNAL);
            pending -= whole;
        }
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

    private void pushOutput() {
        if (output.isEmpty()) {
            return;
        }
        IChemicalHandler target = level.getCapability(MekCapabilities.CHEMICAL, worldPosition.above(), Direction.DOWN);
        if (target == null) {
            return;
        }
        ChemicalStack offer = output.extract(MAX_PUSH, Action.SIMULATE, AutomationType.INTERNAL);
        ChemicalStack rejected = target.insertChemical(offer, Action.EXECUTE);
        long accepted = offer.getAmount() - rejected.getAmount();
        if (accepted > 0) {
            output.extract(accepted, Action.EXECUTE, AutomationType.INTERNAL);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putDouble("rate", currentRate);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        currentRate = tag.getDouble("rate");
    }
}
