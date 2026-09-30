package io.github.capsicum0907.acervus.gas;

import io.github.capsicum0907.acervus.HorreumBlockEntity;

import mekanism.api.Action;
import mekanism.api.chemical.ChemicalStack;
import mekanism.api.chemical.IChemicalHandler;

public class HorreumChemicalHandler implements IChemicalHandler {
    private final HorreumBlockEntity rack;

    public HorreumChemicalHandler(HorreumBlockEntity rack) {
        this.rack = rack;
    }

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
