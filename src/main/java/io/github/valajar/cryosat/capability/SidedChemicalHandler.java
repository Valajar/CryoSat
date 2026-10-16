package io.github.valajar.cryosat.capability;

import java.util.List;
import java.util.function.Supplier;
import mekanism.api.Action;
import mekanism.api.AutomationType;
import mekanism.api.chemical.ChemicalStack;
import mekanism.api.chemical.IChemicalHandler;
import mekanism.api.chemical.IChemicalTank;
import net.minecraft.core.Direction;
import org.jetbrains.annotations.Nullable;

/**
 * What a pipe or neighbouring machine sees when it looks at one face of a machine: only the tanks exposed on that face,
 * with external automation rules applied.
 */
public class SidedChemicalHandler implements IChemicalHandler {
    private final Supplier<List<IChemicalTank>> tanks;
    private final AutomationType automation;

    public SidedChemicalHandler(Supplier<List<IChemicalTank>> tanks, @Nullable Direction side) {
        this.tanks = tanks;
        this.automation = AutomationType.handler(side);
    }

    @Nullable
    private IChemicalTank tank(int index) {
        List<IChemicalTank> list = tanks.get();
        return index >= 0 && index < list.size() ? list.get(index) : null;
    }

    @Override
    public int getChemicalTanks() {
        return tanks.get().size();
    }

    @Override
    public ChemicalStack getChemicalInTank(int index) {
        IChemicalTank tank = tank(index);
        return tank == null ? ChemicalStack.EMPTY : tank.getStack();
    }

    @Override
    public void setChemicalInTank(int index, ChemicalStack stack) {
        IChemicalTank tank = tank(index);
        if (tank != null) {
            tank.setStack(stack);
        }
    }

    @Override
    public long getChemicalTankCapacity(int index) {
        IChemicalTank tank = tank(index);
        return tank == null ? 0 : tank.getCapacity();
    }

    @Override
    public boolean isValid(int index, ChemicalStack stack) {
        IChemicalTank tank = tank(index);
        return tank != null && tank.isValid(stack);
    }

    @Override
    public ChemicalStack insertChemical(int index, ChemicalStack stack, Action action) {
        IChemicalTank tank = tank(index);
        return tank == null ? stack : tank.insert(stack, action, automation);
    }

    @Override
    public ChemicalStack extractChemical(int index, long amount, Action action) {
        IChemicalTank tank = tank(index);
        return tank == null ? ChemicalStack.EMPTY : tank.extract(amount, action, automation);
    }
}
