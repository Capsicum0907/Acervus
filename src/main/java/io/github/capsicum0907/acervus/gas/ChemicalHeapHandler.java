package io.github.capsicum0907.acervus.gas;

import mekanism.api.Action;
import mekanism.api.chemical.ChemicalStack;
import mekanism.api.chemical.IChemicalHandler;

public class ChemicalHeapHandler implements IChemicalHandler {
    private static final int TANK = 0;

    private final ChemicalHeapBlockEntity heap;

    public ChemicalHeapHandler(ChemicalHeapBlockEntity heap) {
        this.heap = heap;
    }

    @Override
    public int getChemicalTanks() {
        return 1;
    }

    @Override
    public ChemicalStack getChemicalInTank(int tank) {
        return tank == TANK ? heap.contents() : ChemicalStack.EMPTY;
    }

    @Override
    public void setChemicalInTank(int tank, ChemicalStack stack) {
        if (tank != TANK) {
            return;
        }
        heap.extract(Long.MAX_VALUE, false);
        if (!stack.isEmpty()) {
            heap.insert(stack, false);
        }
    }

    @Override
    public long getChemicalTankCapacity(int tank) {
        return tank == TANK ? heap.capacity() : 0L;
    }

    @Override
    public boolean isValid(int tank, ChemicalStack stack) {
        return tank == TANK && heap.accepts(stack);
    }

    @Override
    public ChemicalStack insertChemical(int tank, ChemicalStack stack, Action action) {
        if (tank != TANK) {
            return stack;
        }
        long taken = heap.insert(stack, action.simulate());
        return taken >= stack.getAmount() ? ChemicalStack.EMPTY
                : stack.copyWithAmount(stack.getAmount() - taken);
    }

    @Override
    public ChemicalStack extractChemical(int tank, long amount, Action action) {
        return tank == TANK ? heap.extract(amount, action.simulate()) : ChemicalStack.EMPTY;
    }
}
