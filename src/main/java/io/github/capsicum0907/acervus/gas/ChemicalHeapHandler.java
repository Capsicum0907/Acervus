package io.github.capsicum0907.acervus.gas;

import mekanism.api.Action;
import mekanism.api.chemical.ChemicalStack;
import mekanism.api.chemical.IChemicalHandler;

/**
 * The window a chemical heap shows to Mekanism's pipes and machines.
 *
 * <p>The plainest of the four, because Mekanism counts in longs. There is no
 * saturating, no clamping and no choosing which of two true things to say: the tank
 * reports what is in it and hands over what is asked for, and both numbers are the
 * real ones. The other three heaps are all shaped around a ceiling that this one
 * does not have.
 *
 * <p>Nothing is remembered here, for the same reason as the others.
 */
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

    /**
     * Assignment, which a heap has no natural meaning for. Read as "hold exactly this
     * instead" — the only reading that leaves the block consistent afterwards.
     */
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
