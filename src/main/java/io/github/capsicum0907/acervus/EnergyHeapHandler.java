package io.github.capsicum0907.acervus;

import net.neoforged.neoforge.energy.IEnergyStorage;

public class EnergyHeapHandler implements IEnergyStorage {
    private final EnergyHeapBlockEntity heap;

    public EnergyHeapHandler(EnergyHeapBlockEntity heap) {
        this.heap = heap;
    }

    @Override
    public int receiveEnergy(int toReceive, boolean simulate) {
        return heap.receive(toReceive, simulate);
    }

    @Override
    public int extractEnergy(int toExtract, boolean simulate) {
        return heap.give(toExtract, simulate);
    }

    @Override
    public int getEnergyStored() {
        return (int) Math.min(heap.stored(), Integer.MAX_VALUE);
    }

    @Override
    public int getMaxEnergyStored() {
        return (int) Math.min(heap.capacity(), Integer.MAX_VALUE);
    }

    @Override
    public boolean canExtract() {
        return true;
    }

    @Override
    public boolean canReceive() {
        return true;
    }
}
