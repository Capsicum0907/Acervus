package io.github.capsicum0907.acervus.gas.mek;

import io.github.capsicum0907.acervus.gas.ChemicalHeapBlockEntity;

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
        if (tank != TANK || heap.unreadable()) {
            return ChemicalStack.EMPTY;
        }
        return MekanismChemistry.stack(heap.kind(), heap.amount(), heap.registries());
    }

    @Override
    public void setChemicalInTank(int tank, ChemicalStack stack) {
        if (tank != TANK || heap.unreadable()) {
            return;
        }
        heap.extractAmount(Long.MAX_VALUE, false);
        if (!stack.isEmpty()) {
            heap.insertKind(MekanismChemistry.kind(stack, heap.registries()), stack.getAmount(), false);
        }
    }

    @Override
    public long getChemicalTankCapacity(int tank) {
        return tank == TANK ? heap.capacity() : 0L;
    }

    @Override
    public boolean isValid(int tank, ChemicalStack stack) {
        return tank == TANK && !stack.isEmpty() && heap.accepts(MekanismChemistry.kind(stack, heap.registries()));
    }

    @Override
    public ChemicalStack insertChemical(int tank, ChemicalStack stack, Action action) {
        if (tank != TANK || stack.isEmpty()) {
            return stack;
        }
        long taken = heap.insertKind(MekanismChemistry.kind(stack, heap.registries()), stack.getAmount(),
                action.simulate());
        return taken >= stack.getAmount() ? ChemicalStack.EMPTY : stack.copyWithAmount(stack.getAmount() - taken);
    }

    @Override
    public ChemicalStack extractChemical(int tank, long amount, Action action) {
        if (tank != TANK) {
            return ChemicalStack.EMPTY;
        }
        net.minecraft.nbt.CompoundTag kind = heap.kind();
        long taken = heap.extractAmount(amount, action.simulate());
        return MekanismChemistry.stack(kind, taken, heap.registries());
    }
}
