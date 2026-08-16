package io.github.capsicum0907.acervus;

import net.neoforged.neoforge.energy.IEnergyStorage;

/**
 * The window an energy heap shows to cables and machines.
 *
 * <p>Every number in {@link IEnergyStorage} is an int, including the two that only
 * report — so unlike the item side there is no allowance anywhere to say more than
 * two billion. Both reported numbers therefore saturate, and a heap holding a
 * trillion reads as full to anything that only knows how to ask.
 *
 * <p>That is a display problem rather than a storage one: {@code receiveEnergy} and
 * {@code extractEnergy} are bounded per call, and a call is not a lifetime. What it
 * does cost is the {@code stored / capacity} bar every energy readout draws, which
 * will sit at whatever fraction two billion is of two billion. There is no way to
 * be both honest and accurate here; saying the largest true-ish number is the less
 * wrong of the two.
 *
 * <p>Nothing is remembered here, for the same reason as the other two.
 */
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
