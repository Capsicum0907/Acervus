package io.github.capsicum0907.acervus.gas;

import io.github.capsicum0907.acervus.HorreumBlockEntity;

import mekanism.api.Action;
import mekanism.api.chemical.ChemicalStack;
import mekanism.api.chemical.IChemicalHandler;

/**
 * The window Mekanism's tubes see onto a rack of gas heaps.
 *
 * <p>One tank per rack slot, always twelve, for the same reason as the others: a
 * tube remembers indices between ticks, so they must not move when a slot changes.
 * A slot holding anything but a gas heap reads as a tank of no capacity.
 *
 * <p>The one that needs no care at the edge. Mekanism counts in longs at both ends,
 * so nothing here saturates, and the tank reports exactly what it holds.
 *
 * <p>This class exists only when Mekanism does. It is in this package for that
 * reason, and nothing outside the package names a chemical.
 */
public class HorreumChemicalHandler implements IChemicalHandler {
    private final HorreumBlockEntity rack;

    public HorreumChemicalHandler(HorreumBlockEntity rack) {
        this.rack = rack;
    }

    private HeldChemicalHeap at(int tank) {
        return rack.read(tank, GasHeap.ITEM.get(), HeldChemicalHeap::stored);
    }

    @Override
    public int getChemicalTanks() {
        return HorreumBlockEntity.SLOTS;
    }

    @Override
    public ChemicalStack getChemicalInTank(int tank) {
        HeldChemicalHeap heap = at(tank);
        if (heap == null || heap.isEmpty()) {
            return ChemicalStack.EMPTY;
        }
        return heap.sample().copyWithAmount(heap.amount());
    }

    /** Assignment, read as "hold exactly this instead"; see {@link ChemicalHeapHandler}. */
    @Override
    public void setChemicalInTank(int tank, ChemicalStack stack) {
        HeldChemicalHeap heap = at(tank);
        if (heap == null) {
            return;
        }
        heap.extract(Long.MAX_VALUE, false);
        if (!stack.isEmpty()) {
            heap.insert(stack, false);
        }
        rack.changed();
    }

    @Override
    public long getChemicalTankCapacity(int tank) {
        HeldChemicalHeap heap = at(tank);
        return heap == null ? 0L : heap.capacity();
    }

    @Override
    public boolean isValid(int tank, ChemicalStack stack) {
        HeldChemicalHeap heap = at(tank);
        return heap != null && heap.insert(stack, true) > 0;
    }

    @Override
    public ChemicalStack insertChemical(int tank, ChemicalStack stack, Action action) {
        HeldChemicalHeap heap = at(tank);
        if (heap == null || stack.isEmpty()) {
            return stack;
        }
        long taken = heap.insert(stack, action.simulate());
        if (taken > 0 && action.execute()) {
            rack.changed();
        }
        return taken >= stack.getAmount() ? ChemicalStack.EMPTY
                : stack.copyWithAmount(stack.getAmount() - taken);
    }

    @Override
    public ChemicalStack extractChemical(int tank, long amount, Action action) {
        HeldChemicalHeap heap = at(tank);
        if (heap == null) {
            return ChemicalStack.EMPTY;
        }
        ChemicalStack out = heap.extract(amount, action.simulate());
        if (!out.isEmpty() && action.execute()) {
            rack.changed();
        }
        return out;
    }
}
