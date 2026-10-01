package io.github.capsicum0907.acervus.gas.mek;

import java.util.List;

import io.github.capsicum0907.acervus.HorreumBlockEntity;
import io.github.capsicum0907.acervus.gas.GasHeap;
import io.github.capsicum0907.acervus.gas.HeldChemicalHeap;

import mekanism.api.Action;
import mekanism.api.chemical.ChemicalStack;
import mekanism.api.chemical.IChemicalHandler;

public class HorreumChemicalHandler implements IChemicalHandler {
    private final HorreumBlockEntity rack;

    public HorreumChemicalHandler(HorreumBlockEntity rack) {
        this.rack = rack;
    }

    private List<HeldChemicalHeap> tanks() {
        return rack.readAll(GasHeap.ITEM.get(), HeldChemicalHeap::stored);
    }

    private HeldChemicalHeap at(int tank) {
        List<HeldChemicalHeap> tanks = tanks();
        return tank < 0 || tank >= tanks.size() ? null : tanks.get(tank);
    }

    @Override
    public int getChemicalTanks() {
        return tanks().size();
    }

    @Override
    public ChemicalStack getChemicalInTank(int tank) {
        HeldChemicalHeap heap = at(tank);
        if (heap == null || heap.isEmpty() || heap.unreadable()) {
            return ChemicalStack.EMPTY;
        }
        return MekanismChemistry.stack(heap.kind(), heap.amount(), heap.registries());
    }

    @Override
    public void setChemicalInTank(int tank, ChemicalStack stack) {
        HeldChemicalHeap heap = at(tank);
        if (heap == null || heap.unreadable()) {
            return;
        }
        heap.extractAmount(Long.MAX_VALUE, false);
        if (!stack.isEmpty()) {
            heap.insertKind(MekanismChemistry.kind(stack, heap.registries()), stack.getAmount(), false);
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
        return heap != null && !stack.isEmpty()
                && heap.insertKind(MekanismChemistry.kind(stack, heap.registries()), stack.getAmount(), true) > 0;
    }

    @Override
    public ChemicalStack insertChemical(int tank, ChemicalStack stack, Action action) {
        HeldChemicalHeap heap = at(tank);
        if (heap == null || stack.isEmpty()) {
            return stack;
        }
        long taken = heap.insertKind(MekanismChemistry.kind(stack, heap.registries()), stack.getAmount(),
                action.simulate());
        if (taken > 0 && action.execute()) {
            rack.changed();
        }
        return taken >= stack.getAmount() ? ChemicalStack.EMPTY : stack.copyWithAmount(stack.getAmount() - taken);
    }

    @Override
    public ChemicalStack extractChemical(int tank, long amount, Action action) {
        HeldChemicalHeap heap = at(tank);
        if (heap == null) {
            return ChemicalStack.EMPTY;
        }
        ChemicalStack out = MekanismChemistry.stack(heap.kind(), heap.extractAmount(amount, action.simulate()),
                heap.registries());
        if (!out.isEmpty() && action.execute()) {
            rack.changed();
        }
        return out;
    }
}
