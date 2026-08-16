package io.github.capsicum0907.acervus.gas;

import io.github.capsicum0907.acervus.HorreumBlockEntity;

import mekanism.api.Action;
import mekanism.api.chemical.ChemicalStack;
import mekanism.api.chemical.IChemicalHandler;

/**
 * The window Mekanism's tubes see onto a rack of gas heaps.
 *
 * <p><b>One tank per gas heap actually in the rack, not one per rack slot.</b> The
 * same reversal as {@link io.github.capsicum0907.acervus.HorreumFluidHandler}, and for
 * the same reason: twelve fixed tanks meant twelve bars in anything that lists them,
 * eleven of which said Empty forever.
 *
 * <p>This one is the less clear-cut of the two, because {@code insertChemical} and
 * {@code extractChemical} <em>do</em> take an index where the fluid interface does not.
 * What makes it safe is that Mekanism reaches them through the handler's own sideless
 * defaults, which walk the tanks in the tick they were asked for; nothing carries a
 * tank number from one tick to the next. If something ever does, the symptom is an
 * insert landing in the wrong heap of the same rack — not a loss, and not a
 * duplication.
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

    /** The gas heaps in the rack, in slot order. Read fresh: the slots change. */
    private java.util.List<HeldChemicalHeap> tanks() {
        return rack.readAll(GasHeap.ITEM.get(), HeldChemicalHeap::stored);
    }

    private HeldChemicalHeap at(int tank) {
        java.util.List<HeldChemicalHeap> tanks = tanks();
        return tank < 0 || tank >= tanks.size() ? null : tanks.get(tank);
    }

    @Override
    public int getChemicalTanks() {
        return tanks().size();
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
