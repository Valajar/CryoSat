package io.github.valajar.cryosat.superconductor;

import io.github.valajar.cryosat.registries.CryoChemicals;
import mekanism.api.Action;
import mekanism.api.AutomationType;
import mekanism.api.IContentsListener;
import mekanism.api.chemical.BasicChemicalTank;
import mekanism.api.chemical.ChemicalStack;
import mekanism.api.chemical.IChemicalTank;
import net.minecraft.nbt.CompoundTag;

/**
 * The cooling of a superconducting block: a liquid nitrogen tank it drains, a nitrogen gas tank its boil off collects in, and
 * whether it is cold enough to work. The owning tile adds both tanks to its chemical tank holder (so tubes and side config
 * work), calls {@link #tick} once per server tick, saves the state with {@link #save}/{@link #load} and syncs
 * {@link #getState} and {@link #getCooldown} to its container.
 * <p>
 * Rules shared by every superconducting block: a steady trickle keeps it cold; running dry quenches it and the effect stops;
 * once coolant is back it needs {@link #COOLDOWN_TICKS} to recover. Nothing is ever destroyed.
 */
public class Cryostat {
    public static final long COOLANT_CAPACITY = 8_000;
    public static final long BOIL_OFF_CAPACITY = 8_000;
    /** Share of the boiled off coolant that comes back as recoverable gas; the rest is lost, so a closed loop needs topping up. */
    public static final double BOIL_OFF_RETURN = 0.9;
    public static final int COOLDOWN_TICKS = 30 * 20;

    public final IChemicalTank coolantTank;
    public final IChemicalTank boilOffTank;
    private final long trickle;
    private CryostatState state = CryostatState.NO_COOLANT;
    private int cooldown = COOLDOWN_TICKS;
    private double boilOffRemainder;
    private long lastUsed;
    private long usedThisTick;

    /**
     * @param trickle liquid nitrogen in mB per tick that keeps the block cold while it is idle
     */
    public Cryostat(long trickle, IContentsListener listener) {
        this.trickle = trickle;
        coolantTank = BasicChemicalTank.input(COOLANT_CAPACITY, chemical -> chemical == CryoChemicals.LIQUID_NITROGEN.get(), listener);
        boilOffTank = BasicChemicalTank.output(BOIL_OFF_CAPACITY, listener);
    }

    /**
     * Drains the trickle and updates the state. Call once per server tick, before using {@link #isSuperconducting}.
     */
    public void tick() {
        lastUsed = usedThisTick;
        usedThisTick = 0;
        if (!drain(trickle)) {
            state = CryostatState.NO_COOLANT;
            cooldown = COOLDOWN_TICKS;
        } else if (cooldown > 0) {
            cooldown--;
            state = CryostatState.COOLING_DOWN;
        } else {
            state = CryostatState.SUPERCONDUCTING;
        }
    }

    /**
     * Uses extra coolant for one piece of work (for example one cargo batch). Only call while superconducting.
     *
     * @return whether there was enough; if not, nothing is drained and the work should not get its bonus
     */
    public boolean use(long amount) {
        return state == CryostatState.SUPERCONDUCTING && drain(amount);
    }

    private boolean drain(long amount) {
        if (amount <= 0) {
            return true;
        }
        if (coolantTank.extract(amount, Action.SIMULATE, AutomationType.INTERNAL).getAmount() < amount) {
            return false;
        }
        coolantTank.extract(amount, Action.EXECUTE, AutomationType.INTERNAL);
        usedThisTick += amount;
        boilOffRemainder += amount * BOIL_OFF_RETURN;
        long gas = (long) boilOffRemainder;
        if (gas > 0) {
            // Vented when the boil off tank is full
            boilOffTank.insert(new ChemicalStack(CryoChemicals.NITROGEN, gas), Action.EXECUTE, AutomationType.INTERNAL);
            boilOffRemainder -= gas;
        }
        return true;
    }

    public boolean isSuperconducting() {
        return state == CryostatState.SUPERCONDUCTING;
    }

    public CryostatState getState() {
        return state;
    }

    /** Client side setter for container sync. */
    public void setState(CryostatState state) {
        this.state = state;
    }

    /** Ticks left before a cooling down block works again. */
    public int getCooldown() {
        return cooldown;
    }

    public void setCooldown(int cooldown) {
        this.cooldown = cooldown;
    }

    /** Coolant drained during the last tick, trickle and work together. */
    public long getLastUsed() {
        return lastUsed;
    }

    public void setLastUsed(long lastUsed) {
        this.lastUsed = lastUsed;
    }

    public void save(CompoundTag tag) {
        tag.putInt("cryostat_state", state.ordinal());
        tag.putInt("cryostat_cooldown", cooldown);
        tag.putDouble("cryostat_boil_off", boilOffRemainder);
    }

    public void load(CompoundTag tag) {
        state = CryostatState.BY_ID.apply(tag.getInt("cryostat_state"));
        cooldown = tag.contains("cryostat_cooldown") ? tag.getInt("cryostat_cooldown") : COOLDOWN_TICKS;
        boilOffRemainder = tag.getDouble("cryostat_boil_off");
    }
}
